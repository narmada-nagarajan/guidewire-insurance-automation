package com.insurance.gw.driver;

import com.insurance.gw.config.ConfigManager;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Creates and owns the WebDriver of the current thread.
 * ThreadLocal makes parallel execution safe: every Cucumber scenario (one thread) gets its own browser.
 * Selenium Manager (built into Selenium 4.6+) downloads the matching driver binary automatically.
 */
public final class DriverFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DriverFactory.class);
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverFactory() {
    }

    public static WebDriver initDriver() {
        String browser = ConfigManager.get("browser").toLowerCase();
        boolean headless = ConfigManager.getBool("headless");
        int width = ConfigManager.getInt("window.width");
        int height = ConfigManager.getInt("window.height");

        WebDriver driver;
        switch (browser) {
            case "firefox": {
                FirefoxOptions options = new FirefoxOptions();
                if (headless) {
                    options.addArguments("-headless");
                }
                driver = new FirefoxDriver(options);
                break;
            }
            case "edge": {
                EdgeOptions options = new EdgeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=" + width + "," + height);
                driver = new EdgeDriver(options);
                break;
            }
            case "chrome":
            default: {
                ChromeOptions options = new ChromeOptions();
                if (headless) {
                    options.addArguments("--headless=new");
                }
                options.addArguments("--window-size=" + width + "," + height,
                        "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage",
                        "--remote-allow-origins=*", "--allow-file-access-from-files");
                String binary = ConfigManager.get("chrome.binary");
                if (!binary.isEmpty()) {
                    options.setBinary(binary);
                }
                driver = new ChromeDriver(options);
            }
        }

        // Best practice: NEVER mix implicit and explicit waits. We use explicit waits only (see BasePage).
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigManager.getInt("timeout.pageload")));
        if (!headless) {
            driver.manage().window().setSize(new Dimension(width, height));
        }
        DRIVER.set(driver);
        LOG.info("Started {} (headless={})", browser, headless);
        return driver;
    }

    public static WebDriver getDriver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("WebDriver not initialised for this thread - did the @Before hook run?");
        }
        return driver;
    }

    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove(); // avoids leaking the reference when threads are reused by the pool
            }
        }
    }
}
