/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs;

import java.io.File;

/**
 *
 * @author niket
 */
public class StateChanged {

    public final State fromState;
    public final State toState;
    public final File fromFile;
    public final File toFile;
    public final String message;

    public StateChanged(State fromState, State toState,
            File fromFile, File toFile,
            String message) {
        this.fromState = fromState;
        this.toState = toState;
        this.fromFile = fromFile;
        this.toFile = toFile;
        this.message = message;
    }

    @Override
    public String toString() {
        return "StateChanged{fromState: " + fromState
                + ", toState: " + toState
                + ", fromFile: " + fromFile.getName()
                + ", toFile: " + toFile.getName()
                + "}";
    }
}
