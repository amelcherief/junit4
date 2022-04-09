package org.junit.experimental.max;

/**
 * Exception raised if we can't build runner
 */
public class RunnerBuildException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RunnerBuildException(String msg) {
        super(msg);
    }
}
