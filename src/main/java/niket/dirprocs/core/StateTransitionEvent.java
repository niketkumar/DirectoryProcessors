/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs.core;

import java.io.File;
import java.util.Objects;
import java.util.Observable;
import niket.dirprocs.State;
import niket.dirprocs.StateChanged;

/**
 *
 * @author niket
 */
class StateTransitionEvent extends Observable {

    private final String id;

    StateTransitionEvent(String id) {
        this.id = id;
    }

    void fire(State fromState, State toState,
            File fromFile, File toFile,
            String message) {
        setChanged();
        notifyObservers(new StateChanged(fromState, toState,
                fromFile, toFile,
                message));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final StateTransitionEvent other = (StateTransitionEvent) obj;
        if (!Objects.equals(this.id, other.id)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + Objects.hashCode(this.id);
        return hash;
    }
}
