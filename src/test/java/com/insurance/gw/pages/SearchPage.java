package com.insurance.gw.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.stream.Collectors;

/** Policy search (PolicyCenter Search tab). */
public class SearchPage extends BasePage {

    private static final By TITLE = By.id("Search:Title");
    private static final By POLICY_NUMBER = By.id("Search:policyNumber");
    private static final By LAST_NAME = By.id("Search:lastName");
    private static final By RUN = By.id("Search:Run");

    public SearchPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public SearchPage searchByPolicyNumber(String policyNumber) {
        type(POLICY_NUMBER, policyNumber);
        click(RUN);
        return this;
    }

    public SearchPage searchByLastName(String lastName) {
        type(LAST_NAME, lastName);
        click(RUN);
        return this;
    }

    /** Rows of the result table: [policy #, insured, status, premium]. Empty list when nothing was found. */
    public List<List<String>> results() {
        waitForLoading();
        wait.until(d -> !d.findElements(By.id("Search:Results")).isEmpty() || !d.findElements(By.id("Search:NoResults")).isEmpty());
        if (driver.findElements(By.id("Search:Results")).isEmpty()) {
            return List.of();
        }
        return tableRows("Search:Results");
    }

    public PolicySummaryPage openPolicy(String policyNumber) {
        click(By.xpath("//a[@class='policy-link'][normalize-space()='" + policyNumber + "']"));
        return new PolicySummaryPage(driver);
    }

    public List<String> resultPolicyNumbers() {
        return results().stream().map(r -> r.get(0)).collect(Collectors.toList());
    }
}
