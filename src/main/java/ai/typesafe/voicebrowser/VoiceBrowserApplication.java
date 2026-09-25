package ai.typesafe.voicebrowser;

import ai.typesafe.voicebrowser.browser.BrowserManager;
import ai.typesafe.voicebrowser.browser.Controller;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class VoiceBrowserApplication {

    public static void main(String[] args) {
        SpringApplication.run(VoiceBrowserApplication.class, args);
    }

    @Bean(destroyMethod = "close")
    public BrowserManager browserManager() {
        BrowserManager manager = new BrowserManager();
        boolean headless = Boolean.getBoolean("headless");
        String cdp = System.getProperty("cdp");
        String startUrl = System.getProperty("startUrl", "https://example.com/");
        manager.launch(headless, cdp, ".browser-profile", startUrl);
        return manager;
    }

    @Bean(destroyMethod = "close")
    public Controller controller(BrowserManager browserManager) {
        Controller controller = new Controller(browserManager);
        controller.start();
        return controller;
    }
}
