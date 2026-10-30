# Guidewire-style Insurance Test Automation
**Selenium 4 + Java 17 + Cucumber 7 (BDD) + TestNG + PicoContainer + AssertJ**

An end-to-end UI automation framework for a **Personal Auto insurance** platform that behaves like the three core
Guidewire applications:

| Guidewire application | What is covered here |
|---|---|
| **PolicyCenter** | New submission, rating/quote, underwriting referral, bind/issue, policy change (endorsement), cancellation, renewal, search |
| **ClaimCenter** | First Notice of Loss (FNOL), coverage verification, exposure, reserve (authority limits), payment (deductible), close |
| **BillingCenter** | Invoices per payment plan, payments, billing status |

> **Why a mock application?** Real Guidewire is licensed software with no public demo. The project therefore ships
> with `InsureSuite`, a small web app (`src/test/resources/mockapp`) that reproduces Guidewire's *business flows*,
> role-based users, activities, AJAX "loading" behaviour and PCF-style element ids (`NewSubmission:firstName`).
> The framework is environment-agnostic: point it at a real environment with `-Dbase.url=https://...`.

## Prerequisites
* JDK 17+ and Maven 3.8+
* Google Chrome (Selenium Manager downloads the matching chromedriver automatically; Firefox/Edge also supported)

## Run
```bash
mvn clean test                                             # all scenarios, headless Chrome, 2 parallel threads
mvn test -Dcucumber.filter.tags="@smoke"                   # smoke pack
mvn test -Dcucumber.filter.tags="@claimcenter and not @e2e"
mvn test -Dcucumber.filter.tags="@TC17"                    # a single test case by id
mvn test -Dheadless=false -Dthreads=1                      # watch the browser
mvn test -Dbrowser=firefox
mvn test -Dtest=FailedScenariosRunner                      # re-run only the scenarios that failed last time
```
Reports: `target/cucumber-reports/cucumber.html` (open in a browser), `cucumber.json`, `cucumber.xml` (JUnit),
`timeline/` (parallel execution view). Failure screenshots: `target/screenshots/` and embedded in the HTML report.

## Project structure
```
src/test
├── java/com/insurance/gw
│   ├── config/            ConfigManager          – config.properties + -D overrides
│   ├── driver/            DriverFactory          – ThreadLocal WebDriver (parallel safe)
│   ├── pages/             Page Object Model      – BasePage + one class per screen
│   ├── model/             SubmissionData, UserRole – test-data objects
│   ├── utils/             PremiumCalculator (test oracle), MoneyUtils, DateUtils, TestDataReader, ScreenshotUtil
│   ├── context/           TestContext            – scenario state shared via PicoContainer DI
│   ├── parametertypes/    {money}, {role}        – custom Cucumber parameter types
│   ├── hooks/             Hooks                  – @Before / @After / @AfterStep / tagged hooks
│   ├── stepdefinitions/   Login, Submission, Underwriting, Policy, Claim, Billing steps
│   └── runners/           TestRunner (parallel), FailedScenariosRunner (rerun)
└── resources
    ├── features/          11 feature files, 17 scenarios (24 executions with Examples rows)
    ├── testdata/          customers.json         – named customer profiles
    ├── mockapp/           InsureSuite mock (HTML/CSS/JS)
    ├── config.properties, cucumber.properties, simplelogger.properties
docs/INTERVIEW_GUIDE.md    – concepts, flow explanation and Q&A for interviews
```

## Test cases
| ID | Feature | Scenario | Key concept |
|---|---|---|---|
| TC01 | Authentication | Producer logs in | smoke, role-based access |
| TC02 | Authentication | Invalid credentials (3 rows) | Scenario Outline, negative testing |
| TC03 | Quote & rating | Premium for 4 risk profiles | rating factors, data-driven |
| TC04 | Quote & rating | Driver under 18 rejected | DataTable, validation |
| TC05 | Underwriting | Referral → UW approval → bind (2 rows) | UW rules, activities, `Rule:` keyword |
| TC06 | Issuance & billing | Bind creates policy + invoices (Full Pay / Quarterly) | bind/issue, payment plan |
| TC07 | Policy change | Add vehicle mid-term, pro-rata premium | endorsement, pro-rata |
| TC08 | Cancellation | Pro rata / short rate / flat refund | cancellation methods |
| TC09 | Cancellation | Decline confirmation dialog | JS alert handling |
| TC10 | Renewal | Renew with 5% rate change | renewal, policy term |
| TC11 | FNOL | Collision with injuries → complex claim | FNOL, exposure, deductible, DocString |
| TC12 | FNOL | Theft without comprehensive cover rejected | coverage verification |
| TC13 | FNOL | Claim on flat-cancelled policy rejected | PolicyCenter ↔ ClaimCenter integration |
| TC14 | Claim financials | Reserve above authority → supervisor approval | authority limits, workflow |
| TC15 | Claim financials | Pay net of deductible, over-reserve rejected, close | reserve, payment, closure |
| TC16 | Billing | Partial, over-payment, paid in full | BillingCenter |
| TC17 | End-to-end | Quote → UW → bind → bill → search → FNOL → reserve → pay → close | 5 roles, 3 applications |

## Demo users (password `gw`)
`agent1` Producer · `uw1` Underwriter · `adj1` Claims Adjuster (authority $5,000) · `sup1` Claims Supervisor
(authority $100,000) · `bill1` Billing Clerk

## Using this against a real Guidewire environment
1. `-Dbase.url=<PolicyCenter/ClaimCenter URL>`; keep the mock-only reset logic off (it runs only when `base.url=mock`).
2. Update the locators inside the page classes only – steps and features do not change (that is the point of POM).
3. Real PCF ids are long and position based; prefer stable ids/`data-*` attributes, otherwise relative XPath.
4. Create test data through APIs/DB where possible instead of the UI (see *Interview guide → test data*).

## Roadmap
* [x] Push to GitHub
* [x] CI/CD: GitHub Actions workflow in `.github/workflows/ci.yml` (smoke on push/PR, nightly regression, manual run with tags)
