package emma;

import javax.swing.*;
import java.io.*;
/**
 * Created 12/06/2022 by SuperMartijn642
 */
public class Main {

    public static final String TITLE = "EMMA Spoofing Tool";

    public static AttackModeContainer activeAttackHandler;

    public static void main(String[] args){
        // Prepend the current attack mode to console output
        System.setOut(new PrintStream(System.out) {
            @Override
            public void println(String x){
                super.println(activeAttackHandler == null ? x : "[" + activeAttackHandler.getAttackMode().getTitle() + "] " + x);
            }
        });

        // Make sure we catch any exceptions, for better user experience
        try{
            // Start the tool selector
            startToolSelector();
        }catch(Exception e){
            displayError("Program crashed!", e);
        }
    }

    public static void startToolSelector(){
        if(activeAttackHandler != null)
            throw new IllegalStateException("There's an attack mode active!");

        ToolSelectorWindow.show();
    }

    public static void startAttackMode(AttackMode attackMode){
        if(activeAttackHandler != null)
            throw new IllegalStateException("There's already an attack mode active!");

        System.out.println("[INFO] Starting attack mode '" + attackMode + "'!");
        activeAttackHandler = new AttackModeContainer(attackMode);
        activeAttackHandler.run();
    }

    public static void exitAttackMode(){
        if(activeAttackHandler == null)
            throw new IllegalStateException("There's no attack mode active!");

        AttackMode attackMode = activeAttackHandler.getAttackMode();
        activeAttackHandler.exit();
        activeAttackHandler = null;
        System.out.println("[INFO] Exiting attack mode '" + attackMode + "'!");

        startToolSelector();
    }

    public static void displayWarning(String message, Exception e){
        System.out.println("[WARNING] " + message);
        if(e != null)
            e.printStackTrace();
        // TODO create window which also shows the exception stack trace
        JOptionPane.showMessageDialog(null, message, "Warning", JOptionPane.WARNING_MESSAGE);
    }

    public static void displayWarning(String message){
        displayWarning(message, null);
    }
    public static void displayError(String message, Exception e){
        System.err.println("[ERROR] " + message);
        if(e != null)
            e.printStackTrace();
        // TODO create window which also shows the exception stack trace
        JOptionPane.showMessageDialog(null, message, "Error", JOptionPane.WARNING_MESSAGE);
    }

    public static void displayError(String message){
        displayError(message, null);
    }
}
