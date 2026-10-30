package com.insurance.gw.hooks;

import com.insurance.gw.config.ConfigManager;
import com.insurance.gw.context.TestContext;
import com.insurance.gw.driver.DriverFactory;
import com.insurance.gw.utils.ScreenshotUtil;
import io.cucumber.java.After;
import io.cucumber.java.AfterStep;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cucumber hooks = set-up / tear-down around every scenario.
 *  - @Before / @After  : run for every scenario ('order' decides the sequence; for @After HIGHER order runs FIRST)
 *  - @Before("@tag")   : tagged hook, runs only for scenarios carrying that tag
 *  - @AfterStep        : runs after every step (used for optional per-step screenshots)
 * The hook class receives the shared TestContext through constructor injection (PicoContainer).
 */
public class Hooks {

    private static final Logger LOG = LoggerFactory.getLogger(Hooks.class);
    private final TestContext ctx;

    public Hooks(TestContext ctx) {
        this.ctx = ctx;
    }

    @Before(order = 0)
    public void startBrowser(Scenario scenario) {
        LOG.info("START  : {} {}", scenario.getName(), scenario.getSourceTagNames());
        ctx.setScenario(scenario);
        WebDriver driver = DriverFactory.initDriver();
        ctx.setDriver(driver);
        driver.get(ConfigManager.baseUrl());
        if (ConfigManager.isMockApp()) {
            // test-data isolation: every scenario starts from an empty database
            ((JavascriptExecutor) driver).executeScript("window.localStorage.clear();");
            driver.get(ConfigManager.baseUrl());
        }
    }

    @Before(value = "@e2e", order = 1)
    public void announceEndToEndScenario(Scenario scenario) {
        LOG.info("E2E flow (multi-role, multi-application): {}", scenario.getName());
    }

    @AfterStep
    public void screenshotAfterEachStep(Scenario scenario) {
        if (ConfigManager.getBool("screenshot.every.step")) {
            scenario.attach(ScreenshotUtil.asBytes(ctx.driver()), "image/png", "step");
        }
    }

    @After(order = 1)
    public void captureFailure(Scenario scenario) {
        if (scenario.isFailed() && ctx.driver() != null) {
            scenario.attach(ScreenshotUtil.asBytes(ctx.driver()), "image/png", "FAILED - " + scenario.getName());
            ScreenshotUtil.saveToDisk(ctx.driver(), scenario.getName());
        }
    }

    @After(order = 0)
    public void closeBrowser(Scenario scenario) {
        LOG.info("{} : {}", scenario.getStatus().toString().toUpperCase(), scenario.getName());
        DriverFactory.quitDriver();
    }
}
