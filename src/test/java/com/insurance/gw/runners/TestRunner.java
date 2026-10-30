package com.insurance.gw.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;
import org.testng.annotations.DataProvider;

/**
 * Main runner. Cucumber discovers features and glue; TestNG executes every scenario as one test.
 *
 * Run examples:
 *   mvn test                                          -> everything
 *   mvn test -Dcucumber.filter.tags="@smoke"          -> tag expression (and / or / not)
 *   mvn test -Dcucumber.filter.tags="@claims and not @e2e"
 *   mvn test -Dthreads=4                              -> parallel scenarios (DataProvider thread count)
 *   mvn test -Dbrowser=firefox -Dheadless=false
 */
@CucumberOptions(
        features = "src/test/resources/features",
        glue = {
                "com.insurance.gw.stepdefinitions",
                "com.insurance.gw.hooks",
                "com.insurance.gw.parametertypes"
        },
        plugin = {
                "pretty",
                "summary",
                "html:target/cucumber-reports/cucumber.html",
                "json:target/cucumber-reports/cucumber.json",
                "junit:target/cucumber-reports/cucumber.xml",
                "rerun:target/rerun.txt",
                "timeline:target/cucumber-reports/timeline"
        },
        monochrome = true
)
public class TestRunner extends AbstractTestNGCucumberTests {

    /** parallel = true makes TestNG run scenarios concurrently (thread count from -Dthreads). */
    @Override
    @DataProvider(parallel = true)
    public Object[][] scenarios() {
        return super.scenarios();
    }
}
