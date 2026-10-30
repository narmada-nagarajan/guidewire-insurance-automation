@policycenter @cancellation @regression
Feature: Policy cancellation
  As a producer
  I want to cancel a policy using the right refund method
  So that the customer receives the correct return premium

  Background:
    Given the InsureSuite application is open
    And an in-force personal auto policy exists for the "STANDARD_ADULT" customer

  # Pro rata   : refund = premium x remaining days / total days
  # Short rate : same, minus a 10% penalty on the unearned premium
  # Flat       : cancelled from inception, 100% refund
  @TC08 @smoke
  Scenario Outline: Return premium is calculated according to the cancellation method
    When I start the cancellation with reason "<reason>" using the "<method>" method effective <days> days after inception
    And I accept the cancellation confirmation
    Then the policy status should be "Cancelled"
    And the policy transactions should include "Cancellation"
    And the return premium should be calculated correctly for the chosen method

    Examples:
      | reason               | method     | days |
      | Insured Request      | Pro Rata   | 90   |
      | Insured Request      | Short Rate | 90   |
      | Underwriting Reasons | Flat       | 0    |

  # The "Are you sure?" box is a native JavaScript confirm(): it is handled with driver.switchTo().alert().
  @TC09 @negative
  Scenario: Declining the confirmation dialog keeps the policy in force
    When I start the cancellation with reason "Insured Request" using the "Pro Rata" method effective 30 days after inception
    And I decline the cancellation confirmation
    And I return to the policy summary
    Then the policy status should be "In Force"
    And the policy transactions should not include "Cancellation"
