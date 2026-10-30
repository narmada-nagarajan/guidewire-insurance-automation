@claimcenter @financials @regression
Feature: Claim reserves, payments and closure
  As a claims adjuster
  I want to reserve and pay a claim within my authority limits
  So that financial controls are respected and claims are settled correctly

  Background:
    Given the InsureSuite application is open
    And an in-force personal auto policy exists for the "FULL_COVERAGE_FAMILY" customer
    And a collision claim has been reported on the policy

  # Authority limits: adjuster = $5,000, supervisor = $100,000.
  @TC14 @workflow @authority
  Scenario: Reserve above the adjuster's authority needs supervisor approval
    When I set the claim reserve to $8,000.00
    Then I should see the message "exceeds your authority"
    And the reserve status should be "Pending Approval"
    And the open reserve should be $0.00
    When I switch to the Claims Supervisor user
    And I open the reserve approval activity for the claim
    And I approve the pending reserve
    Then I should see the message "Reserve approved"
    And the reserve status should be "Approved"
    And the open reserve should be $8,000.00

  # The $250 collision deductible is taken from the first payment.
  @TC15 @smoke
  Scenario: Payment is net of the deductible, cannot exceed the reserve and the claim can then be closed
    When I set the claim reserve to $2,000.00
    Then the open reserve should be $2,000.00
    When I make a claim payment of $2,500.00
    Then I should see the error "Payment exceeds the open reserve"
    When I make a claim payment of $2,000.00
    Then the latest claim payment should have a net amount of $1,750.00
    And the total paid on the claim should be $1,750.00
    And the open reserve should be $0.00
    When I close the claim
    Then the claim status should be "Closed"
