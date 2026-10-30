package com.insurance.gw.context;

import com.insurance.gw.model.SubmissionData;
import io.cucumber.java.Scenario;
import org.openqa.selenium.WebDriver;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Scenario-scoped state holder.
 * PicoContainer creates ONE instance per scenario and injects it (constructor injection) into every
 * step-definition class and hook class that asks for it. This is how separate step classes
 * (LoginSteps, PolicySteps, ClaimSteps ...) share data such as the policy number without static variables,
 * which would break parallel execution.
 */
public class TestContext {

    private WebDriver driver;
    private Scenario scenario;

    private SubmissionData submission = new SubmissionData();
    private String submissionNumber;
    private String policyNumber;
    private String claimNumber;
    private BigDecimal policyPremium;
    private LocalDate policyEffective;
    private LocalDate policyExpiration;
    private BigDecimal totalPaidToBilling = BigDecimal.ZERO;
    private String pendingLossType;
    private String pendingLossDescription;

    public WebDriver driver() { return driver; }
    public void setDriver(WebDriver driver) { this.driver = driver; }

    public Scenario scenario() { return scenario; }
    public void setScenario(Scenario scenario) { this.scenario = scenario; }

    public SubmissionData submission() { return submission; }
    public void setSubmission(SubmissionData submission) { this.submission = submission; }

    public String submissionNumber() { return submissionNumber; }
    public void setSubmissionNumber(String submissionNumber) { this.submissionNumber = submissionNumber; }

    public String policyNumber() { return policyNumber; }
    public void setPolicyNumber(String policyNumber) { this.policyNumber = policyNumber; }

    public String claimNumber() { return claimNumber; }
    public void setClaimNumber(String claimNumber) { this.claimNumber = claimNumber; }

    public BigDecimal policyPremium() { return policyPremium; }
    public void setPolicyPremium(BigDecimal policyPremium) { this.policyPremium = policyPremium; }

    public LocalDate policyEffective() { return policyEffective; }
    public void setPolicyEffective(LocalDate policyEffective) { this.policyEffective = policyEffective; }

    public LocalDate policyExpiration() { return policyExpiration; }
    public void setPolicyExpiration(LocalDate policyExpiration) { this.policyExpiration = policyExpiration; }

    public BigDecimal totalPaidToBilling() { return totalPaidToBilling; }
    public void addBillingPayment(BigDecimal amount) { this.totalPaidToBilling = this.totalPaidToBilling.add(amount); }

    public String pendingLossType() { return pendingLossType; }
    public void setPendingLossType(String pendingLossType) { this.pendingLossType = pendingLossType; }

    public String pendingLossDescription() { return pendingLossDescription; }
    public void setPendingLossDescription(String pendingLossDescription) { this.pendingLossDescription = pendingLossDescription; }
}
