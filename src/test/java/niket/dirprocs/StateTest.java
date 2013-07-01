package niket.dirprocs;

import java.io.File;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class StateTest extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public StateTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(StateTest.class);
    }

    public void testEquals() {
        assertEquals(true, new StateA(null).equals(new StateA(null)));
        assertEquals(true, new StateB(new File("")).equals(new StateB(null)));
        assertEquals(false, new StateA(null).equals(new StateB(null)));
        assertEquals(false, new StateA(null).equals(null));
        assertEquals(false, new StateA(null).equals(new Object()));
    }

    private class StateA extends State {

        public StateA(File directory) {
            super(directory);
        }
    }

    private class StateB extends State {

        public StateB(File directory) {
            super(directory);
        }
    }
}
