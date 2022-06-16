package emma;

import javax.swing.*;

/**
 * Created 12/06/2022 by SuperMartijn642
 */
public interface AttackHandler {

    /**
     * Called before any other method on the attack handler. May be used to initialize static data.
     */
    void initialize();

    /**
     * Adjusts window properties and appends components to the window. Properties such as the title, layout, and close operation will already be set.
     * @param frame the window
     */
    void initializeWindow(JFrame frame);

    /**
     * Starts any background tasks.
     */
    default void run(){
    }

    /**
     * Stops any ongoing actions and background tasks.
     */
    void shutdown();
}
