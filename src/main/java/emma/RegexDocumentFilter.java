package emma;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

/**
 * Created 15/06/2022 by SuperMartijn642
 */
public class RegexDocumentFilter extends DocumentFilter {

    private final String regex;

    public RegexDocumentFilter(String regex){
        this.regex = regex;
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException{
        if(text.matches(this.regex))
            super.insertString(fb, offset, text, attr);
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException{
        if(text.matches(this.regex))
            super.replace(fb, offset, length, text, attrs);
    }
}
