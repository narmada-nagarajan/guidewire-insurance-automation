package com.insurance.gw.pages;

import com.insurance.gw.utils.DateUtils;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;

/** PolicyCenter > Cancel Policy. Demonstrates handling of a native JavaScript confirm() dialog. */
public class CancelPolicyPage extends BasePage {

    private static final By TITLE = By.id("CancelPolicy:Title");
    private static final By REASON = By.id("CancelPolicy:reason");
    private static final By METHOD = By.id("CancelPolicy:method");
    private static final By EFFECTIVE_DATE = By.id("CancelPolicy:effectiveDate");
    private static final By SUBMIT = By.id("CancelPolicy:Submit");
    private static final By BACK = By.id("CancelPolicy:Back");

    public CancelPolicyPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public void startCancellation(String reason, String method, LocalDate effective) {
        selectByText(REASON, reason);
        selectByText(METHOD, method);
        type(EFFECTIVE_DATE, DateUtils.format(effective));
        click(SUBMIT);
    }

    /** Waits for the native browser dialog (it is NOT part of the DOM so normal locators cannot see it). */
    private Alert waitForConfirmation() {
        return wait.until(ExpectedConditions.alertIsPresent());
    }

    public String confirmationText() {
        return waitForConfirmation().getText();
    }

    public void acceptConfirmation() {
        waitForConfirmation().accept();
    }

    public void dismissConfirmation() {
        waitForConfirmation().dismiss();
    }

    public PolicySummaryPage backToPolicy() {
        click(BACK);
        return new PolicySummaryPage(driver);
    }
}
