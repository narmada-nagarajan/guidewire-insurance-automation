@policycenter @renewal @regression
Feature: Policy renewal
  As a producer
  I want to renew an in-force policy
  So that the customer keeps continuous coverage for the next term

  Background:
    Given the InsureSuite application is open
    And an in-force personal auto policy exists for the "STANDARD_ADULT" customer

  @TC10
  Scenario: Renewal creates the next policy term with a 5 percent rate increase
    When I renew the policy
    Then the policy should be renewed into term 2
    And the renewal term should start when the expiring term ends
    And the renewal premium should be 5 percent higher than the expiring premium
    And the policy transactions should include "Renewal"
