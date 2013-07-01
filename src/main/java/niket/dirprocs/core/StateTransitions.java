/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs.core;

import java.io.File;
import java.util.Observer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import niket.dirprocs.State;

/**
 *
 * @author niket
 */
class StateTransitions {

    private final ConcurrentMap<String, StateTransitionEvent> stateTransitionEvents;

    StateTransitions() {
        stateTransitionEvents = new ConcurrentHashMap<>();
    }

    private String buildKey(State fromState, State toState) {
        return fromState.getClass().getName() + "->" + toState.getClass().getName();
    }

    synchronized void addObserver(State fromState, State toState, Observer observer) {
        final String key = buildKey(fromState, toState);
        if (stateTransitionEvents.containsKey(key)) {
            stateTransitionEvents.get(key).addObserver(observer);
        } else {
            final StateTransitionEvent event = new StateTransitionEvent(key);
            event.addObserver(observer);
            stateTransitionEvents.put(key, event);
        }
    }

    void fireStateChanged(State fromState, State toState,
            File from, File to,
            String message) {
        final String key = buildKey(fromState, toState);
        final StateTransitionEvent event = stateTransitionEvents.get(key);
        if (null != event) {
            event.fire(fromState, toState, from, to, message);
        }
    }

    synchronized void removeAllObservers() {
        for (StateTransitionEvent stateTransitionEvent : stateTransitionEvents.values()) {
            stateTransitionEvent.deleteObservers();
        }
    }
}
