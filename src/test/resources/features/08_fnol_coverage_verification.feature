@claimcenter @fnol @regression
Feature: First Notice of Loss (FNOL) and coverage verification
  As a claims adjuster
  I want the system to verify the policy and coverage when a loss is reported
  So that only valid claims are created and the right exposure and deductible are applied

  Background:
    Given the InsureSuite application is open

  @TC11 @smoke
  Scenario: Collision loss with injuries creates a complex claim with a vehicle damage exposure
    Given an in-force personal auto policy exists for the "FULL_COVERAGE_FAMILY" customer
    And I am logged in as a Claims Adjuster
    When I search the policy in FNOL with a loss date 0 days from today
    Then the policy should be verified for the claim
    When I report a "Collision" loss with the description:
      """
      Insured was rear-ended while stopped at a traffic signal.
      Rear bumper and boot damaged, vehicle is drivable.
      Other driver admitted fault and shared insurance details.
      """
    And injuries were reported
    And I finish the first notice of loss
    Then a claim should be created with status "Open"
    And the claim severity should be "Complex"
    And the claim exposure should be "Vehicle Damage"
    And the claim deductible should be $250.00

  @TC12 @negative
  Scenario: Theft claim is rejected when the policy has no comprehensive coverage
    Given an in-force personal auto policy exists for the "STANDARD_ADULT" customer
    And I am logged in as a Claims Adjuster
    When I search the policy in FNOL with a loss date 0 days from today
    Then the policy should be verified for the claim
    When I report a "Comprehensive - Theft" loss with the description:
      """
      Vehicle stolen from the parking lot overnight.
      """
    And I finish the first notice of loss
    Then I should see the error "No applicable coverage found on the policy for loss type: Comprehensive - Theft"

  # Integration across PolicyCenter and ClaimCenter: a flat-cancelled policy was never in force.
  @TC13 @negative @integration
  Scenario: Claim cannot be reported on a policy that was flat cancelled
    Given an in-force personal auto policy exists for the "STANDARD_ADULT" customer
    And the policy is cancelled with the "Flat" method
    And I am logged in as a Claims Adjuster
    When I search the policy in FNOL with a loss date 0 days from today
    Then I should see the error "Policy is not in force on the date of loss"
