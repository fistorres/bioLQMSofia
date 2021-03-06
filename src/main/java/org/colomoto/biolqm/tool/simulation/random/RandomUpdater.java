package org.colomoto.biolqm.tool.simulation.random;

import org.colomoto.biolqm.tool.simulation.LogicalModelUpdater;

/**
 * Interface for random (stochastic) updaters.
 * A stochastic random will pick a single successor for a given state,
 * but a different successor may be selected when calling it again on the same state.
 *
 * @author Aurelien Naldi
 */
public interface RandomUpdater extends LogicalModelUpdater {

    /**
     * Get the successor of a state state.
     * @param state the current state
     * @return the successor state
     */
    byte[] pickSuccessor(byte[] state);


    /**
     * Set the seed for the random generator (for reproducibility in tests, NOT for real use)
     *
     * @param seed the initial seed
     */
    void setSeed(long seed);
}
