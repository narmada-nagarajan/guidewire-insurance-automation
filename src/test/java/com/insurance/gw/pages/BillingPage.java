package com.insurance.gw.pages;

import com.insurance.gw.utils.MoneyUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/** BillingCenter > billing account of a policy: invoices and payments. */
public class BillingPage extends BasePage {

    private static final By TITLE = By.id("Billing:Title");
    private static final By SEARCH_POLICY = By.id("Billing:searchPolicyNumber");
    private static final By SEARCH = By.id("Billing:Search");
    private static final By ACCOUNT = By.id("Billing:account");
    private static final By TOTAL_BILLED = By.id("Billing:totalBilled");
    private static final By TOTAL_PAID = By.id("Billing:totalPaid");
    private static final By BALANCE = By.id("Billing:balance");
    private static final By STATUS = By.id("Billing:status");
    private static final By PAYMENT_AMOUNT = By.id("Billing:paymentAmount");
    private static final By MAKE_PAYMENT = By.id("Billing:MakePayment");

    public BillingPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public BillingPage findAccount(String policyNumber) {
        type(SEARCH_POLICY, policyNumber);
        click(SEARCH);
        wait.until(ExpectedConditions.visibilityOfElementLocated(ACCOUNT));
        return this;
    }

    public BigDecimal totalBilled() { return MoneyUtils.parse(text(TOTAL_BILLED)); }

    public BigDecimal totalPaid() { return MoneyUtils.parse(text(TOTAL_PAID)); }

    public BigDecimal outstandingBalance() { return MoneyUtils.parse(text(BALANCE)); }

    public String billingStatus() { return text(STATUS); }

    public List<List<String>> invoices() { return tableRows("Billing:Invoices"); }

    /** Amount column (index 3) of every invoice. */
    public List<BigDecimal> invoiceAmounts() {
        return invoices().stream().map(r -> MoneyUtils.parse(r.get(3))).collect(Collectors.toList());
    }

    public void pay(BigDecimal amount) {
        type(PAYMENT_AMOUNT, amount.toPlainString());
        click(MAKE_PAYMENT);
    }
}
