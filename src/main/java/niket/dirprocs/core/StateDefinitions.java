/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import niket.dirprocs.Processor;
import niket.dirprocs.State;
import niket.util.PersistentDirectoryBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author niket
 */
class StateDefinitions {

    private static final Logger logger = LoggerFactory.getLogger(StateDefinitions.class);
    private final ConcurrentMap<Class<? extends State>, State> allStates;
    private final ConcurrentMap<Class<? extends Processor>, Processor> processors;
    private final ConcurrentMap<Class<? extends Processor>, State> processorsFromStates;
    private final ConcurrentMap<Class<? extends Processor>, Set<State>> processorsToStates;

    StateDefinitions() {
        allStates = new ConcurrentHashMap<>();
        processors = new ConcurrentHashMap<>();
        processorsFromStates = new ConcurrentHashMap<>();
        processorsToStates = new ConcurrentHashMap<>();
    }

    State lookupState(Class<? extends State> state) {
        return allStates.get(state);
    }

    synchronized void add(State fromState,
            Processor processor,
            Set<State> toStates) {
        final Class<? extends Processor> processorClass = processor.getClass();
        validateProcessorAlreadyMapped(processorClass);
        validateSameNameSourceStateAlreadyMapped(fromState);
        validateSameDirectorySourceStateAlreadyMapped(fromState);
        processors.put(processorClass, processor);
        processorsFromStates.put(processorClass, fromState);
        processorsToStates.put(processorClass, toStates);
        allStates.put(fromState.getClass(), fromState);
        for (State state : toStates) {
            allStates.put(state.getClass(), state);
        }
    }

    synchronized List<Processor> getAffectedProcessors(State updateState) {
        logger.info("Finding Processors associated to " + updateState);
        if (noChange(updateState)) {
            return Collections.EMPTY_LIST;
        }
        if (isUnknown(updateState)) {
            throw new IllegalStateException("Unknown " + updateState);
        }
        if (isSource(updateState)) {
            validateSameDirectorySourceStateAlreadyMapped(updateState);
        }
        Set<Processor> affected = new TreeSet<>(new Comparator<Processor>() {
            @Override
            public int compare(Processor o1, Processor o2) {
                return o1.getClass().getName().compareTo(o2.getClass().getName());
            }
        });
        for (Map.Entry<Class<? extends Processor>, State> entry : processorsFromStates.entrySet()) {
            if (entry.getValue().equals(updateState)) {
                affected.add(processors.get(entry.getKey()));
                break;
            }
        }
        for (Map.Entry<Class<? extends Processor>, Set<State>> entry : processorsToStates.entrySet()) {
            final Set<State> toStates = entry.getValue();
            for (State state : toStates) {
                if (state.equals(updateState)) {
                    affected.add(processors.get(entry.getKey()));
                }
            }
        }
        return new ArrayList<>(affected);
    }

    synchronized void updateState(List<Processor> affectedProcessors, State updateState) {
        allStates.put(updateState.getClass(), updateState);
        for (Processor proc : affectedProcessors) {
            final Class<? extends Processor> procClass = proc.getClass();
            logger.info("Updating Processor " + procClass.getName() + ", State " + updateState);
            if (updateState.equals(processorsFromStates.get(procClass))) {
                processorsFromStates.put(procClass, updateState);
            }
            final Set<State> toStates = processorsToStates.get(procClass);
            if (toStates != null && toStates.contains(updateState)) {
                toStates.remove(updateState);
                toStates.add(updateState);
            }
        }
    }

    synchronized List<RunnableProcessor> createRunnableProcessors(
            ProcessorsLauncher processorLauncher,
            StateTransitions transitions) {
        List<RunnableProcessor> runnables = new ArrayList<>();
        for (Map.Entry<Class<? extends Processor>, Processor> entry : processors.entrySet()) {
            final Class<? extends Processor> processorClass = entry.getKey();
            final Processor processor = entry.getValue();
            runnables.addAll(createRunnableProcessors(processorClass,
                    processor,
                    processorLauncher.threadCounts(processor),
                    transitions));
        }
        return runnables;
    }

    synchronized List<RunnableProcessor> createRunnableProcessors(
            Class<? extends Processor> processorClass,
            ProcessorsLauncher processorLauncher,
            StateTransitions transitions) {
        final Processor processor = processors.get(processorClass);
        return createRunnableProcessors(processorClass,
                processor,
                processorLauncher.threadCounts(processor),
                transitions);
    }

    private List<RunnableProcessor> createRunnableProcessors(
            final Class<? extends Processor> processorClass,
            final Processor processor,
            int threadCounts,
            StateTransitions transitions) {
        List<RunnableProcessor> runnables = new ArrayList<>();
        PersistentDirectoryBlockingQueue q = new PersistentDirectoryBlockingQueue(
                processorsFromStates.get(processorClass).directory,
                processor.getFileFilter());
        for (int i = 0; i < threadCounts; i++) {
            runnables.add(new RunnableProcessor(processor,
                    i + 1,
                    q,
                    processorsFromStates.get(processorClass),
                    Collections.unmodifiableSet(processorsToStates.get(processorClass)),
                    transitions,
                    this));
        }
        return runnables;
    }

    private void validateProcessorAlreadyMapped(Class<? extends Processor> processorClass)
            throws IllegalStateException {
        if (processors.containsKey(processorClass)) {
            throw new IllegalStateException("Processor "
                    + processorClass.getSimpleName() + " is already mapped to Source "
                    + processorsFromStates.get(processorClass));
        }
    }

    private void validateSameNameSourceStateAlreadyMapped(State fromState)
            throws IllegalStateException {
        if (processorsFromStates.containsValue(fromState)) {
            throw new IllegalStateException("Source " + fromState + " is already mapped");
        }
    }

    private void validateSameDirectorySourceStateAlreadyMapped(State fromState) {
        for (State state : processorsFromStates.values()) {
            if (!state.equals(fromState) && state.sameDirectory(fromState)) {
                throw new IllegalStateException("Source " + state + "'s directory "
                        + state.directory.getAbsolutePath() + " is already mapped");
            }
        }
    }

    private boolean isSource(State state) {
        return processorsFromStates.values().contains(state);
    }

    private boolean isUnknown(State updateState) {
        return lookupState(updateState.getClass()) == null;
    }

    private boolean noChange(State updateState) {
        final State s = lookupState(updateState.getClass());
        return s != null && s.sameDirectory(updateState);
    }
}
