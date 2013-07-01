/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package example;

import java.io.File;
import java.io.FileFilter;
import niket.dirprocs.ProcessedState;
import niket.dirprocs.Processor;
import example.states.State2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author niket
 */
public class State1Processor implements Processor {

    private static final Logger logger = LoggerFactory.getLogger(State1Processor.class);

    @Override
    public ProcessedState execute(File file) {
        logger.info("Processing " + file.getName());
        return new ProcessedState(State2.class, file, null);
    }

    @Override
    public FileFilter getFileFilter() {
        return null;
    }
}
