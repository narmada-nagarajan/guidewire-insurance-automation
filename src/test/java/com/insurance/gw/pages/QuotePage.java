package com.insurance.gw.pages;

import com.insurance.gw.utils.DateUtils;
import com.insurance.gw.utils.MoneyUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** PolicyCenter > Quote screen of a submission (rating result, UW issues, bind). */
public class QuotePage extends BasePage {

    private static final By TITLE = By.id("QuoteScreen:Title");
    private static final By SUBMISSION_NUMBER = By.id("QuoteScreen:submissionNumber");
    private static final By STATUS = By.id("QuoteScreen:status");
    private static final By PREMIUM = By.id("QuoteScreen:premium");
    private static final By EFFECTIVE = By.id("QuoteScreen:effective");
    private static final By EXPIRATION = By.id("QuoteScreen:expiration");
    private static final By DRIVER_AGE = By.id("QuoteScreen:driverAge");
    private static final By UW_ISSUES = By.xpath("//ul[@id='QuoteScreen:UWIssues']/li");
    private static final By BIND = By.id("QuoteScreen:Bind");
    private static final By APPROVE = By.id("QuoteScreen:Approve");
    private static final By REJECT = By.id("QuoteScreen:Reject");

    public QuotePage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public String submissionNumber() { return text(SUBMISSION_NUMBER); }

    public String status() { return text(STATUS); }

    public BigDecimal premium() { return MoneyUtils.parse(text(PREMIUM)); }

    public LocalDate effectiveDate() { return DateUtils.parse(text(EFFECTIVE)); }

    public LocalDate expirationDate() { return DateUtils.parse(text(EXPIRATION)); }

    public int driverAge() { return Integer.parseInt(text(DRIVER_AGE)); }

    public List<String> underwritingIssues() {
        find(UW_ISSUES);
        return driver.findElements(UW_ISSUES).stream().map(e -> e.getText().trim()).collect(Collectors.toList());
    }

    public boolean isBindEnabled() { return isEnabled(BIND); }

    public PolicySummaryPage bind() {
        click(BIND);
        return new PolicySummaryPage(driver);
    }

    public void approve() { click(APPROVE); }

    public void reject() { click(REJECT); }
}
