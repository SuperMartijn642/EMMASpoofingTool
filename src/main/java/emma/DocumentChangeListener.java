package emma;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * Created 15/06/2022 by SuperMartijn642
 */
public record DocumentChangeListener(Runnable listener) implements DocumentListener {

    @Override
    public void insertUpdate(DocumentEvent e){
        this.listener.run();
    }

    @Override
    public void removeUpdate(DocumentEvent e){
        this.listener.run();
    }

    @Override
    public void changedUpdate(DocumentEvent e){
        this.listener.run();
    }
}
