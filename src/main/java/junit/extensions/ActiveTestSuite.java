package junit.extensions;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;

/**
 * A TestSuite for active Tests. It runs each
 * test in a separate thread and waits until all
 * threads have terminated.
 * -- Aarhus Radisson Scandinavian Center 11th floor
 */
public class ActiveTestSuite extends TestSuite {
    private volatile int fActiveTestDeathCount;

    public ActiveTestSuite() {
    }

    public ActiveTestSuite(Class<? extends TestCase> theClass) {
        super(theClass);
    }

    public ActiveTestSuite(String name) {
        super(name);
    }

    public ActiveTestSuite(Class<? extends TestCase> theClass, String name) {
        super(theClass, name);
    }

    @Override
    public void run(TestResult result) {
        fActiveTestDeathCount = 0;
        super.run(result);
        waitUntilFinished();
    }

    @Override
    public void runTest(final Test test, final TestResult result) {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    // inlined due to limitation in VA/Java
                    test.run(result);
                } finally {
                    ActiveTestSuite.this.runFinished();
                }
            }
        };
        t.start();
    }

    synchronized void waitUntilFinished() {
        while (fActiveTestDeathCount < testCount()) {
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void runFinished() {
        fActiveTestDeathCount++;
        notifyAll();
    }
}