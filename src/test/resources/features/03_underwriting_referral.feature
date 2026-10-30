@policycenter @underwriting @regression
Feature: Underwriting referral
  As an insurer
  I want risky submissions blocked from binding until an underwriter approves them
  So that we only accept risks that meet our underwriting guidelines

  Background:
    Given the InsureSuite application is open
    And I am logged in as a Producer

  # Rule (Gherkin 6+) groups the scenarios that illustrate ONE business rule.
  Rule: Submissions that trigger an underwriting rule are referred and cannot be bound by the producer

    @TC05 @workflow
    Scenario Outline: Referred submission is approved by the underwriter and then bound
      Given I start a new personal auto submission for the "<profile>" customer
      When I request the quote
      Then the quote should be created with status "Referred"
      And the quote should list the underwriting issue "<issue>"
      And the Bind button should be disabled
      Then a referral activity for the submission should be waiting on the Underwriter desktop
      When I open the referral activity for the submission
      And I approve the referred submission
      Then the submission status should be "Approved"
      And the Bind button should be enabled
      When I bind the quote and issue the policy
      Then the policy should be issued with status "In Force"

      Examples:
        | profile                     | issue                                         |
        | YOUNG_DRIVER_WITH_VIOLATION | Young driver with prior violations            |
        | HIGH_RISK_DRIVER            | Three or more violations in the last 3 years  |
