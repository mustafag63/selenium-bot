package com.ids.bot;

import com.ids.bot.scenarios.*;
import com.ids.bot.util.UserAgentPool;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.concurrent.*;

public class ParallelRunner {

    public record BotResult(String personaName, boolean success, String errorMessage) {
        static BotResult ok(String name)                    { return new BotResult(name, true, null); }
        static BotResult failure(String name, String msg)  { return new BotResult(name, false, msg); }
    }

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3, r -> {
            Thread t = new Thread(r);
            return t;
        });

        Callable<BotResult> browsingTask = () -> {
            Thread.currentThread().setName("bot-browsing");
            WebDriver driver = null;
            try {
                driver = App.createDriver(UserAgentPool.randomEntry());
                new BrowsingScenario(driver).run();
                return BotResult.ok("browsing");
            } catch (Exception e) {
                System.err.println("[bot-browsing] ERROR: " + e.getMessage());
                return BotResult.failure("browsing", e.getMessage());
            } finally {
                if (driver != null) driver.quit();
            }
        };

        Callable<BotResult> searchingTask = () -> {
            Thread.currentThread().setName("bot-searching");
            WebDriver driver = null;
            try {
                driver = App.createDriver(UserAgentPool.randomEntry());
                new SearchingScenario(driver).run();
                return BotResult.ok("searching");
            } catch (Exception e) {
                System.err.println("[bot-searching] ERROR: " + e.getMessage());
                return BotResult.failure("searching", e.getMessage());
            } finally {
                if (driver != null) driver.quit();
            }
        };

        Callable<BotResult> formFillingTask = () -> {
            Thread.currentThread().setName("bot-formfilling");
            WebDriver driver = null;
            try {
                driver = App.createDriver(UserAgentPool.randomEntry());
                new FormFillingScenario(driver).run();
                return BotResult.ok("formfilling");
            } catch (Exception e) {
                System.err.println("[bot-formfilling] ERROR: " + e.getMessage());
                return BotResult.failure("formfilling", e.getMessage());
            } finally {
                if (driver != null) driver.quit();
            }
        };

        List<Future<BotResult>> futures = executor.invokeAll(
            List.of(browsingTask, searchingTask, formFillingTask)
        );

        executor.shutdown();
        if (!executor.awaitTermination(15, TimeUnit.MINUTES)) {
            executor.shutdownNow();
        }

        // Sonuç özeti
        System.out.println("\n================ PARALEL KOŞU ÖZETİ ================");
        String[] personas = {"browsing", "searching", "formfilling"};
        String[] threads  = {"bot-browsing", "bot-searching", "bot-formfilling"};
        int ok = 0;
        for (int i = 0; i < futures.size(); i++) {
            try {
                BotResult r = futures.get(i).get();
                String status = r.success() ? "OK  " : "FAIL";
                if (r.success()) ok++;
                System.out.printf("%-4s %-12s (%s)%n", status, r.personaName(), threads[i]);
                if (!r.success()) System.out.println("     -> " + r.errorMessage());
            } catch (ExecutionException e) {
                System.out.printf("FAIL %-12s (%s) -> %s%n", personas[i], threads[i], e.getCause());
            }
        }
        System.out.println(ok + "/3 bot başarıyla tamamlandı.");
    }
}
