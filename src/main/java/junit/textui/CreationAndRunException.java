package junit.textui;

/**
 * exception raised if we could not create or run the test
 */
public class CreationAndRunException extends Exception {
    private static final long serialVersionUID = 1L;

    public CreationAndRunException(String msg) {
        super(msg);
    }
}
