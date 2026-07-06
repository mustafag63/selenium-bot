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
 *
 * <p><b>Concurrency design:</b> The arrival loop never blocks on capacity —
 * it records each arrival immediately (persona sampling, counter increment)
 * and delegates to the executor. The submitted task acquires a semaphore
 * permit and blocks only its own thread, not the arrival timing. This keeps
 * the arrival process statistically independent of slot availability.
 *
 * <p><b>Why newCachedThreadPool instead of newFixedThreadPool(MAX_CONCURRENT):</b>
 * A fixed pool of size MAX_CONCURRENT would create two overlapping capacity
 * constraints (pool size AND semaphore), making the semaphore redundant and
 * coupling two invariants that must always stay in sync. With a cached pool,
 * the semaphore is the single, authoritative concurrency gate.
 * Note: newFixedThreadPool would NOT deadlock here — a queued task holds no
 * resources while waiting for a thread, so circular wait cannot form.
 *
 * <p><b>Thread growth and Little's Law:</b> newCachedThreadPool() creates a
 * new OS thread per submitted task. With IAT uniform(28s, 72s) the mean
 * arrival rate is λ=1/50 s⁻¹. At W≈102 s weighted-average session duration,
 * Little's Law gives L=λW≈0.68 in-flight sessions on average — well below
 * MAX_CONCURRENT=3, so the backlog does not grow without bound. After the
 * deadline, at most ~2 sessions may still be running; the natural drain tail
 * is ≲290 s (≈ one maximum-length BROWSING session). The 1-day
 * awaitTermination ensures no session is ever force-killed.
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
            return BROWSING; // floating-point safety fallback
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int durationMinutes = args.length > 0 ? Integer.parseInt(args[0]) : 15;
        Instant deadline = Instant.now().plus(Duration.ofMinutes(durationMinutes));

        // newCachedThreadPool: the semaphore is the real concurrency gate.
        // A fixed pool of size MAX_CONCURRENT would starve queued tasks waiting for a slot.
        ExecutorService executor = Executors.newCachedThreadPool();
        Semaphore slots = new Semaphore(MAX_CONCURRENT);

        AtomicInteger launched = new AtomicInteger(0);
        AtomicInteger okCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        Map<Persona, AtomicInteger> perPersonaCount = new EnumMap<>(Persona.class);
        for (Persona p : Persona.values()) perPersonaCount.put(p, new AtomicInteger(0));

        System.out.printf("=== MixedTrafficRunner: %d min, max %d concurrent bots, weights 60/30/10 ===%n",
            durationMinutes, MAX_CONCURRENT);

        while (Instant.now().isBefore(deadline)) {
            Thread.sleep(ThreadLocalRandom.current().nextLong(28_000, 72_000));
            if (!Instant.now().isBefore(deadline)) break;

            // Arrival is recorded now — independently of slot availability.
            Persona persona = Persona.sampleWeighted();
            int id = launched.incrementAndGet();
            perPersonaCount.get(persona).incrementAndGet();

            executor.submit(() -> {
                String threadLabel = "bot-" + persona.name().toLowerCase() + "-" + id;
                Thread.currentThread().setName(threadLabel);
                // Capacity gate: only this task thread blocks, not the arrival loop.
                try {
                    slots.acquire();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
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

        System.out.println("Deadline reached — no new bots, waiting for in-flight sessions to finish...");
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS); // no session is ever force-killed

        System.out.println("\n================ MIXED TRAFFIC SUMMARY ================");
        System.out.println("Total launched: " + launched.get());
        for (Persona p : Persona.values()) {
            System.out.printf("  %-14s %d%n", p.name(), perPersonaCount.get(p).get());
        }
        System.out.println("OK: " + okCount.get() + " | FAIL: " + failCount.get());
    }
}