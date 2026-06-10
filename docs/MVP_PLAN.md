# Seven-Hour MVP Delivery Plan

## Objective

Deliver one complete, deployed, and tested workflow by the end of the day:

1. A reviewer enters the application brand name and alcohol by volume (ABV).
2. The reviewer uploads one alcohol-label image.
3. Azure OCR extracts the visible text.
4. The Java application checks the brand name, ABV, and mandatory government warning.
5. The reviewer receives a simple field-by-field result in approximately five seconds under normal warm conditions.

This is a decision-support prototype, not an automated regulatory decision system. Its result statuses are **Pass**, **Review**, and **Unable to process**; it does not issue a regulatory rejection.

## MVP scope

### Must ship

- Java 21, Maven, Spring Boot, Spring MVC, and Thymeleaf.
- One server-rendered page; no separate JavaScript frontend.
- A form with:
  - expected brand name;
  - expected ABV;
  - one JPG or PNG label image.
- Azure Image Analysis OCR behind one `LabelTextExtractor` interface.
- Deterministic Java checks for:
  - brand name, ignoring case, repeated whitespace, and typographic apostrophe differences;
  - ABV, parsed and compared numerically;
  - government warning heading and canonical warning body text.
- An obvious result page showing the expected value, detected evidence, status, and explanation for each check.
- Safe handling of empty, unsupported, corrupt, and oversized uploads.
- Friendly handling of missing Azure configuration, OCR errors, and OCR timeouts.
- Unit tests for comparison rules and MVC tests using a fake OCR implementation.
- A README containing local setup, Azure configuration, test, build, and deployment instructions.
- A publicly reachable Azure deployment.

### Only if the core is complete early

In priority order:

1. Class/type comparison.
2. Net-contents comparison.
3. One low-quality-image warning based on weak or missing OCR output.
4. Additional sample labels and result-page polish.

### Explicitly out of scope today

- Batch upload.
- A database, object storage, queues, or background workers.
- COLA integration, accounts, authentication, or reviewer history.
- Beer-, wine-, and import-specific rules.
- Custom-trained models, Azure OpenAI, or an LLM.
- Fuzzy matching that can automatically pass a field.
- Reliable font size, placement, or bold-style verification.
- Infrastructure as code, CI/CD, custom domains, and production compliance controls.
- Pixel-perfect handling of glare, curved bottles, or severely angled photographs.

These are documented follow-up opportunities, not partially implemented features.

## Why this is the smallest useful design

The README identifies brand, ABV, and the government warning as the repeated manual checks. A single-label path demonstrates the important product hypothesis: OCR can turn label artwork into evidence, and straightforward rules can reduce routine visual comparison.

The application is a modular monolith:

```text
Browser
  -> Spring MVC controller
  -> Verification service
       -> LabelTextExtractor interface
            -> Azure Image Analysis adapter
       -> Brand, ABV, and warning rules
  -> Thymeleaf result page
```

There is one intentional abstraction: `LabelTextExtractor`. The Azure API is the unstable external boundary and must be replaceable by a fake in tests. Controllers should not call the Azure SDK directly. The remaining behavior can be implemented with small immutable records and stateless services; no repository, factory hierarchy, strategy registry, or rules framework is warranted.

Azure Image Analysis is preferred for this MVP because Microsoft describes its Read capability as suitable for text in external images such as labels and as a synchronous API intended for real-time experiences. Azure App Service with its Java SE runtime can host the executable Spring Boot JAR. The implementation should use currently supported stable SDK versions selected when the project is generated.

## Minimal package structure

```text
src/main/java/.../
  LabelVerifierApplication.java
  config/
    AzureVisionProperties.java
  ocr/
    LabelTextExtractor.java
    ExtractedLabelText.java
    AzureLabelTextExtractor.java
  verification/
    LabelApplication.java
    VerificationService.java
    VerificationResult.java
    FieldResult.java
    VerificationStatus.java
    TextNormalizer.java
  web/
    VerificationController.java
    VerificationForm.java

src/main/resources/
  application.yml
  templates/index.html
  templates/result.html
  static/app.css

src/test/java/.../
  verification/
  web/
```

Do not add a persistence layer. Uploaded bytes should exist only for the request and should not be logged or stored.

## Rule behavior

### Brand name

- Apply Unicode normalization.
- Trim and collapse whitespace.
- Normalize straight and typographic apostrophes.
- Compare without case sensitivity.
- Pass `STONE'S THROW` against `Stone’s Throw`.
- Return Review—not Pass—when only a partial or fuzzy match exists.

### ABV

- Accept the expected ABV as a decimal percentage, such as `45` or `45.0`.
- Search OCR output for common forms such as `45%`, `45 % Alc./Vol.`, or `ALC 45% BY VOL`.
- Compare parsed numbers rather than display strings.
- Return Review when no unambiguous ABV is found or when it differs from the application.

### Government warning

- Require an uppercase `GOVERNMENT WARNING:` heading.
- Compare the warning body against the canonical statutory text after normalizing line breaks and repeated whitespace.
- Preserve word order and wording; missing or substituted words produce Review.
- Report bold styling as **not verified by this MVP**. Do not claim that visual styling passed.
- Return Review when OCR confidence or text quality prevents a reliable decision.

### Overall result

- **Pass:** all three checks pass.
- **Review:** at least one check needs human attention and OCR completed successfully.
- **Unable to process:** upload validation or OCR failed, timed out, or produced no useful text.

## Seven-hour execution schedule

The clock starts after this plan is accepted. Local development proceeds with demo OCR while the human operator prepares Azure resources in parallel. Each checkpoint leaves the project in a runnable state.

| Elapsed time | Work | Exit criterion |
|---|---|---|
| 0:00-0:30 | Confirm scope and generate the Spring Boot project while Azure resources are prepared in parallel. | The application starts locally in demo mode; Azure credentials may follow before the live adapter smoke test. |
| 0:30-1:20 | Build the form, upload validation, controller, and a fake OCR implementation. | A valid submission reaches a result page without Azure. |
| 1:20-2:10 | Implement the Azure OCR adapter and error/timeout mapping. | A real sample image returns extracted text locally. |
| 2:10-3:20 | Implement brand, ABV, warning, and aggregate-result rules. | Core rule unit tests pass. |
| 3:20-4:10 | Complete the accessible result UI and friendly error states. | The full local workflow is understandable without inspecting logs. |
| 4:10-5:10 | Add MVC tests, upload edge-case tests, and representative OCR fixtures. | Default Maven verification passes without Azure credentials. |
| 5:10-6:00 | Package and deploy to Azure App Service with the human-in-the-loop resource handoff. | Public URL completes one successful verification. |
| 6:00-7:00 | Buffer: fix deployment issues, measure warm latency, improve README, and rehearse the demo. | Repository and deployment satisfy the acceptance checklist. |

### Scope-cut rule

At 4 hours elapsed, stop adding behavior. If all three checks are not complete, simplify the presentation and finish the rules. At 5 hours elapsed, stop UI polishing and begin deployment. At 6 hours elapsed, make only fixes required for the acceptance checklist.

## Human-in-the-loop Azure handoff

The human operator is responsible for creating or approving billable Azure resources and supplying configuration. The implementation should never commit secrets.

### Needed by the 30-minute checkpoint

Create or identify:

1. An Azure resource group.
2. An Azure Vision resource that supports Image Analysis OCR.
3. Its endpoint and one access key for the take-home deployment.
4. An Azure App Service application using a Java SE runtime suitable for the selected Java version.

Provide these runtime settings to the application:

```text
AZURE_VISION_ENDPOINT=<resource endpoint>
AZURE_VISION_KEY=<resource key>
```

For a seven-hour prototype, App Service application settings are sufficient. Managed identity and Key Vault are worthwhile production improvements but should not block delivery. Restrict the key to the deployed app settings, do not place it in source control, and rotate or remove it after the evaluation.

### Deployment handoff

The developer supplies the tested executable JAR and exact startup/configuration instructions. The human operator performs Azure authentication and any portal or CLI actions that require subscription access. Both verify the public URL with the same known sample before considering deployment complete.

## Test plan

### Required automated tests

- Brand normalization:
  - capitalization differences;
  - repeated whitespace;
  - straight versus typographic apostrophes;
  - true mismatch.
- ABV parsing:
  - supported display formats;
  - decimal values;
  - mismatch;
  - missing and ambiguous values.
- Warning verification:
  - exact warning;
  - title-case heading;
  - missing word;
  - missing warning.
- Overall status aggregation.
- MVC flow with a fake `LabelTextExtractor`:
  - successful Pass result;
  - Review result;
  - empty upload;
  - unsupported type;
  - oversized upload;
  - OCR timeout/error.

The default test suite must run offline and must not require Azure credentials. One manual smoke test exercises the real service.

### Manual smoke test

1. Start the application with Azure settings.
2. Submit a known label with matching brand and ABV and the canonical warning.
3. Confirm the extracted evidence and Pass result.
4. Change the expected ABV and confirm Review.
5. Upload a non-image and confirm a friendly validation message.
6. Measure three warm successful requests and record the times in the README.
7. Repeat steps 2-5 against the deployed URL.

The five-second stakeholder target is an objective to measure, not a result to fabricate. If Azure OCR exceeds it, document the observed timing and the likely next optimization.

## Definition of done

The MVP is complete only when all of the following are true:

- `mvn test` passes without Azure credentials.
- `mvn package` creates a deployable executable JAR.
- The form accepts expected brand, expected ABV, and one supported image.
- A real Azure OCR request is successfully mapped into application-owned text data.
- Matching sample data can produce Pass.
- Incorrect brand, ABV, or warning wording produces an explainable Review result.
- Bad uploads and Azure failures produce friendly errors without a stack trace or secret exposure.
- Uploaded images are not persisted.
- The README explains setup, environment variables, tests, known limitations, and deployment.
- A reviewer can reach and exercise the public Azure URL.
- Observed warm response times are recorded honestly.

## Demo script

Keep the final demonstration under five minutes:

1. Explain that the prototype assists rather than replaces a compliance reviewer.
2. Enter the matching brand and ABV, upload a clean label, and show the three findings.
3. Change the expected ABV and show the explainable Review status.
4. Point out that warning wording is checked strictly while bold styling is explicitly unverified.
5. Mention that the architecture isolates Azure OCR behind one interface and that the default test suite runs offline.
6. Close with the documented trade-offs: single upload today; batch processing and richer visual compliance checks next.

## Post-MVP backlog

If more time becomes available after the deployed acceptance checklist passes:

1. Add class/type and net-contents checks.
2. Add bounded batch upload with a CSV manifest.
3. Add word-level confidence and bounding-box evidence.
4. Evaluate warning typography and placement using image/layout metadata.
5. Add managed identity, Key Vault, infrastructure as code, and CI/CD.
6. Expand beverage-specific rules and test imagery.

## Azure references

- [Azure Image Analysis 4.0 quickstart](https://learn.microsoft.com/azure/ai-services/computer-vision/quickstarts-sdk/image-analysis-client-library-40)
- [Azure OCR guidance for images and documents](https://learn.microsoft.com/azure/ai-services/document-intelligence/concept-read)
- [Java on Azure App Service](https://learn.microsoft.com/azure/app-service/getting-started#java)
