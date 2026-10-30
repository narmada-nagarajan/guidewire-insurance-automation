package com.insurance.gw.pages;

import com.insurance.gw.utils.MoneyUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.util.List;

/** ClaimCenter > Claim Summary: exposure, reserves, payments, closing. */
public class ClaimSummaryPage extends BasePage {

    private static final By TITLE = By.id("ClaimSummary:Title");
    private static final By CLAIM_NUMBER = By.id("ClaimSummary:claimNumber");
    private static final By STATUS = By.id("ClaimSummary:status");
    private static final By SEVERITY = By.id("ClaimSummary:severity");
    private static final By EXPOSURE = By.id("ClaimSummary:exposureType");
    private static final By DEDUCTIBLE = By.id("ClaimSummary:deductible");
    private static final By RESERVE = By.id("ClaimSummary:reserve");
    private static final By RESERVE_STATUS = By.id("ClaimSummary:reserveStatus");
    private static final By PAID = By.id("ClaimSummary:paid");
    private static final By RESERVE_AMOUNT = By.id("ClaimSummary:reserveAmount");
    private static final By SET_RESERVE = By.id("ClaimSummary:SetReserve");
    private static final By APPROVE_RESERVE = By.id("ClaimSummary:ApproveReserve");
    private static final By PAYMENT_AMOUNT = By.id("ClaimSummary:paymentAmount");
    private static final By MAKE_PAYMENT = By.id("ClaimSummary:MakePayment");
    private static final By CLOSE_CLAIM = By.id("ClaimSummary:CloseClaim");

    public ClaimSummaryPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public String claimNumber() { return text(CLAIM_NUMBER); }

    public String status() { return text(STATUS); }

    public String severity() { return text(SEVERITY); }

    public String exposureType() { return text(EXPOSURE); }

    public BigDecimal deductible() { return MoneyUtils.parse(text(DEDUCTIBLE)); }

    public BigDecimal openReserve() { return MoneyUtils.parse(text(RESERVE)); }

    public String reserveStatus() { return text(RESERVE_STATUS); }

    public BigDecimal totalPaid() { return MoneyUtils.parse(text(PAID)); }

    public List<List<String>> payments() { return tableRows("ClaimSummary:Payments"); }

    public void setReserve(BigDecimal amount) {
        type(RESERVE_AMOUNT, amount.toPlainString());
        click(SET_RESERVE);
    }

    public void approvePendingReserve() { click(APPROVE_RESERVE); }

    public void makePayment(BigDecimal grossAmount) {
        type(PAYMENT_AMOUNT, grossAmount.toPlainString());
        click(MAKE_PAYMENT);
    }

    public void closeClaim() { click(CLOSE_CLAIM); }
}
