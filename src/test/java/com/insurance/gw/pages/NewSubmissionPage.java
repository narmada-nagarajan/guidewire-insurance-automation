package com.insurance.gw.pages;

import com.insurance.gw.model.SubmissionData;
import com.insurance.gw.utils.DateUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/** PolicyCenter > New Submission wizard (single screen in the mock). */
public class NewSubmissionPage extends BasePage {

    private static final By TITLE = By.id("NewSubmission:Title");
    private static final By FIRST_NAME = By.id("NewSubmission:firstName");
    private static final By LAST_NAME = By.id("NewSubmission:lastName");
    private static final By DOB = By.id("NewSubmission:dob");
    private static final By STATE = By.id("NewSubmission:state");
    private static final By VIOLATIONS = By.id("NewSubmission:violations");
    private static final By EFFECTIVE_DATE = By.id("NewSubmission:effectiveDate");
    private static final By TERM = By.id("NewSubmission:term");
    private static final By PAYMENT_PLAN = By.id("NewSubmission:paymentPlan");
    private static final By VEHICLE_YEAR = By.id("NewSubmission:vehicleYear");
    private static final By VEHICLE_MAKE = By.id("NewSubmission:vehicleMake");
    private static final By VEHICLE_MODEL = By.id("NewSubmission:vehicleModel");
    private static final By LIABILITY = By.id("NewSubmission:liability");
    private static final By COLLISION = By.id("NewSubmission:collision");
    private static final By COMPREHENSIVE = By.id("NewSubmission:comprehensive");
    private static final By QUOTE_BUTTON = By.id("NewSubmission:Quote");

    public NewSubmissionPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public NewSubmissionPage fillSubmission(SubmissionData data) {
        type(FIRST_NAME, data.firstName);
        type(LAST_NAME, data.lastName);
        type(DOB, DateUtils.format(data.dateOfBirth()));
        selectByText(STATE, data.state);
        selectByText(VIOLATIONS, String.valueOf(data.violations));
        type(EFFECTIVE_DATE, DateUtils.format(data.effectiveDate));
        selectByText(TERM, data.term);
        selectByText(PAYMENT_PLAN, data.paymentPlan);
        type(VEHICLE_YEAR, String.valueOf(data.vehicleYear));
        selectByText(VEHICLE_MAKE, data.vehicleMake);
        type(VEHICLE_MODEL, data.vehicleModel);
        selectByText(LIABILITY, data.liability);
        selectByText(COLLISION, data.collision);
        setCheckbox(COMPREHENSIVE, data.comprehensive);
        return this;
    }

    /** Clicks Quote. The result (Quote screen or validation error) is verified by the next step. */
    public void clickQuote() {
        click(QUOTE_BUTTON);
    }
}
