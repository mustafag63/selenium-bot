package com.ids.bot.util;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * General-purpose first-order Markov chain engine.
 * Holds the transition probabilities from each BotState to the next.
 */
public class MarkovTransitionModel {

    private static final double EPSILON = 1e-9;

    private final Map<BotState, Map<BotState, Double>> matrix;

    public MarkovTransitionModel(Map<BotState, Map<BotState, Double>> matrix) {
        validate(matrix);
        this.matrix = matrix;
    }

    private void validate(Map<BotState, Map<BotState, Double>> m) {
        for (Map.Entry<BotState, Map<BotState, Double>> row : m.entrySet()) {
            double sum = row.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
            if (Math.abs(sum - 1.0) > EPSILON) {
                throw new IllegalArgumentException(
                    "Markov matrix row " + row.getKey() + " sums to " + sum + " (expected 1.0)");
            }
        }
    }

    /**
     * Returns a probability-weighted random next state for the given current state.
     * Thread-safe via ThreadLocalRandom.
     */
    public BotState nextState(BotState current) {
        Map<BotState, Double> transitions = matrix.get(current);
        if (transitions == null) {
            throw new IllegalStateException("No transitions defined for state: " + current);
        }
        double roll = ThreadLocalRandom.current().nextDouble();
        double cumulative = 0.0;
        for (Map.Entry<BotState, Double> entry : transitions.entrySet()) {
            cumulative += entry.getValue();
            if (roll < cumulative) {
                return entry.getKey();
            }
        }
        // Floating-point boundary: return the last element
        return transitions.keySet().stream().reduce((a, b) -> b).orElseThrow();
    }

    public Map<BotState, Map<BotState, Double>> getMatrix() {
        return matrix;
    }
}
