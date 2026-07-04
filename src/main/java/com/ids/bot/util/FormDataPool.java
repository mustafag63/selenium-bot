package com.ids.bot.util;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * İletişim formunu doldururken kullanılacak sahte veri havuzu.
 *
 * UYARI: SUBJECTS listesi TechMarket sitesinin gerçek select seçenekleriyle birebir eşleşmeli.
 * Türkçe değerler SİLİNMEMELİ / İngilizce'ye ÇEVRİLMEMELİ — site HTML'indeki option'larla
 * birebir eşleşme zorunludur, aksi halde selectByVisibleText başarısız olur.
 */
public class FormDataPool {

    private FormDataPool() {}

    private static final List<String> NAMES = List.of(
        "Ahmet Yılmaz", "Ayşe Kaya", "Mehmet Demir", "Fatma Çelik", "Ali Şahin",
        "Zeynep Arslan", "Mustafa Öztürk", "Elif Yıldız", "Emre Doğan", "Selin Aydın"
    );

    private static final List<String> EMAILS = List.of(
        "ahmet.yilmaz@example.com", "ayse.kaya@example.com", "mehmet.demir@example.com",
        "fatma.celik@example.com", "ali.sahin@example.com", "zeynep.arslan@example.com",
        "mustafa.ozturk@example.com", "elif.yildiz@example.com", "emre.dogan@example.com",
        "selin.aydin@example.com"
    );

    /** TechMarket contact.html <select> seçenekleriyle birebir eşleşir (4 option). */
    private static final List<String> SUBJECTS = List.of(
        "Genel Bilgi", "Sipariş Durumu", "Teknik Destek", "Diğer"
    );

    private static final List<String> MESSAGES = List.of(
        "Merhaba, geçen hafta verdiğim siparişin durumunu öğrenmek istiyorum. Takip numarasını alamadım.",
        "Satın aldığım ürün açıklama ile uyuşmuyor, iade işlemi başlatmak istiyorum.",
        "Web sitesinde belirtilen garanti şartları hakkında bilgi alabilir miyim?",
        "Toplu alım için özel fiyat teklifi almak istiyorum, nasıl iletişime geçebilirim?",
        "Ürünü teslim aldım fakat kutu hasarlıydı. Değişim yapabilir misiniz?"
    );

    public static String randomName() {
        return pick(NAMES);
    }

    public static String randomEmail() {
        return pick(EMAILS);
    }

    public static String randomSubject() {
        return pick(SUBJECTS);
    }

    /** Tüm seçenek metinlerini döndürür — deterministik kapsama testleri için. */
    public static List<String> allSubjects() {
        return SUBJECTS;
    }

    public static String randomMessage() {
        return pick(MESSAGES);
    }

    private static <T> T pick(List<T> list) {
        return list.get(ThreadLocalRandom.current().nextInt(list.size()));
    }
}
