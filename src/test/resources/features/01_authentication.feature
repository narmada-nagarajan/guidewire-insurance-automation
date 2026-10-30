@authentication @smoke
Feature: User authentication
  As an insurance employee (producer, underwriter, adjuster ...)
  I want to log in with my own credentials
  So that I only get the work and permissions that belong to my role

  Background:
    Given the InsureSuite application is open

  @TC01
  Scenario: Producer logs in successfully and lands on the Desktop
    When I log in with username "agent1" and password "gw"
    Then I should land on the Desktop
    And the logged-in role should be "Producer"

  @TC02 @negative
  Scenario Outline: Login is rejected for invalid credentials
    When I log in with username "<username>" and password "<password>"
    Then I should see the error "<message>"

    Examples: wrong password, unknown user and empty form
      | username | password | message                      |
      | agent1   | wrong    | Invalid username or password |
      | nobody   | gw       | Invalid username or password |
      |          |          | Invalid username or password |
