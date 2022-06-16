package emma.arp;

import emma.*;
import org.pcap4j.core.PcapHandle;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.PcapNetworkInterface;
import org.pcap4j.util.MacAddress;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Created 12/06/2022 by SuperMartijn642
 */
public class ARPSpoofingAttackHandler implements AttackHandler {

    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("[HH:mm:ss]");

    private JComboBox<ComboBoxEntryWrapper<PcapNetworkInterface>> interfaceDropdown;
    private JTextField spoofIpField, spoofMacField;
    private JTextField victimIpField;
    private JButton sendOnceButton, startSendingButton, stopSendingButton;
    private JTextArea outputField;

    private List<PcapNetworkInterface> allNetworkInterfaces;
    private PcapNetworkInterface networkInterface;
    private InetAddress spoofIp;
    private MacAddress spoofMac;
    private InetAddress victimIp;

    private boolean isSending = false;

    @Override
    public void initialize(){
        this.allNetworkInterfaces = NetworkUtils.gatherNetworkInterfaces();
    }

    @Override
    public void initializeWindow(JFrame frame){
        // Set the window size
        frame.setSize(875, 320);

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

        // Spoof ip-address
        JLabel spoofIpLabel = new JLabel("Spoof ip-address:");
        spoofIpLabel.setBounds(10, 75, 150, 15);
        frame.add(spoofIpLabel);
        this.spoofIpField = new JTextField(20);
        this.spoofIpField.setBounds(10, 90, 150, 30);
        this.spoofIpField.getDocument().addDocumentListener(new DocumentChangeListener(() -> this.updateSpoofIp(this.spoofIpField.getText())));
        ((PlainDocument)this.spoofIpField.getDocument()).setDocumentFilter(new RegexDocumentFilter("[a-zA-Z0-9.]*"));
        frame.add(this.spoofIpField);

        // Spoof mac-address
        JLabel spoofMacLabel = new JLabel("Spoof mac-address:");
        spoofMacLabel.setBounds(10, 125, 150, 15);
        frame.add(spoofMacLabel);
        this.spoofMacField = new JTextField(20);
        this.spoofMacField.setBounds(10, 140, 150, 30);
        this.spoofMacField.getDocument().addDocumentListener(new DocumentChangeListener(() -> this.updateSpoofMac(this.spoofMacField.getText())));
        ((PlainDocument)this.spoofMacField.getDocument()).setDocumentFilter(new RegexDocumentFilter("[a-zA-Z0-9:]*"));
        frame.add(this.spoofMacField);

        // Victim ip-address
        JLabel victimIpLabel = new JLabel("Victim ip-address:");
        victimIpLabel.setBounds(170, 75, 150, 15);
        frame.add(victimIpLabel);
        this.victimIpField = new JTextField();
        this.victimIpField.setBounds(170, 90, 150, 30);
        this.victimIpField.getDocument().addDocumentListener(new DocumentChangeListener(() -> this.updateVictimIp(this.victimIpField.getText())));
        ((PlainDocument)this.victimIpField.getDocument()).setDocumentFilter(new RegexDocumentFilter("[a-zA-Z0-9.]*"));
        frame.add(this.victimIpField);

        // Send once button
        this.sendOnceButton = new JButton("Send once");
        this.sendOnceButton.setBounds(10, 195, 150, 30);
        this.sendOnceButton.addActionListener(action -> this.sendPacket());
        this.sendOnceButton.setEnabled(false);
        frame.add(this.sendOnceButton);

        // Start sending button
        this.startSendingButton = new JButton("Start sending");
        this.startSendingButton.setBounds(10, 240, 150, 30);
        this.startSendingButton.addActionListener(action -> this.startSending());
        this.startSendingButton.setEnabled(false);
        frame.add(this.startSendingButton);

        // Stop sending button
        this.stopSendingButton = new JButton("Stop sending");
        this.stopSendingButton.setBounds(170, 240, 150, 30);
        this.stopSendingButton.addActionListener(action -> this.stopSending());
        this.stopSendingButton.setEnabled(false);
        frame.add(this.stopSendingButton);

        // Output field
        this.outputField = new JTextArea();
        this.outputField.setEditable(false);
        this.outputField.setLineWrap(true);
        JScrollPane outputScrollPane = new JScrollPane(this.outputField);
        outputScrollPane.setBounds(350, 10, 500, 260);
        outputScrollPane.setBorder(null);
        outputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(outputScrollPane);
    }

    @Override
    public void shutdown(){
        if(this.isSending)
            this.stopSending();
    }

    private void selectInterface(PcapNetworkInterface networkInterface){
        this.networkInterface = networkInterface;

        // Update the buttons
        this.checkAllFields();
    }

    private void updateSpoofIp(String spoofIp){
        if(spoofIp.isEmpty()){
            // Return if the address is empty
            this.spoofIp = null;
        }else{
            // Try to parse the address
            try{
                this.spoofIp = InetAddress.getByName(spoofIp);
            }catch(UnknownHostException e){
                this.spoofIp = null;
            }
        }

        // Update the buttons
        this.checkAllFields();
    }

    private void updateSpoofMac(String spoofMac){
        if(spoofMac.isEmpty()){
            // Return if the address is empty
            this.spoofMac = null;
        }else{
            // Try to parse the mac address
            try{
                this.spoofMac = MacAddress.getByName(spoofMac);
            }catch(Exception e){
                this.spoofMac = null;
            }
        }

        // Update the buttons
        this.checkAllFields();
    }

    private void updateVictimIp(String victimIp){
        if(victimIp.isEmpty()){
            // Return if the address is empty
            this.victimIp = null;
        }else{
            // Try to parse the address
            try{
                this.victimIp = InetAddress.getByName(victimIp);
            }catch(UnknownHostException e){
                this.victimIp = null;
            }
        }

        // Update the buttons
        this.checkAllFields();
    }

    private void checkAllFields(){
        // Check if all fields are filled
        boolean allValid = this.networkInterface != null && this.spoofIp != null && this.spoofMac != null && this.victimIp != null;
        // Update the buttons
        this.sendOnceButton.setEnabled(allValid);
        this.startSendingButton.setEnabled(allValid);
    }

    private void sendPacket(){
        // Open a connection
        PcapHandle connection;
        try{
            connection = NetworkUtils.openConnection(this.networkInterface, 1000);
        }catch(PcapNativeException e){
            Main.displayError("Failed to open a connection!", e);
            return;
        }

        // Send a packet
        try(connection){
            ARPSpoofing.sendSpoofedARPPacket(this.spoofIp, this.spoofMac, this.victimIp, connection);

            // Log send packet to the output field
            this.logMessage(TIMESTAMP_FORMAT.format(new Date()) + " Sending spoofed packet to '" + this.victimIp + "'");
        }catch(Exception e){
            Main.displayError("An error occurred whilst sending a packet!", e);
        }
    }

    private void startSending(){
        if(this.isSending){
            Main.displayError("Program is already sending ARP requests!");
            return;
        }
        this.isSending = true;

        // Lock all fields
        this.interfaceDropdown.setEnabled(false);
        this.spoofIpField.setEnabled(false);
        this.spoofMacField.setEnabled(false);
        this.victimIpField.setEnabled(false);
        // Disable send buttons
        this.sendOnceButton.setEnabled(false);
        this.startSendingButton.setEnabled(false);
        // Enable the stop sending button
        this.stopSendingButton.setEnabled(true);

        // Create a new thread to continuously send packets
        new Thread(() -> {
            Random random = new Random();

            // Continue only whilst isSending is true
            while(this.isSending){
                // Send a packet
                this.sendPacket();

                // Wait for 10-15 seconds
                try{
                    Thread.sleep(random.nextInt(10000, 15000));
                }catch(InterruptedException e){
                    Main.displayError("Error occurred whilst trying to sleep!", e);
                }
            }
        }, "ARP Packet Sender").start();
    }

    private void stopSending(){
        if(!this.isSending){
            Main.displayError("Program is not currently sending ARP requests!");
            return;
        }
        this.isSending = false;

        // Unlock all fields
        this.interfaceDropdown.setEnabled(true);
        this.spoofIpField.setEnabled(true);
        this.spoofMacField.setEnabled(true);
        this.victimIpField.setEnabled(true);
        // Enable send buttons
        this.sendOnceButton.setEnabled(true);
        this.startSendingButton.setEnabled(true);
        // Disable the stop sending button
        this.stopSendingButton.setEnabled(false);
    }

    private void logMessage(String message){
        this.outputField.append(message + "\n");
        System.out.println(message);
    }
}
