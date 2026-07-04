package com.ids.bot.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Persona başına Markov geçiş matrislerini üreten factory sınıfı.
 *
 * Bu geçiş olasılıkları CICIDS2017 veri setinden türetilmedi — davranışsal varsayımdan geliyor.
 * RetailRocket ve UCI clickstream veri setleri bu amaçla araştırıldı ama TechMarket'in sayfa
 * yapısına uymadığı için kullanılmadı. Olasılıklar, tipik bir e-ticaret sitesindeki mantıklı
 * kullanıcı davranışını yansıtacak şekilde elle tasarlandı.
 */
public class BotPersonas {

    private BotPersonas() {}

    /**
     * Gezinme-odaklı persona: ürün detaylarına sık girer.
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
     * Ürün-odaklı persona: PRODUCTS→PRODUCT_DETAIL ağırlıklı, CONTACT'a bazen uğrar ama doldurmaz.
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
     * FormFilling senaryosu Markov zincirini kullanmıyor (faz-bazlı çalışıyor).
     * Bu model sadece entry-fazındaki "brief orientation" adımları için.
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

    /** Vararg çiftlerinden (state, prob) bir satır map'i oluşturur. */
    private static Map<BotState, Double> row(Object... pairs) {
        Map<BotState, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            m.put((BotState) pairs[i], (Double) pairs[i + 1]);
        }
        return m;
    }
}
