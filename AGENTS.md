# Kestra Terraform Plugin

## What

- Provides plugin components under `io.kestra.plugin.terraform.cli`.
- Includes classes such as `TerraformCLI`.

## Why

- This plugin integrates Kestra with Terraform CLI.
- It provides tasks that execute Terraform CLI operations.

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
