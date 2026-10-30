@billingcenter @billing @regression
Feature: Billing payments
  As a billing clerk
  I want to apply customer payments to the policy invoices
  So that the account balance and billing status are always correct

  Background:
    Given the InsureSuite application is open
    And an in-force personal auto policy exists for the "STANDARD_ADULT" customer
    And I am logged in as a Billing Clerk

  @TC16
  Scenario: Partial payment, rejected overpayment and payment in full
    When I open the billing account of the policy
    And I pay 50 percent of the policy premium
    Then the billing status should be "Partially Paid"
    And the outstanding balance should be the policy premium less the payments made
    When I try to pay $10.00 more than the outstanding balance
    Then I should see the error "Payment exceeds the outstanding balance"
    When I pay the full outstanding balance
    Then the billing status should be "Paid in Full"
    And the outstanding balance should be $0.00
