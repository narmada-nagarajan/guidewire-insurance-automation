@policycenter @issuance @regression
Feature: Policy issuance (bind) and invoicing
  As a producer
  I want to bind an approved quote
  So that the customer gets an in-force policy and a bill matching the chosen payment plan

  Background:
    Given the InsureSuite application is open
    And I am logged in as a Producer

  @TC06 @smoke
  Scenario Outline: Binding a quote issues the policy and creates the invoices of the payment plan
    Given I start a new personal auto submission for the "<profile>" customer
    When I request the quote
    Then the quote should be created with status "Quoted"
    When I bind the quote and issue the policy
    Then the policy should be issued with status "In Force"
    And the policy period should span 12 months
    And the policy transactions should include "Issuance"
    When I open the billing account of the policy
    Then <invoices> invoices should be billed for the policy
    And the invoices should add up to the policy premium

    Examples:
      | profile          | invoices |
      | STANDARD_ADULT   | 1        |
      | QUARTERLY_PAYER  | 4        |
