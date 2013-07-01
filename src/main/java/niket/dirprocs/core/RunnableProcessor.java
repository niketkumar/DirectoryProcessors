/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Set;
import niket.dirprocs.ProcessedState;
import niket.dirprocs.Processor;
import niket.dirprocs.State;
import niket.util.PersistentDirectoryBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author niket
 */
class RunnableProcessor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(RunnableProcessor.class);
    private final String name;
    private final int instanceId;
    private final PersistentDirectoryBlockingQueue queue;
    private final State fromState;
    private final Set<? extends State> toStates;
    private final StateTransitions stateTransitions;
    private final StateDefinitions stateDefinitions;
    private final Processor processor;

    RunnableProcessor(Processor processor,
            int instanceId,
            PersistentDirectoryBlockingQueue queue,
            State fromState,
            Set<? extends State> toStates,
            StateTransitions stateTransitions,
            StateDefinitions stateDefinitions) {
        this.processor = processor;
        this.name = this.processor.getClass().getSimpleName();
        this.instanceId = instanceId;
        this.queue = queue;
        this.fromState = fromState;
        this.toStates = toStates;
        this.stateTransitions = stateTransitions;
        this.stateDefinitions = stateDefinitions;
    }

    Class<? extends Processor> getProcessorClass() {
        return this.processor.getClass();
    }

    @Override
    public void run() {
        logger.info("Launching " + this);
        while (!Thread.currentThread().isInterrupted()) {
            File file = null;
            try {
                file = this.queue.take();
            } catch (IOException ex) {
                logger.error(ex.getMessage(), ex);
            }
            if (file != null) {
                final ProcessedState processedState = this.processor.execute(file);
                final State toState = this.stateDefinitions.lookupState(processedState.toState);
                final String message = processedState.message;
                final File processedFile = processedState.processedFile;
                try {
                    if (this.toStates.contains(toState)) {
                        final File moved = Files.move(processedFile.toPath(),
                                new File(toState.directory, processedFile.getName()).toPath(),
                                StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.ATOMIC_MOVE).toFile();
                        this.stateTransitions.fireStateChanged(
                                this.fromState,
                                toState,
                                file,
                                moved,
                                message);
                    } else {
                        throw new IllegalStateException("Illegal State Transition: "
                                + fromState + "->" + toState
                                + ". File: " + file.getAbsolutePath());
                    }
                } catch (IOException | IllegalStateException ex) {
                    logger.error(ex.getMessage(), ex);
                }
            }
        }
        logger.warn("Processor " + this + " is stopping");
    }

    @Override
    public String toString() {
        return name + "[" + instanceId + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final RunnableProcessor other = (RunnableProcessor) obj;
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (this.instanceId != other.instanceId) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 59 * hash + Objects.hashCode(this.name);
        hash = 59 * hash + this.instanceId;
        return hash;
    }
}
