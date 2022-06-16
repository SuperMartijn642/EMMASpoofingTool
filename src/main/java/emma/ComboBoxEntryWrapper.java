package emma;

import java.util.function.Function;

/**
 * Created 14/06/2022 by SuperMartijn642
 */
public class ComboBoxEntryWrapper<T> {

    private final T object;
    private final Function<T,String> formatter;

    public ComboBoxEntryWrapper(T object, Function<T,String> formatter){
        this.object = object;
        this.formatter = formatter;
    }

    public T getObject(){
        return this.object;
    }

    @Override
    public String toString(){
        // Info in the object may change, so reformat every time
        return this.formatter.apply(this.object);
    }
}
