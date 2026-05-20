# How to use the Terraform plugin

The `TerraformCLI` task runs any sequence of Terraform commands inside a container, making infrastructure provisioning a first-class step in a Kestra flow.

## Authentication

Terraform credentials are passed via environment variables in the `env` property rather than task properties. For AWS, set `AWS_ACCESS_KEY_ID` and `AWS_SECRET_ACCESS_KEY`; for GCP, set `GOOGLE_APPLICATION_CREDENTIALS` or pass service account key content. Use `{{ secret('NAME') }}` to inject values from [secrets](https://kestra.io/docs/concepts/secret) at runtime.

## Common properties

`containerImage` sets the Terraform image — defaults to the official `hashicorp/terraform` image. Pin a specific version (e.g., `hashicorp/terraform:1.9`) to ensure reproducible runs. `taskRunner` defaults to Docker and can be overridden for other execution environments.

## Tasks

`TerraformCLI` is the only task. Set `commands` to an ordered list of Terraform CLI arguments (e.g., `["init", "plan", "apply -auto-approve"]`). Configuration files must be present in the working directory — use a preceding `Clone` task or [namespace files](https://kestra.io/docs/concepts/namespace-files) to make them available.

Configure remote state (S3, GCS, Azure Blob) inside your Terraform configuration rather than relying on local state — local state does not persist between executions. Backend configuration values can be injected via `env` or passed as `-backend-config` flags in `commands`.
