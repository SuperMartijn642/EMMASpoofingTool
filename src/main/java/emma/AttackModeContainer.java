package emma;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created 12/06/2022 by SuperMartijn642
 */
public class AttackModeContainer {

    private final AttackMode mode;
    private final AttackHandler handler;

    private JFrame window;

    public AttackModeContainer(AttackMode mode){
        this.mode = mode;
        this.handler = mode.createInstance();
    }

    public void run(){
        // Let the handler initialize
        this.handler.initialize();
        // Create the window
        this.createWindow();
        // Now let the handler take over
        this.handler.run();
    }

    private void createWindow(){
        // Create a basic window
        this.window = new JFrame(Main.TITLE + " | " + this.mode.getTitle());
        this.window.setResizable(false);
        this.window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.window.setLayout(null);
        this.window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event){
                int response = JOptionPane.showConfirmDialog(AttackModeContainer.this.window, "Do you want to exit the current attack mode?", "Exit " + AttackModeContainer.this.mode.getTitle(), JOptionPane.OK_CANCEL_OPTION);
                if(response == JOptionPane.OK_OPTION)
                    Main.exitAttackMode();
            }
        });

        // Let the attack handler populate the window
        this.handler.initializeWindow(this.window);

        // Position the window in the center of the screen and show it
        this.window.setLocationRelativeTo(null);
        this.window.show();
    }

    public void exit(){
        // First let the attack handler shutdown any active processes
        this.handler.shutdown();
        // Dispose of the window
        this.window.dispose();
    }

    public AttackMode getAttackMode(){
        return this.mode;
    }
}
