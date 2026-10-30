package com.insurance.gw.runners;

import io.cucumber.testng.AbstractTestNGCucumberTests;
import io.cucumber.testng.CucumberOptions;

/**
 * Re-runs ONLY the scenarios that failed in the previous run. TestRunner's "rerun" plugin writes their
 * locations to target/rerun.txt; "@target/rerun.txt" tells Cucumber to use that file as the feature list.
 *   mvn test -Dtest=FailedScenariosRunner
 */
@CucumberOptions(
        features = "@target/rerun.txt",
        glue = {
                "com.insurance.gw.stepdefinitions",
                "com.insurance.gw.hooks",
                "com.insurance.gw.parametertypes"
        },
        plugin = {"pretty", "html:target/cucumber-reports/rerun.html", "json:target/cucumber-reports/rerun.json"},
        monochrome = true
)
public class FailedScenariosRunner extends AbstractTestNGCucumberTests {
}
