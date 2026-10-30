@e2e @smoke @regression
Feature: End-to-end policy lifecycle across PolicyCenter, BillingCenter and ClaimCenter
  As the business
  I want a risky customer to be quoted, approved, issued, billed and later paid on a claim
  So that the whole insurance value chain works for five different roles

  @TC17
  Scenario: Quote to claim settlement for a high risk driver
    # ---- 1. PolicyCenter: producer quotes, the rules refer the risk to underwriting
    Given I am logged in as a Producer
    And I start a new personal auto submission for the "HIGH_RISK_DRIVER" customer
    When I request the quote
    Then the quote should be created with status "Referred"
    And the quote should list the underwriting issue "Three or more violations in the last 3 years"
    And the Bind button should be disabled

    # ---- 2. Underwriting: referral activity, approval, bind / issue
    Then a referral activity for the submission should be waiting on the Underwriter desktop
    When I open the referral activity for the submission
    And I approve the referred submission
    Then the submission status should be "Approved"
    When I bind the quote and issue the policy
    Then the policy should be issued with status "In Force"
    And the policy transactions should include "Issuance"

    # ---- 3. BillingCenter: the premium is invoiced and paid
    When I switch to the Billing Clerk user
    And I open the billing account of the policy
    And I pay the full outstanding balance
    Then the billing status should be "Paid in Full"

    # ---- 4. Search for the policy
    When I search for the policy by its policy number
    Then the search results should contain the policy with status "In Force"

    # ---- 5. ClaimCenter: FNOL, reserve above authority, supervisor approval, payment, closure
    When I switch to the Claims Adjuster user
    And I search the policy in FNOL with a loss date 0 days from today
    Then the policy should be verified for the claim
    When I report a "Collision" loss with the description:
      """
      Insured lost control on a wet road and hit the median barrier.
      Front end heavily damaged, vehicle towed to a body shop.
      """
    And I finish the first notice of loss
    Then a claim should be created with status "Open"
    When I set the claim reserve to $8,000.00
    Then I should see the message "exceeds your authority"
    When I switch to the Claims Supervisor user
    And I open the reserve approval activity for the claim
    And I approve the pending reserve
    And I make a claim payment of $8,000.00
    Then the latest claim payment should have a net amount of $7,500.00
    When I close the claim
    Then the claim status should be "Closed"
