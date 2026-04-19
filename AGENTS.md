# Kestra Terraform Plugin

## What

- Provides plugin components under `io.kestra.plugin.terraform.cli`.
- Includes classes such as `TerraformCLI`.

## Why

- What user problem does this solve? Teams need to run Terraform commands from Kestra from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Terraform steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Terraform.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `terraform`

### Key Plugin Classes

- `io.kestra.plugin.terraform.cli.TerraformCLI`

### Project Structure

```
plugin-terraform/
├── src/main/java/io/kestra/plugin/terraform/cli/
├── src/test/java/io/kestra/plugin/terraform/cli/
├── build.gradle
└── README.md
```

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
