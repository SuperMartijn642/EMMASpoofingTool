package emma.dns;

import emma.DocumentChangeListener;
import emma.Main;
import emma.RegexDocumentFilter;

import javax.swing.*;
import javax.swing.text.PlainDocument;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.function.Consumer;

/**
 * Created 15/06/2022 by SuperMartijn642
 */
public class DNSEntryWindow {

    private final Consumer<DNSEntry> entryConsumer;

    private final JFrame window;
    private final JRadioButton ip4entryTypeButton, ip6entryTypeButton;
    private final JTextField domainField, ipField;
    private final JToggleButton regexButton;
    private final JLabel ipFieldLabel;
    private final JButton addButton, cancelButton;

    private boolean isV4 = true;
    private String domain = "", ip = "";
    private boolean regex;

    public DNSEntryWindow(JFrame parent, Consumer<DNSEntry> entryConsumer){
        this.entryConsumer = entryConsumer;

        // Create the window
        this.window = new JFrame("Add DNS Entry");
        this.window.setResizable(false);
        this.window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.window.setLayout(null);
        this.window.setSize(310, 275);

        // Choice between A and AAAA entries
        JLabel entryTypeLabel = new JLabel("Entry type:");
        entryTypeLabel.setBounds(10, 10, 150, 15);
        this.window.add(entryTypeLabel);
        ButtonGroup buttonGroup = new ButtonGroup();
        this.ip4entryTypeButton = new JRadioButton("A (IPv4)");
        this.ip4entryTypeButton.setBounds(10, 25, 150, 30);
        this.ip4entryTypeButton.setSelected(true);
        this.ip4entryTypeButton.addChangeListener(action -> {
            this.isV4 = this.ip4entryTypeButton.isSelected();
            this.checkFields();
        });
        buttonGroup.add(this.ip4entryTypeButton);
        this.window.add(this.ip4entryTypeButton);
        this.ip6entryTypeButton = new JRadioButton("AAAA (IPv6)");
        this.ip6entryTypeButton.setBounds(10, 50, 150, 30);
        this.ip6entryTypeButton.setSelected(false);
        buttonGroup.add(this.ip6entryTypeButton);
        this.window.add(this.ip6entryTypeButton);

        // Domain name field
        JLabel domainLabel = new JLabel("Domain name:");
        domainLabel.setBounds(10, 85, 150, 15);
        this.window.add(domainLabel);
        this.domainField = new JTextField();
        this.domainField.setBounds(10, 100, 200, 30);
        this.domainField.getDocument().addDocumentListener(new DocumentChangeListener(() -> {
            this.domain = this.domainField.getText();
            this.checkFields();
        }));
        this.window.add(this.domainField);
        // Domain regex button
        this.regexButton = new JCheckBox("Regex");
        this.regexButton.setBounds(220, 100, 80, 30);
        this.regexButton.setSelected(false);
        this.regexButton.addActionListener(action -> {
            this.regex = this.regexButton.isSelected();
            this.checkFields();
        });
        this.window.add(this.regexButton);

        // Ip text field
        this.ipFieldLabel = new JLabel();
        this.ipFieldLabel.setBounds(10, 135, 150, 15);
        this.window.add(this.ipFieldLabel);
        this.ipField = new JTextField();
        this.ipField.setBounds(10, 150, 200, 30);
        this.ipField.getDocument().addDocumentListener(new DocumentChangeListener(() -> {
            this.ip = this.ipField.getText();
            this.checkFields();
        }));
        this.window.add(this.ipField);

        // Add button
        this.addButton = new JButton("Add");
        this.addButton.setBounds(10, 195, 128, 30);
        this.addButton.addActionListener(action -> this.confirmEntry());
        this.window.add(this.addButton);

        // Cancel button
        this.cancelButton = new JButton("Cancel");
        this.cancelButton.setBounds(153, 195, 128, 30);
        this.cancelButton.addActionListener(action -> this.window.dispose());
        this.window.add(this.cancelButton);

        this.checkFields();

        // Show the window
        this.window.setLocationRelativeTo(parent);
        this.window.setVisible(true);
    }

    private void checkFields(){
        // Set domain field allowed characters
        ((PlainDocument)this.domainField.getDocument()).setDocumentFilter(new RegexDocumentFilter(this.regex ? ".*" : "[a-zA-Z0-9.]*"));
        // Set ip label text
        this.ipFieldLabel.setText(this.isV4 ? "Ip address (IPv4)" : "Ip address (IPv6)");
        // Set ip field allowed characters
        ((PlainDocument)this.ipField.getDocument()).setDocumentFilter(new RegexDocumentFilter(this.isV4 ? "[0-9.]*" : "[a-fA-F0-9:]"));
        // Update the add button
        boolean valid = (this.regex || this.domain.matches("[a-zA-Z0-9-]+([.][a-zA-Z0-9-]+){1,3}"))
            && (this.isV4 ? this.ip.matches("[0-9]{1,3}([.][0-9]{1,3}){3}") : this.ip.matches("[a-fA-F0-9]{0,4}(:[a-fA-F0-9]{0,4}){2,8}"));
        this.addButton.setEnabled(valid);
    }

    private void confirmEntry(){
        // Parse the ip address
        InetAddress address;
        try{
            address = InetAddress.getByName(this.ip);
        }catch(UnknownHostException e){
            Main.displayWarning("Could not parse IP" + (this.isV4 ? "v4" : "v6") + " address '" + this.ip + "'!", e);
            return;
        }
        // Add the entry and close the window
        this.entryConsumer.accept(new DNSEntry(this.regex, this.domain, address));
        this.window.dispose();
    }
}
