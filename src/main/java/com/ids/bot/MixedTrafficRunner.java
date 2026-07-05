package com.ids.bot;
import com.ids.bot.scenarios.*;
import com.ids.bot.util.UserAgentPool;
import org.openqa.selenium.WebDriver;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Realistic mixed-traffic simulation: repeatedly launches bot sessions of
 * randomly weighted personas, capped at a fixed concurrency, for a fixed
 * wall-clock duration. Unlike ParallelRunner (one-shot, 3 bots, run once),
 * this models overlapping user arrivals over an extended time window.
 */
public class MixedTrafficRunner {

    private static final int MAX_CONCURRENT = 3;

    private enum Persona {
        BROWSING(0.60), SEARCHING(0.30), FORM_FILLING(0.10);

        final double weight;
        Persona(double weight) { this.weight = weight; }

        static Persona sampleWeighted() {
            double r = ThreadLocalRandom.current().nextDouble();
            double cumulative = 0.0;
            for (Persona p : values()) {
                cumulative += p.weight;
                if (r < cumulative) return p;
            }
            return BROWSING; // floating point güvenliği için fallback
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int durationMinutes = args.length > 0 ? Integer.parseInt(args[0]) : 15;
        Instant deadline = Instant.now().plus(Duration.ofMinutes(durationMinutes));

        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT);
        Semaphore slots = new Semaphore(MAX_CONCURRENT);

        AtomicInteger launched = new AtomicInteger(0);
        AtomicInteger okCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Map<Persona, AtomicInteger> perPersonaCount = new EnumMap<>(Persona.class);
        for (Persona p : Persona.values()) perPersonaCount.put(p, new AtomicInteger(0));

        System.out.printf("=== MixedTrafficRunner: %d dakika, max %d eşzamanlı bot, ağırlık 60/30/10 ===%n",
            durationMinutes, MAX_CONCURRENT);

        while (Instant.now().isBefore(deadline)) {
            // Gerçekçi varış aralığı — bir sonraki "kullanıcı" için rastgele bekleme
            Thread.sleep(ThreadLocalRandom.current().nextLong(3_000, 15_000));

            slots.acquire(); // kapasite dolu ise burada doğal olarak kuyruklanır
            Persona persona = Persona.sampleWeighted();
            int id = launched.incrementAndGet();
            perPersonaCount.get(persona).incrementAndGet();

            executor.submit(() -> {
                String threadLabel = "bot-" + persona.name().toLowerCase() + "-" + id;
                Thread.currentThread().setName(threadLabel);
                WebDriver driver = null;
                try {
                    driver = App.createDriver(UserAgentPool.randomEntry());
                    switch (persona) {
                        case BROWSING     -> new BrowsingScenario(driver).run();
                        case SEARCHING    -> new SearchingScenario(driver).run();
                        case FORM_FILLING -> new FormFillingScenario(driver).run();
                    }
                    okCount.incrementAndGet();
                    System.out.println("[" + threadLabel + "] OK");
                } catch (Exception e) {
                    failCount.incrementAndGet();
                    System.err.println("[" + threadLabel + "] FAIL -> " + e.getMessage());
                } finally {
                    if (driver != null) driver.quit();
                    slots.release();
                }
            });
        }

        System.out.println("Süre doldu — yeni bot başlatılmıyor, devam edenler bitiriliyor...");
        executor.shutdown();
        if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
            executor.shutdownNow();
        }

        System.out.println("\n================ MIXED TRAFFIC ÖZETİ ================");
        System.out.println("Toplam başlatılan: " + launched.get());
        for (Persona p : Persona.values()) {
            System.out.printf("  %-14s %d%n", p.name(), perPersonaCount.get(p).get());
        }
        System.out.println("Başarılı: " + okCount.get() + " | Başarısız: " + failCount.get());
    }
}