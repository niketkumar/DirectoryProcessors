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
public class ProcessedState {

    public final Class<? extends State> toState;
    public final String message;
    public final File processedFile;

    public ProcessedState(Class<? extends State> toState, File processedFile, String message) {
        this.toState = toState;
        this.processedFile = processedFile;
        this.message = message;
    }
}
