/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package niket.dirprocs;

import java.io.File;
import java.util.Objects;

/**
 *
 * @author niket
 */
public abstract class State {

    public final File directory;

    public State(File directory) {
        this.directory = directory;
    }

    public boolean sameDirectory(State other) {
        return directory.equals(other.directory);
    }

    @Override
    final public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        return getClass() == obj.getClass();
    }

    @Override
    final public int hashCode() {
        int hash = 7;
        hash = 89 * hash + Objects.hashCode(getClass());
        return hash;
    }

    @Override
    final public String toString() {
        return "State[" + getClass().getSimpleName() + "," + directory.getAbsolutePath() + "]";
    }
}
