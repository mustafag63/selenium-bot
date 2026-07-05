package com.ids.bot.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Factory class that produces per-persona Markov transition matrices.
 *
 * Transition probabilities were NOT derived from CICIDS2017 — they come from behavioural
 * assumptions. RetailRocket and UCI clickstream datasets were investigated for this purpose
 * but did not map onto TechMarket's page structure. Probabilities were designed by hand
 * to reflect plausible user behaviour on a typical e-commerce site.
 */
public class BotPersonas {

    private BotPersonas() {}

    /**
     * Browsing-focused persona: frequently visits product detail pages.
     * HOME → PRODUCTS(0.45), ABOUT(0.20), CONTACT(0.15), PRODUCT_DETAIL(0.20)
     */
    public static MarkovTransitionModel browsingModel() {
        Map<BotState, Map<BotState, Double>> matrix = new LinkedHashMap<>();

        matrix.put(BotState.HOME, row(
            BotState.PRODUCTS, 0.45,
            BotState.ABOUT, 0.20,
            BotState.CONTACT, 0.15,
            BotState.PRODUCT_DETAIL, 0.20
        ));
        matrix.put(BotState.PRODUCTS, row(
            BotState.PRODUCT_DETAIL, 0.55,
            BotState.HOME, 0.20,
            BotState.ABOUT, 0.10,
            BotState.CONTACT, 0.15
        ));
        matrix.put(BotState.PRODUCT_DETAIL, row(
            BotState.PRODUCTS, 0.40,
            BotState.HOME, 0.30,
            BotState.PRODUCT_DETAIL, 0.20,
            BotState.CONTACT, 0.10
        ));
        matrix.put(BotState.ABOUT, row(
            BotState.HOME, 0.50,
            BotState.PRODUCTS, 0.30,
            BotState.CONTACT, 0.20
        ));
        matrix.put(BotState.CONTACT, row(
            BotState.HOME, 0.60,
            BotState.PRODUCTS, 0.40
        ));

        return new MarkovTransitionModel(matrix);
    }

    /**
     * Product-searching persona: heavy PRODUCTS→PRODUCT_DETAIL flow; visits CONTACT but never fills it.
     * HOME → PRODUCTS(0.60), CONTACT(0.15), ABOUT(0.10), PRODUCT_DETAIL(0.15)
     */
    public static MarkovTransitionModel searchingModel() {
        Map<BotState, Map<BotState, Double>> matrix = new LinkedHashMap<>();

        matrix.put(BotState.HOME, row(
            BotState.PRODUCTS, 0.60,
            BotState.CONTACT, 0.15,
            BotState.ABOUT, 0.10,
            BotState.PRODUCT_DETAIL, 0.15
        ));
        matrix.put(BotState.PRODUCTS, row(
            BotState.PRODUCT_DETAIL, 0.80,
            BotState.HOME, 0.10,
            BotState.CONTACT, 0.10
        ));
        matrix.put(BotState.PRODUCT_DETAIL, row(
            BotState.PRODUCTS, 0.50,
            BotState.HOME, 0.35,
            BotState.PRODUCT_DETAIL, 0.15
        ));
        matrix.put(BotState.ABOUT, row(
            BotState.PRODUCTS, 0.60,
            BotState.HOME, 0.40
        ));
        matrix.put(BotState.CONTACT, row(
            BotState.HOME, 0.70,
            BotState.PRODUCTS, 0.30
        ));

        return new MarkovTransitionModel(matrix);
    }

    /**
     * FormFilling scenario does not use a Markov chain for its main flow (phase-based).
     * This model covers only the "brief orientation" steps in the entry phase.
     * HOME → ABOUT(0.5), PRODUCTS(0.5)
     */
    public static MarkovTransitionModel formFillingModel() {
        Map<BotState, Map<BotState, Double>> matrix = new LinkedHashMap<>();

        matrix.put(BotState.HOME, row(
            BotState.ABOUT, 0.5,
            BotState.PRODUCTS, 0.5
        ));
        matrix.put(BotState.ABOUT, row(
            BotState.PRODUCTS, 0.5,
            BotState.HOME, 0.5
        ));
        matrix.put(BotState.PRODUCTS, row(
            BotState.ABOUT, 0.5,
            BotState.HOME, 0.5
        ));

        return new MarkovTransitionModel(matrix);
    }

    /** Builds a row map from vararg (state, prob) pairs. */
    private static Map<BotState, Double> row(Object... pairs) {
        Map<BotState, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((BotState) pairs[i], (Double) pairs[i + 1]);
        }
        return m;
    }
}
