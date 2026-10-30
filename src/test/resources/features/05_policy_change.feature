@policycenter @endorsement @regression
Feature: Mid-term policy change (endorsement)
  As a producer
  I want to add a vehicle in the middle of the policy period
  So that the customer pays only for the part of the term the vehicle is covered

  Background:
    Given the InsureSuite application is open
    And an in-force personal auto policy exists for the "STANDARD_ADULT" customer

  @TC07
  Scenario: Adding a vehicle charges a pro-rata premium
    When I add a 2020 "Toyota" "Camry" vehicle to the policy effective 180 days after inception
    Then the policy should have 2 vehicles
    And the policy transactions should include "Endorsement"
    And the endorsement premium should be pro-rated for the remaining policy term
