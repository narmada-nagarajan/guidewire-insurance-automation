package com.insurance.gw.pages;

import com.insurance.gw.utils.DateUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.time.LocalDate;

/** ClaimCenter > New Claim wizard (First Notice of Loss): find policy, verify coverage, enter loss details. */
public class FnolPage extends BasePage {

    private static final By TITLE = By.id("FNOL:Title");
    private static final By POLICY_NUMBER = By.id("FNOL:policyNumber");
    private static final By LOSS_DATE = By.id("FNOL:lossDate");
    private static final By SEARCH = By.id("FNOL:Search");
    private static final By COVERAGE_STATUS = By.id("FNOL:CoverageStatus");
    private static final By LOSS_TYPE = By.id("FNOL:lossType");
    private static final By DESCRIPTION = By.id("FNOL:description");
    private static final By INJURIES = By.id("FNOL:injuries");
    private static final By FINISH = By.id("FNOL:Finish");

    public FnolPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public void searchPolicy(String policyNumber, LocalDate lossDate) {
        type(POLICY_NUMBER, policyNumber);
        type(LOSS_DATE, DateUtils.format(lossDate));
        click(SEARCH);
    }

    public boolean isPolicyVerified() {
        return text(COVERAGE_STATUS).startsWith("Policy verified");
    }

    public void enterLossDetails(String lossType, String description) {
        selectByText(LOSS_TYPE, lossType);
        type(DESCRIPTION, description);
    }

    public void setInjuriesReported(boolean reported) {
        setCheckbox(INJURIES, reported);
    }

    public void finish() {
        click(FINISH);
    }
}
