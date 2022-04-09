package junit.textui;

/**
 * exception raised if the test runner is used incorrectly
 */
public class TestRunnerUse extends Exception {
    private static final long serialVersionUID = 1L;

    public TestRunnerUse(String msg) {
        super(msg);
    }
}
