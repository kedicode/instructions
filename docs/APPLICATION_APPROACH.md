# Application Approach and Architecture Rationale

## Document purpose

This document explains the approach taken for the Alcohol Label Verifier, the reasons behind the main product and technology choices, how the current application works, and how it can evolve into a broader end-to-end label-review workflow.

## Document outline

1. [Goals and guiding principles](#goals-and-guiding-principles)
2. [Chosen solution](#chosen-solution)
3. [User workflow](#user-workflow)
4. [Architecture](#architecture)
5. [Technology decisions and rationale](#technology-decisions-and-rationale)
6. [Verification behavior](#verification-behavior)
7. [Example label data](#example-label-data)
8. [Current boundaries and assumptions](#current-boundaries-and-assumptions)
9. [Future enhancements](#future-enhancements)
10. [Summary](#summary)

## Goals and guiding principles

The application was intentionally designed as a small, understandable decision-support tool. Its primary goals are to:

- reduce the time reviewers spend visually comparing routine label fields;
- provide a simple workflow that requires little or no training;
- use computer vision to extract text while keeping regulatory checks deterministic and explainable;
- keep a human reviewer responsible for the final decision;
- demonstrate a useful Azure-hosted workflow without introducing unnecessary infrastructure; and
- create a foundation that can be extended later without committing the prototype to a complex production architecture too early.

These goals led to a deliberately narrow first release: one user, one label image, one verification request, and a clear field-by-field result.

## Chosen solution

We chose a simple, server-rendered web application hosted in Azure. A reviewer provides the expected label information, uploads a JPG or PNG image, and receives a result that identifies whether the image contains the expected brand name, alcohol by volume (ABV), and government warning.

The application uses:

- **Java 21** as the runtime;
- **Spring Boot and Spring MVC** for the web application and request lifecycle;
- **Thymeleaf** for server-rendered pages;
- **Azure AI Vision Image Analysis** for optical character recognition (OCR) in Azure mode;
- **Azure App Service** to host the executable application; and
- **Maven** for builds, tests, and packaging.

The result is a modular monolith rather than a set of microservices. This keeps deployment, operation, troubleshooting, and local development straightforward while preserving a clear boundary around the external computer-vision service.

## User workflow

The current end-to-end interaction is intentionally short:

1. The reviewer opens the verification page.
2. The reviewer enters the brand name and expected ABV from the application record.
3. The reviewer selects one label image in JPG or PNG format.
4. The application validates the file before sending it for OCR.
5. In Azure mode, Azure AI Vision extracts visible text from the image. In demo mode, the application supplies built-in sample OCR text so the workflow can be evaluated without Azure credentials.
6. The verification service compares the expected values with the extracted text.
7. The application presents the result and supporting evidence for each check.
8. The reviewer uses that information to complete the human review; the application does not issue an autonomous regulatory approval or rejection.

The short workflow supports reviewers with a wide range of technical experience and avoids navigation, configuration, or multi-step setup during an individual review.

## Architecture

### Logical flow

```text
Reviewer browser
    |
    v
Spring MVC controller and upload validation
    |
    +--> LabelTextExtractor
    |       |
    |       +--> Demo OCR adapter (local/default mode)
    |       |
    |       +--> Azure AI Vision adapter (Azure mode)
    |
    v
Deterministic verification service
    |
    +--> Brand-name check
    +--> ABV check
    +--> Government-warning check
    |
    v
Server-rendered result page
```

### Deployment view

```text
Browser
    |
    | HTTPS
    v
Azure App Service
    |
    | Spring Boot executable JAR (Java 21)
    |
    +--> Azure AI Vision Image Analysis API
```

Uploaded image bytes are held only for the active request and are not persisted by the current application. Azure connection information is provided through application settings rather than stored in source control.

### Internal separation of responsibilities

- The **web layer** accepts the form, validates the upload, invokes the application services, and selects the response view.
- The **OCR boundary** is represented by `LabelTextExtractor`. It allows the Azure implementation to be replaced by demo or test implementations without changing the controller or verification rules.
- The **verification layer** applies application-owned, deterministic rules to OCR text.
- The **view layer** presents findings in a form intended to support a reviewer rather than hide uncertainty behind a single automated decision.

This structure gives the application useful separation without adding the operational cost of distributed services.

## Technology decisions and rationale

### Simple application instead of microservices

A single deployable application is sufficient for the current synchronous, single-label workflow. It reduces the number of moving parts, shortens the path from source code to a working deployment, and makes failures easier to diagnose. Separate services, queues, and data stores would add cost and complexity before the workflow requires independent scaling or asynchronous processing.

### Azure hosting

Azure was selected because it matches the target hosting environment and provides both managed application hosting and a managed computer-vision capability. Azure App Service can run the Spring Boot executable JAR without requiring the team to manage virtual machines or a container orchestration platform. Keeping the web application and OCR integration in the same cloud ecosystem also simplifies configuration and operational ownership for the prototype.

### Azure computer vision for OCR

The application needs text from an image before it can compare label fields. Using Azure AI Vision avoids building or operating a custom OCR model and supports the near-real-time request/response experience required by the user workflow.

Computer vision is used for evidence extraction, not for the final compliance decision. The application applies explicit Java rules after OCR, making the behavior easier to understand, test, and explain to a reviewer. The `LabelTextExtractor` interface isolates the provider-specific integration so it can be updated or replaced if accuracy, cost, latency, or service requirements change.

### Java 21

Java 21 provides a current long-term-support runtime, strong typing, mature tooling, and a stable foundation for a business application expected to evolve over time. It also supports concise immutable data structures and has broad support in developer tooling and managed hosting environments.

### Spring Boot, Spring MVC, and Thymeleaf

Spring Boot provides established patterns for configuration, validation, testing, health checks, and production packaging. Spring MVC handles the upload and request workflow, while Thymeleaf keeps the interface server-rendered.

A separate single-page application was not needed for the current form-and-result experience. Avoiding a second frontend project reduces build complexity, dependencies, client-side state, and deployment coordination while still allowing the application to provide an accessible and responsive interface.

### Deterministic verification rules

The comparison rules are implemented in application code rather than delegated to a generative model. This approach was chosen because brand, ABV, and required-warning checks are constrained comparisons that benefit from repeatability and explainability. Reviewers can see what was expected, what was detected, and why a field needs review.

### No persistence in the initial version

The current application does not need a database to prove the core workflow. Avoiding persistence reduces privacy and retention concerns for uploaded images, removes database administration from the prototype, and keeps each request independent. Persistence should be introduced when workflow requirements define what records must be retained, for how long, and under which access and audit controls.

## Verification behavior

The application currently checks three pieces of information:

| Check | Current behavior | Reason for approach |
| --- | --- | --- |
| Brand name | Compares normalized text while tolerating capitalization, repeated whitespace, and straight versus typographic apostrophes. | Reduces false review outcomes caused by OCR formatting differences without silently accepting unrelated text. |
| ABV | Extracts percentage values and compares them numerically with the expected value. | Treats equivalent numeric representations consistently and highlights missing, different, or ambiguous values. |
| Government warning | Checks for the uppercase heading and canonical warning wording. | Supports a direct, explainable check of mandatory text while leaving typography and placement to human review. |

The overall statuses are **Pass**, **Review**, and **Unable to process**. These terms reinforce that the application supplies decision support and that a person remains accountable for the regulatory determination.

## Example label data

The repository includes example label information that can be used to understand and exercise the application:

- The form is pre-populated with the sample brand **OLD TOM DISTILLERY** and an expected ABV of **45%**.
- Demo mode supplies representative OCR text for an Old Tom Distillery bourbon label, including class/type, `45% ALC./VOL. (90 PROOF)`, net contents, and the government warning.
- The README describes the same sample label fields, and automated tests include matching, mismatching, missing, and ambiguous OCR examples.

To try the application without Azure credentials:

1. Start it with `mvn spring-boot:run`.
2. Leave the sample brand and ABV values in place.
3. Select any valid JPG or PNG file up to 5 MB.
4. Submit the form and review the field-level results produced from the built-in demo OCR text.

Demo mode validates the uploaded file but does not inspect its visual contents. To evaluate an actual example label image, configure Azure mode and upload that image so Azure AI Vision performs OCR.

## Current boundaries and assumptions

The initial implementation deliberately accepts the following boundaries:

- one image is processed per request;
- only JPG and PNG uploads up to 5 MB are supported;
- images and results are not stored;
- there is no user authentication, reviewer queue, audit history, or upstream system integration;
- the application does not verify font weight, font size, warning placement, or other visual-layout requirements;
- severely angled, obscured, curved, low-resolution, or glare-heavy images may still require manual review;
- OCR output can contain errors, so extracted text is evidence rather than an authoritative interpretation; and
- beverage-specific and import-specific exceptions remain outside the initial rule set.

These constraints keep the prototype focused on validating the core proposition: computer vision plus clear comparison rules can assist a reviewer with common label checks.

## Future enhancements

### 1. Automatic import from an upstream database

A future version could replace manual entry and upload with an integration that retrieves pending application data and associated image references from an upstream system. That enhancement should define:

- how records are selected or assigned;
- whether images are read directly, copied to controlled object storage, or accessed through signed references;
- how duplicate imports and retries are handled;
- which system is authoritative for expected label values;
- how credentials, network boundaries, and access permissions are managed; and
- how source record identifiers are preserved for traceability.

The current `LabelApplication` model and OCR boundary provide a starting point, but an import adapter, persistence model, and idempotent processing controls would be needed.

### 2. Full end-to-end review workflow

The application could grow into a complete reviewer workflow that includes:

1. importing or receiving an application;
2. placing it in a review queue;
3. retrieving the label image and expected metadata;
4. running OCR and verification automatically;
5. routing low-confidence or mismatched fields to a reviewer;
6. allowing the reviewer to confirm, annotate, or override findings;
7. recording the final outcome and audit history; and
8. sending status and evidence back to the upstream system.

This stage would likely introduce authentication and authorization, persistent records, audit logging, retention policies, reviewer assignment, status transitions, notifications, and operational reporting.

### 3. Asynchronous and batch processing

Once images arrive automatically or in larger groups, OCR should be moved from the interactive request path to a bounded asynchronous workflow. A queue and background workers could provide retry handling, rate control, failure isolation, and independent scaling. The existing synchronous path can remain useful for manual ad hoc verification.

### 4. Richer image and compliance analysis

Additional capabilities could include:

- OCR confidence and bounding-box evidence;
- image-quality checks for blur, glare, orientation, and resolution;
- class/type, net-contents, producer, and country-of-origin rules;
- beverage-specific rules and exceptions;
- visual checks for warning placement, capitalization, prominence, and typography; and
- side-by-side image highlighting to show reviewers where evidence was found.

These features should preserve the principle that uncertain outcomes are routed to review rather than automatically passed.

### 5. Production hardening

Before becoming a production system, the application should also add:

- managed identity and a secret store such as Azure Key Vault;
- infrastructure as code and repeatable environment provisioning;
- CI/CD with automated security and dependency checks;
- centralized logging, metrics, tracing, alerting, and service-level objectives;
- privacy, records-retention, and audit controls;
- threat modeling and penetration testing;
- resiliency policies for timeouts, retries, and downstream outages; and
- load, accessibility, and representative OCR-accuracy testing.

## Summary

The selected approach favors a small Azure-hosted Java 21 and Spring application because it is enough to validate the highest-value user workflow without premature infrastructure. Azure AI Vision extracts text, deterministic Java rules compare the required fields, and a simple server-rendered interface keeps a reviewer in control. The modular-monolith structure is inexpensive to operate now and establishes clear integration boundaries for future automatic image import, asynchronous processing, and a complete end-to-end review workflow.
