/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs.core;

import java.util.List;
import java.util.Observer;
import java.util.Set;
import niket.dirprocs.Processor;
import niket.dirprocs.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author niket
 */
public class StateManager {

    private static final Logger logger = LoggerFactory.getLogger(StateManager.class);
    private final StateDefinitions definitions;
    private final StateTransitions transitions;
    private final ProcessorsLauncher processorLauncher;

    public StateManager() {
        this.definitions = new StateDefinitions();
        this.transitions = new StateTransitions();
        this.processorLauncher = new ProcessorsLauncher();
    }

    public StateManager addProcessorThreads(Processor processor, int nosThreads) {
        processorLauncher.addProcessorThreads(processor.getClass(), nosThreads);
        return this;
    }

    public StateManager addDefinition(State fromState,
            Processor processor,
            Set<State> toStates) {
        this.definitions.add(fromState, processor, toStates);
        return this;
    }

    public StateManager addObserver(State fromState, State toState, Observer observer) {
        this.transitions.addObserver(fromState, toState, observer);
        return this;
    }

    public boolean startAll() {
        return processorLauncher.startAll(definitions, transitions);
    }

    public boolean stopAll() {
        return processorLauncher.stopAll();
    }

    public boolean shutdown() {
        return processorLauncher.shutdown(transitions);
    }

    public boolean stopProcessor(Class<? extends Processor> processorClass) {
        return processorLauncher.stopProcessor(processorClass);
    }

    public boolean startProcessor(Class<? extends Processor> processorClass) {
        return processorLauncher.startProcessor(processorClass,
                definitions,
                transitions);
    }

    public synchronized void updateState(State updateState) {
        logger.info("Updating State " + updateState);
        final List<Processor> restarts = definitions.getAffectedProcessors(updateState);
        if (!restarts.isEmpty()) {
            for (Processor processor : restarts) {
                stopProcessor(processor.getClass());
            }
            definitions.updateState(restarts, updateState);
            for (Processor processor : restarts) {
                startProcessor(processor.getClass());
            }
        }
    }

    synchronized public void disableProcessor(Class<? extends Processor> processorClass) {
        logger.info("Disabling Processor " + processorClass.getName());
        stopProcessor(processorClass);
        processorLauncher.addProcessorThreads(processorClass, 0);
    }

    synchronized public void enableProcessor(Class<? extends Processor> processorClass,
            int threadCount) {
        logger.info("Enabling Processor " + processorClass.getName());
        processorLauncher.addProcessorThreads(processorClass, threadCount);
        startProcessor(processorClass);
    }
}
