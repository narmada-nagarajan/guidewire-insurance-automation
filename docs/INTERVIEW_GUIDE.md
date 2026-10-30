# Interview Guide - Selenium + Java + Cucumber BDD on an Insurance (Guidewire-style) Project

## 0. Say this honestly
This is a **portfolio project I built to practise and demonstrate** an insurance BDD automation framework. The
application under test is a Guidewire-style mock (real Guidewire is licensed software). Present it that way:
*"I modelled PolicyCenter, ClaimCenter and BillingCenter flows, designed the framework and wrote the scenarios."*
Do not describe it as a client project. Then connect it to your real strength: 11+ years of insurance/banking
testing, test strategy, UAT, defect management and reviewing automation.

## 1. The 60-second pitch
"The framework is Selenium 4 with Java, Cucumber 7 for BDD and TestNG as the engine. It follows the Page Object
Model, shares scenario state through PicoContainer dependency injection and runs in parallel with a ThreadLocal
WebDriver. It covers the full insurance lifecycle in 17 scenarios: quote and rating, underwriting referral, bind,
endorsement, cancellation, renewal, FNOL, reserves and payments with authority limits, billing, and one end-to-end
scenario that crosses five roles and three Guidewire applications. Tags drive smoke and regression packs, and
reports, screenshots and a rerun-failed runner are built in. The next step is CI/CD."

## 2. Architecture (draw this on the whiteboard)
```
 feature file (Gherkin, business language)
        │  matched by regex / cucumber expression
        ▼
 step definitions  ──(constructor injection)──►  TestContext (per-scenario state)
        │
        ▼
 page objects (BasePage + one class per screen)  ── explicit waits, locators
        │
        ▼
 WebDriver (ThreadLocal, DriverFactory)  ──►  application (PolicyCenter / ClaimCenter / BillingCenter)

 Hooks: @Before open browser · @After screenshot on failure + quit
 Runner: TestRunner (TestNG DataProvider, parallel) · FailedScenariosRunner (rerun.txt)
 Reports: HTML / JSON / JUnit XML / timeline
```
Layers keep each concern in one place: **features** = *what*, **steps** = *glue*, **pages** = *how on screen*.

## 3. Insurance domain concepts (and where they appear)
| Concept | Plain-English meaning | Scenario |
|---|---|---|
| Policy | The insurance contract | all |
| Premium | Price of the policy | TC03 |
| Producer / Agent | Sells the policy | TC01, TC03 |
| Submission | A request for insurance, not yet a policy | TC03-TC05 |
| Quote | The priced offer | TC03 |
| Rating | Premium calculation from risk factors (age, state, vehicle, coverage, violations) | TC03 |
| Underwriting | Deciding whether to accept a risk | TC05 |
| Referral / UW issue | Rule blocks binding until an underwriter approves | TC05 |
| Bind / Issue | Accept the quote and create the in-force policy | TC06 |
| Policy term / period | Effective to expiration (6 or 12 months) | TC06 |
| Coverage / Limit / Deductible | What is covered, the maximum payout, what the insured pays first | TC03, TC11, TC15 |
| Endorsement / Policy change | Mid-term change, e.g. add a vehicle | TC07 |
| Pro-rata | Premium proportional to remaining days | TC07, TC08 |
| Cancellation (pro rata / short rate / flat) | Ending the policy; refund method differs | TC08, TC09 |
| Renewal | New term after expiry, usually with a rate change | TC10 |
| FNOL | First Notice of Loss - the claim is reported | TC11-TC13 |
| Coverage verification | Was the policy in force on the loss date and does it cover this loss? | TC12, TC13 |
| Claim / Exposure | The loss event / each insured item or person affected | TC11 |
| Reserve | Money set aside for the expected claim cost | TC14, TC15 |
| Authority limit | How much a user may reserve/pay without approval | TC14 |
| Payment (net of deductible) | Money paid to claimant | TC15 |
| Subrogation / Salvage | Recovering cost from at-fault party / sale of damaged property (explain, not automated) | - |
| Billing / Invoice / Payment plan | Collecting premium: full pay or instalments | TC06, TC16 |
| Delinquency | Overdue payment, can lead to cancellation for non-payment (explain, not automated) | - |
| Activity | Task assigned to a user/role, e.g. UW referral | TC05, TC14 |

**Lifecycle in one breath:** Quote → Underwriting → Bind/Issue → Bill → Change/Renew/Cancel → Claim (FNOL → reserve → pay → close).

## 4. Guidewire concepts
* **PolicyCenter (PC)** policy administration - submissions, quotes, issuance, changes, renewals, cancellations.
* **ClaimCenter (CC)** claims - FNOL, assignment, exposures, reserves, payments, recovery.
* **BillingCenter (BC)** - accounts, invoices, payment plans, delinquency.
* **ContactManager** - shared contact/address data; **Digital** portals; **Cloud / Guidewire Cloud Platform**.
* **Gosu** - Guidewire's own Java-like language used for rules, business logic and configuration.
* **PCF (Page Configuration Files)** - XML that defines screens; generates ids like `NewSubmission:NewSubmissionScreen:...`
  (reason the mock uses that id style).
* **Product model** - lines of business, coverages, modifiers defined in configuration.
* **Rule sets** - validation, underwriting and assignment rules (e.g. our UW referral and claim authority).
* **Activities** - work items on the Desktop.
* **Integration** - messaging/plugins/web services between PC, CC, BC and external systems (rating, payments, ISO).
* **Test automation challenges in Guidewire:** very dynamic generated ids, heavy AJAX re-rendering (stale elements),
  role-based screens, long wizards, test data setup cost, environment refresh and data masking, frequent upgrades.
  Answers: stable locators, explicit waits, page objects, API/DB data setup, tags for packs, small focused flows.

## 5. BDD and Cucumber concepts (all used in this project)
| Concept | One-line explanation | Where |
|---|---|---|
| BDD | Collaboration (Three Amigos: BA, Dev, QA) using examples in business language | all features |
| Gherkin | Given-When-Then plain-text syntax | `features/` |
| Feature / Scenario | Business capability / one example of behaviour | all |
| Given / When / Then / And / But | Context / action / outcome | all |
| Background | Steps repeated before every scenario of the feature | 02-10 |
| Scenario Outline + Examples | One template, many data rows | TC02, TC03, TC05, TC06, TC08 |
| DataTable | Table argument to a step (`asMap`, `asMaps`, `asList`) | TC04 |
| DocString | Multi-line text block `"""` | TC11, TC12, TC17 |
| Tags | Labels for filtering and traceability (`@smoke`, `@TC17`) | all |
| Tag expressions | `@smoke and not @e2e` | `-Dcucumber.filter.tags` |
| Rule | Groups scenarios for one business rule (Gherkin 6) | 03 |
| Step definitions | Java methods bound to steps | `stepdefinitions/` |
| Cucumber expressions | `{int}`, `{string}`, `a(n)` optional text | steps |
| Custom parameter types | `{money}` → BigDecimal, `{role}` → enum | `ParameterTypes` |
| Hooks | `@Before`, `@After`, `@BeforeStep`, `@AfterStep`, tagged hooks, `order` | `Hooks` |
| Dependency injection | PicoContainer shares `TestContext` per scenario | `TestContext` |
| Runner | `@CucumberOptions` (features, glue, plugin, tags, dryRun, monochrome) | `runners/` |
| Plugins / reports | pretty, html, json, junit, rerun, timeline | `TestRunner` |
| Dry run | Checks every step has a definition without executing | `dryRun = true` |
| Parallel execution | TestNG DataProvider `parallel=true` + ThreadLocal driver | `TestRunner` |
| Rerun failed | `rerun:` plugin writes `target/rerun.txt` | `FailedScenariosRunner` |

**Good vs bad scenarios:** declarative ("When I bind the quote") not imperative ("click button X"); one behaviour per
scenario; no UI detail in the feature; independent scenarios; business language the BA can read.

## 6. Framework design talking points
* **POM:** locators and actions in page classes; steps never call `driver.findElement` directly.
* **Waits:** only explicit waits (`WebDriverWait`, `ExpectedConditions`); never `Thread.sleep`; never mix implicit
  and explicit. `BasePage.waitForLoading()` waits for the AJAX overlay (typical of Guidewire).
* **ThreadLocal driver:** one browser per scenario thread; `quitDriver()` also removes the ThreadLocal.
* **No static shared state:** `TestContext` is scenario scoped; static fields would break parallel runs.
* **Config:** `config.properties` plus `-D` overrides, so the same code runs locally, in Docker and in CI.
* **Test oracle:** `PremiumCalculator` re-implements pro-rata/short-rate independently so app defects are not echoed.
* **Test data:** JSON profiles (`customers.json`), a fresh object per call, DataTables and Examples for variations.
* **Alerts:** cancellation uses a native `confirm()` → `ExpectedConditions.alertIsPresent()` + `switchTo().alert()`.
* **Failure evidence:** screenshot attached to the Cucumber report and saved to `target/screenshots`.
* **Locators:** `By.id` for colon-style ids (CSS needs `\\:` escaping); XPath for tables and text.
* **Test setup via UI is slow:** "Given an in-force policy exists" uses the UI here; in a real project call APIs/DB.

## 7. Test Lead topics (you are strong here)
* **Strategy:** test pyramid; UI automation only for critical end-to-end flows, APIs/units for the rest.
* **Risk-based selection:** smoke = login, bind, cancellation, payment, E2E; regression = everything.
* **Traceability:** requirement ID → scenario tag (`@TC03`) → defect ID; RTM kept in ALM/qTest/Jira.
* **Entry/exit criteria, RAG status, defect leakage, DRE, automation coverage and flakiness rate** as metrics.
* **Flaky tests:** root causes (waits, shared data, environment) and the fix before adding retries.
* **Estimation and team:** who writes features (BA/QA), who writes steps (SDET), review via pull requests.
* **UAT and release sign-off, regression after Guidewire upgrades, data masking, defect triage.**

## 8. Likely questions with short answers
1. **Why Cucumber/BDD?** Shared understanding between business and QA; scenarios are living documentation and tests.
2. **Difference between Scenario and Scenario Outline?** Outline repeats the same steps for each Examples row.
3. **What is Background?** Steps that run before each scenario in the feature, e.g. open app and log in.
4. **Hooks vs Background?** Hooks are code-level setup/teardown (browser); Background is business-level context.
5. **What does `order` do in hooks?** Defines sequence; for `@After`, higher order runs first (screenshot before quit).
6. **How do step classes share data?** PicoContainer constructor injection of `TestContext`; no statics.
7. **How do you run parallel?** DataProvider `parallel=true`, `-Dthreads`, ThreadLocal driver, no shared state.
8. **How do you run only smoke?** `-Dcucumber.filter.tags="@smoke"`.
9. **How do you rerun failures?** `rerun` plugin → `target/rerun.txt` → `FailedScenariosRunner`.
10. **What is a custom parameter type?** Converts step text into an object, e.g. `$1,234.50` → `BigDecimal`.
11. **DataTable vs Examples?** DataTable = data for one step; Examples = data for the whole scenario.
12. **What is a DocString?** Multi-line text argument, used for the loss description.
13. **How do you handle stale elements/AJAX?** Re-find elements each time, wait for overlay + visibility, no cached elements.
14. **How do you handle a JavaScript alert?** `WebDriverWait` + `alertIsPresent()`, then accept/dismiss.
15. **Implicit vs explicit vs fluent wait?** Implicit global and unpredictable; explicit per condition; fluent adds polling and ignored exceptions.
16. **How do you keep tests independent?** Each scenario creates its own policy/claim; browser and data reset in `@Before`.
17. **Where do you keep test data?** JSON profiles, builders with defaults, DataTables, API/DB for heavy setup; never hard-code in pages.
18. **How is premium validated?** Examples hold expected values derived from rating rules; time-based amounts use `PremiumCalculator`.
19. **Pro-rata vs short-rate vs flat?** Proportional refund / proportional refund minus penalty / full refund from inception.
20. **Why does a claim fail on a flat-cancelled policy?** Coverage was never in force on the loss date (TC13).
21. **What is an exposure?** Each insured item or party in a claim, e.g. vehicle damage vs third-party property.
22. **Reserve vs payment?** Reserve is the expected cost set aside; payment is money actually issued.
23. **Why approval above $5,000?** Authority limits are a financial control (TC14).
24. **What would you automate vs keep manual?** Stable, repeatable, high-risk flows automated; exploratory, usability, one-off UAT manual.
25. **How would you integrate with CI/CD?** Maven on a pipeline, smoke on pull request, full regression nightly, HTML report as artifact (next step).
26. **How do you handle Guidewire dynamic ids?** Prefer stable attributes, relative XPath, avoid index-based locators, centralise in page objects.
27. **What happens when a test fails?** Screenshot + log attached, rerun runner, triage for product bug vs script/data/env issue.
28. **What coverage do you report?** Requirement coverage via tags, pass rate, defect leakage, flakiness, execution time.

## 9. Command cheat sheet
```bash
mvn clean test
mvn test -Dcucumber.filter.tags="@smoke"
mvn test -Dcucumber.filter.tags="@TC17" -Dheadless=false -Dthreads=1
mvn test -Dcucumber.filter.tags="@policycenter and not @negative"
mvn test -Dtest=FailedScenariosRunner
```
