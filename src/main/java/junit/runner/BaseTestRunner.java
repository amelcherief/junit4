package junit.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.NumberFormat;
import java.util.Properties;
import java.util.logging.Logger;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestListener;
import junit.framework.TestSuite;

import org.junit.internal.Throwables;

/**
 * Base class for all test runners.
 * This class was born live on stage in Sardinia during XP2000.
 */
public abstract class BaseTestRunner implements TestListener {
    public static final String SUITE_METHODNAME = "suite";

    private static Properties fPreferences;
    static int fgMaxMessageLength = 500;
    static boolean fgFilterStack = true;
    boolean fLoading = true;

    /*
    * Implementation of TestListener
    */
    public synchronized void startTest(Test test) {
        testStarted(test.toString());
    }

    protected static void setPreferences(Properties preferences) {
        fPreferences = preferences;
    }

    protected static Properties getPreferences() {
        if (fPreferences == null) {
            fPreferences = new Properties();
            fPreferences.put("loading", "true");
            fPreferences.put("filterstack", "true");
            readPreferences();
        }
        return fPreferences;
    }

    public static void savePreferences() throws IOException {
        FileOutputStream fos = new FileOutputStream(getPreferencesFile());
        try {
            getPreferences().store(fos, "");
        } finally {
            fos.close();
        }
    }

    public static void setPreference(String key, String value) {
        getPreferences().put(key, value);
    }

    public synchronized void endTest(Test test) {
        testEnded(test.toString());
    }

    public synchronized void addError(final Test test, final Throwable e) {
        testFailed(TestRunListener.STATUS_ERROR, test, e);
    }

    public synchronized void addFailure(final Test test, final AssertionFailedError e) {
        testFailed(TestRunListener.STATUS_FAILURE, test, e);
    }

    // TestRunListener implementation

    public void testStarted(String testName) {
        // Do nothing because it's only used to print that the test started
    }

    public void testEnded(String testName) {
        // Do nothing because it's only used to print that the test ended
    }

    public void testFailed(int status, Test test, Throwable e) {
        // Do nothing because it's only used to know the state of the test
    }

    public boolean suiteClass(String suiteClassName, Class<?> testClass) {
        try {
            testClass = loadSuiteClass(suiteClassName);
            return true;
        } catch (ClassNotFoundException e) {
            String clazz = e.getMessage();
            if (clazz == null) {
                clazz = suiteClassName;
            }
            runFailed("Class not found \"" + clazz + "\"");
            return false;
        } catch (Exception e) {
            runFailed("Error: " + e.toString());
            return false;
        }
    }
    
    public boolean suiteMethod(Method suiteMethod, Class<?> testClass) {
        try {
            suiteMethod = testClass.getMethod(SUITE_METHODNAME);
            return true;
        } catch (Exception e) {
            // try to extract a test suite automatically
            clearStatus();
            return false;
        }
    }
    
    public boolean suiteTest(Test test, Method suiteMethod) {
        boolean bool = false;
        try {
            test = (Test) suiteMethod.invoke(null); // static method
            if (test == null) {
                bool = true;
            }
        } catch (InvocationTargetException e) {
            runFailed("Failed to invoke suite():" + e.getTargetException().toString());
        } catch (IllegalAccessException e) {
            runFailed("Failed to invoke suite():" + e.toString());
        }
        return bool;
    }
    
    /**
     * Returns the Test corresponding to the given suite. This is
     * a template method, subclasses override runFailed(), clearStatus().
     */
    public Test getTest(String suiteClassName) {
        if (suiteClassName.length() <= 0) {
            clearStatus();
            return null;
        }
        Class<?> testClass = null;
        boolean continu = suiteClass(suiteClassName, testClass);
        if (!continu) {
            return null;
        }
        
        Method suiteMethod = null;
        boolean continu1 = suiteMethod(suiteMethod, testClass);
        if(! continu1) {
            return new TestSuite(testClass);
        }
        
        if (!Modifier.isStatic(suiteMethod.getModifiers())) {
            runFailed("Suite() method must be static");
            return null;
        }
        Test test = null;
        boolean continu2 = suiteTest(test, suiteMethod);
        if (!continu2) {return null;}
        else {
            clearStatus();
            return test;
        }
    }

    /**
     * Returns the formatted string of the elapsed time.
     */
    public String elapsedTimeAsString(long runTime) {
        return NumberFormat.getInstance().format((double) runTime / 1000);
    }

    /**
     * switch fgFilterStack from true to false
     */
    protected void switchFgFilterStack() {
        fgFilterStack = false;
    }
    
    /**
     * Processes the command line arguments and
     * returns the name of the suite class to run or null
     */
    protected String processArguments(String[] args) {
        String suiteName = null;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-noloading")) {
                setLoading(false);
            } else if (args[i].equals("-nofilterstack")) {
                switchFgFilterStack();
            } else if (args[i].equals("-c")) {
                if (args.length > i + 1) {
                    suiteName = extractClassName(args[i + 1]);
                } else {
                    Logger logger = Logger.getLogger("logger");
                    logger.log(null, "Missing Test class name");
                }
                i++;
            } else {
                suiteName = args[i];
            }
        }
        return suiteName;
    }

    /**
     * Sets the loading behaviour of the test runner
     */
    public void setLoading(boolean enable) {
        fLoading = enable;
    }

    /**
     * Extract the class name from a String in VA/Java style
     */
    public String extractClassName(String className) {
        if (className.startsWith("Default package for")) {
            return className.substring(className.lastIndexOf(".") + 1);
        }
        return className;
    }

    /**
     * Truncates a String to the maximum length.
     */
    public static String truncate(String s) {
        if (fgMaxMessageLength != -1 && s.length() > fgMaxMessageLength) {
            s = s.substring(0, fgMaxMessageLength) + "...";
        }
        return s;
    }

    /**
     * Override to define how to handle a failed loading of
     * a test suite.
     */
    protected abstract void runFailed(String message);

    /**
     * Returns the loaded Class for a suite name.
     */
    protected Class<?> loadSuiteClass(String suiteClassName) throws ClassNotFoundException {
        return Class.forName(suiteClassName);
    }

    /**
     * Clears the status message.
     */
    protected void clearStatus() { // Belongs in the GUI TestRunner class
    }

    protected boolean useReloadingTestSuiteLoader() {
        return getPreference("loading").equals("true") && fLoading;
    }

    private static File getPreferencesFile() {
        String home = System.getProperty("user.home");
        return new File(home, "junit.properties");
    }

    private static void readPreferences() {
        InputStream is = null;
        try {
            is = new FileInputStream(getPreferencesFile());
            setPreferences(new Properties(getPreferences()));
            getPreferences().load(is);
        } catch (IOException ignored) {
            ignored.printStackTrace();
        } catch (SecurityException ignored) {
            ignored.printStackTrace();
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }

    public static String getPreference(String key) {
        return getPreferences().getProperty(key);
    }

    public static int getPreference(String key, int dflt) {
        String value = getPreference(key);
        int intValue = dflt;
        if (value == null) {
            return intValue;
        }
        try {
            intValue = Integer.parseInt(value);
        } catch (NumberFormatException ne) {
            ne.printStackTrace();
        }
        return intValue;
    }

    /**
     * Returns a filtered stack trace
     */
    public static String getFilteredTrace(Throwable e) {
        return BaseTestRunner.getFilteredTrace(Throwables.getStacktrace(e));
    }

    /**
     * Filters stack frames from internal JUnit classes
     */
    public static String getFilteredTrace(String stack) {
        if (showStackRaw()) {
            return stack;
        }

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        StringReader sr = new StringReader(stack);
        BufferedReader br = new BufferedReader(sr);

        String line;
        try {
            while ((line = br.readLine()) != null) {
                if (!filterLine(line)) {
                    pw.println(line);
                }
            }
        } catch (IOException e) {
            return stack; // return the stack unfiltered
        }
        return sw.toString();
    }

    protected static boolean showStackRaw() {
        return !getPreference("filterstack").equals("true") || !fgFilterStack;
    }

    static boolean filterLine(String line) {
        String[] patterns = new String[]{
                "junit.framework.TestCase",
                "junit.framework.TestResult",
                "junit.framework.TestSuite",
                "junit.framework.Assert.", // don't filter AssertionFailure
                "junit.swingui.TestRunner",
                "junit.awtui.TestRunner",
                "junit.textui.TestRunner",
                "java.lang.reflect.Method.invoke("
        };
        for (int i = 0; i < patterns.length; i++) {
            if (line.contains(patterns[i])) {
                return true;
            }
        }
        return false;
    }

    static {
        fgMaxMessageLength = getPreference("maxmessage", fgMaxMessageLength);
    }

}
