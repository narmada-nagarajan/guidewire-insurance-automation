@policycenter @quote @regression
Feature: Personal Auto quoting
  As a producer (agent)
  I want to create a submission and get a quote
  So that I can offer my customer an accurate premium

  Background:
    Given the InsureSuite application is open
    And I am logged in as a Producer

  # Scenario Outline + Examples = one scenario executed once per data row (data-driven testing).
  # Expected premiums come from the rating rules: base 800 x state x liability x driver age x violations
  # x new-vehicle surcharge x collision deductible factor (+120 comprehensive) x term/12.
  @TC03 @rating
  Scenario Outline: Premium is rated correctly for different risk profiles
    Given I start a new personal auto submission
    And the driver is "<name>" aged <age> living in "<state>" with <violations> violations
    And the vehicle is a <vehicleAge> year old "<make>" "<model>"
    And the coverages are liability "<liability>", collision deductible "<collision>" and comprehensive "<comprehensive>"
    And the policy term is "<term>" with payment plan "Full Pay"
    When I request the quote
    Then the quote should be created with status "Quoted"
    And the quote should have no underwriting issues
    And the total premium should be <premium>

    Examples: standard risk, young driver, new car with full coverage, senior with 6-month term
      | name         | age | state | violations | vehicleAge | make   | model   | liability | collision | comprehensive | term      | premium   |
      | John Carter  | 35  | OH    | 0          | 6          | Honda  | Civic   | 100/300   | 500       | No            | 12 Months | $828.00   |
      | Anita Rao    | 22  | CA    | 0          | 6          | Toyota | Corolla | 100/300   | 500       | No            | 12 Months | $1,545.60 |
      | Kiran Patel  | 45  | TX    | 1          | 1          | Toyota | Camry   | 250/500   | 250       | Yes           | 12 Months | $1,773.10 |
      | Ravi Kumar   | 70  | NY    | 0          | 8          | Ford   | Fusion  | 50/100    | 1000      | No            | 6 Months  | $561.60   |

  # A DataTable (key / value form) passes many related fields in one readable block.
  @TC04 @negative @validation
  Scenario: A driver younger than 18 cannot be quoted
    Given I start a new personal auto submission
    And I enter the submission details:
      | First name    | Sam      |
      | Last name     | Junior   |
      | Driver age    | 17       |
      | State         | OH       |
      | Vehicle year  | 2020     |
      | Vehicle make  | Honda    |
      | Vehicle model | Civic    |
    When I request the quote
    Then I should see the error "Driver must be at least 18 years old to be quoted"
