package junit.framework;

/**
 * An exception to be thrown in the application instead of Throwable
 */
public class ThrowException extends Throwable {
    private static final long serialVersionUID = -9107177676171481020L;

    public ThrowException(String msg) {
        super(msg);
    }
}
