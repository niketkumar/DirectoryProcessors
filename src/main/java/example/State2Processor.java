/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package example;

import java.io.File;
import java.io.FileFilter;
import niket.dirprocs.ProcessedState;
import niket.dirprocs.Processor;
import example.states.FailedState3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author niket
 */
public class State2Processor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(State2Processor.class);

    @Override
    public ProcessedState execute(File file) {
        logger.info("Decompressing " + file.getName());
        return new ProcessedState(FailedState3.class, file, "decompression failed");
    }

    @Override
    public FileFilter getFileFilter() {
        return null;
    }
}
