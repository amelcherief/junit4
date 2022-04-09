package junit.runner;

/**
 * This class defines the current version of JUnit
 */
public class Version {
    public static final String ID = "4.13.3-SNAPSHOT";
    
	private Version() {
		// don't instantiate
	}

	public static void main(String[] args) {
		System.out.println(ID);
	}
}
