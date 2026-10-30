package com.insurance.gw.pages;

import com.insurance.gw.utils.DateUtils;
import com.insurance.gw.utils.MoneyUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** PolicyCenter > Policy Summary (the policy file): status, term, premium, vehicles, transactions. */
public class PolicySummaryPage extends BasePage {

    private static final By TITLE = By.id("PolicySummary:Title");
    private static final By POLICY_NUMBER = By.id("PolicySummary:policyNumber");
    private static final By STATUS = By.id("PolicySummary:status");
    private static final By TERM = By.id("PolicySummary:term");
    private static final By EFFECTIVE = By.id("PolicySummary:effective");
    private static final By EXPIRATION = By.id("PolicySummary:expiration");
    private static final By PREMIUM = By.id("PolicySummary:premium");
    private static final By REFUND = By.id("PolicySummary:refund");
    private static final By CHANGE = By.id("PolicySummary:ChangePolicy");
    private static final By CANCEL = By.id("PolicySummary:CancelPolicy");
    private static final By RENEW = By.id("PolicySummary:RenewPolicy");
    private static final By VIEW_BILLING = By.id("PolicySummary:ViewBilling");

    public PolicySummaryPage(WebDriver driver) {
        super(driver);
        waitForPage(TITLE);
    }

    public String policyNumber() { return text(POLICY_NUMBER); }

    public String status() { return text(STATUS); }

    public int termNumber() { return Integer.parseInt(text(TERM)); }

    public LocalDate effectiveDate() { return DateUtils.parse(text(EFFECTIVE)); }

    public LocalDate expirationDate() { return DateUtils.parse(text(EXPIRATION)); }

    public BigDecimal totalPremium() { return MoneyUtils.parse(text(PREMIUM)); }

    public BigDecimal returnPremium() { return MoneyUtils.parse(text(REFUND)); }

    public int vehicleCount() { return tableRows("PolicySummary:Vehicles").size(); }

    /** Transaction type column (Issuance, Endorsement, Cancellation, Renewal ...). */
    public List<String> transactionTypes() {
        return tableRows("PolicySummary:Transactions").stream().map(r -> r.get(0)).collect(Collectors.toList());
    }

    /** Premium change column of the most recent transaction. */
    public BigDecimal lastTransactionAmount() {
        List<List<String>> rows = tableRows("PolicySummary:Transactions");
        return MoneyUtils.parse(rows.get(rows.size() - 1).get(2));
    }

    public boolean isCancelEnabled() { return isEnabled(CANCEL); }

    public PolicyChangePage startPolicyChange() {
        click(CHANGE);
        return new PolicyChangePage(driver);
    }

    public CancelPolicyPage startCancellation() {
        click(CANCEL);
        return new CancelPolicyPage(driver);
    }

    /** Renewal is processed in place; the success banner confirms it. */
    public void renew() {
        click(RENEW);
    }

    public BillingPage viewBilling() {
        click(VIEW_BILLING);
        return new BillingPage(driver);
    }
}
