# Azure App Service Deployment

## Prerequisites

- An Azure App Service web app configured for a supported Java 21 Java SE runtime.
- An Azure Vision resource that supports Image Analysis Read.
- Azure CLI authenticated to the target subscription.

## Build

```bash
mvn clean package
```

The deployable artifact is `target/label-verifier-0.0.1-SNAPSHOT.jar`.

## Configure the web app

Set these App Service application settings in the portal or CLI:

```text
OCR_MODE=azure
AZURE_VISION_ENDPOINT=https://<resource-name>.cognitiveservices.azure.com
AZURE_VISION_KEY=<resource-key>
```

Do not add the key to source control. For this time-boxed prototype the key is an App Service setting; managed identity and Key Vault are post-MVP hardening work.

## Deploy

The human operator can deploy the built JAR to an existing web app:

```bash
az webapp deploy \
  --resource-group <resource-group> \
  --name <web-app-name> \
  --src-path target/label-verifier-0.0.1-SNAPSHOT.jar \
  --type jar
```

App Service's Java SE runtime runs the executable Spring Boot JAR. After deployment, verify `/actuator/health`, then submit a known label through `/`.

## Smoke test

1. Confirm `/actuator/health` reports `UP`.
2. Submit a clean label with matching brand and ABV.
3. Confirm Azure-extracted text appears under the result details.
4. Change the expected ABV and confirm a Review result.
5. Submit a non-image and confirm a friendly validation message.
6. Record three warm-request response times in the README before the final demonstration.
