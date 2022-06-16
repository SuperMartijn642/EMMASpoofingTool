package emma;

import javax.swing.*;

/**
 * Created 12/06/2022 by SuperMartijn642
 */
public class ToolSelectorWindow {

    private static JFrame frame;
    private static JList<AttackMode> modeList;
    private static JTextArea modeDescription;
    private static JButton confirmButton;

    private static void createWindow(){
        frame = new JFrame(Main.TITLE);
        frame.setResizable(false);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(390, 295);
        frame.setLocationRelativeTo(null);
        frame.setLayout(null);

        modeList = new JList<>(AttackMode.values());
        modeList.setBounds(10, 10, 150, 200);
        modeList.setSelectedIndex(-1);
        modeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        modeList.addListSelectionListener(event -> {
            confirmButton.setEnabled(event.getFirstIndex() >= 0);
            modeDescription.setText(event.getFirstIndex() >= 0 ? modeList.getSelectedValue().getDescription() : "");
        });
        frame.add(modeList);

        modeDescription = new JTextArea();
        modeDescription.setEditable(false);
        modeDescription.setLineWrap(true);
        modeDescription.setWrapStyleWord(true);
        modeDescription.setBounds(165, 10, 200, 235);
        frame.add(modeDescription);

        confirmButton = new JButton("Select");
        confirmButton.setBounds(10 + modeList.getWidth() / 2 - 40, 215, 80, 30);
        confirmButton.setEnabled(false);
        confirmButton.addActionListener(action -> confirm());
        frame.add(confirmButton);
    }

    public static void show(){
        createWindow();
        frame.setVisible(true);
    }

    public static void hide(){
        frame.dispose();
        frame = null;
        modeList = null;
        modeDescription = null;
        confirmButton = null;
    }

    private static void confirm(){
        AttackMode mode = modeList.getSelectedValue();
        hide();
        Main.startAttackMode(mode);
    }
}
