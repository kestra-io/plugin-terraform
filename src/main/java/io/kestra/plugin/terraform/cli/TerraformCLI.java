package io.kestra.plugin.terraform.cli;

import java.util.List;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.AbstractExecScript;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.runner.docker.Docker;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
// callSuper = false: the inherited env has no @ToString.Exclude, keeps rendered secrets out
@ToString(callSuper = false)
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Run Terraform CLI commands in Docker",
    description = """
        Executes Terraform commands inside the task runner container. Defaults to the `hashicorp/terraform` image and assumes a remote state backend such as S3, GCS, or Terraform Cloud.
        The inherited `warningOnStdErr` and `runner` properties are ignored. The deprecated `docker` option only applies on Kestra 1.3, use `taskRunner`.
        """
)
@Plugin(
    examples = {
        @Example(
            title = "Initialize Terraform, then create and apply the Terraform plan",
            full = true,
            code = """
                id: git_terraform
                namespace: company.team

                tasks:
                  - id: git
                    type: io.kestra.plugin.core.flow.WorkingDirectory
                    tasks:
                      - id: clone_repository
                        type: io.kestra.plugin.git.Clone
                        url: https://github.com/anna-geller/kestra-ci-cd
                        branch: main

                      - id: terraform
                        type: io.kestra.plugin.terraform.cli.TerraformCLI
                        beforeCommands:
                          - terraform init
                        inputFiles:
                          terraform.tfvars: |
                            username            = "cicd"
                            password            = "{{ secret('CI_CD_PASSWORD') }}"
                            hostname            = "https://demo.kestra.io"
                        commands:
                          - terraform plan
                          - terraform apply -auto-approve
                        env:
                          AWS_ACCESS_KEY_ID: "{{ secret('AWS_ACCESS_KEY_ID') }}"
                          AWS_SECRET_ACCESS_KEY: "{{ secret('AWS_SECRET_ACCESS_KEY') }}"
                          AWS_DEFAULT_REGION: "{{ secret('AWS_DEFAULT_REGION') }}"
                """
        ),
        @Example(
            title = "Pin Terraform version and run validate then plan",
            full = true,
            code = """
                id: terraform_plan_only
                namespace: company.team

                tasks:
                  - id: terraform
                    type: io.kestra.plugin.terraform.cli.TerraformCLI
                    containerImage: hashicorp/terraform:1.6.6
                    beforeCommands:
                      - terraform init -input=false
                    commands:
                      - terraform validate -no-color
                      - terraform plan -input=false -no-color -out=tfplan
                    env:
                      TF_VAR_region: us-east-1
                      TF_TOKEN_app_terraform_io: "{{ secret('TF_API_TOKEN') }}"
                    outputFiles:
                      - tfplan
                """
        )
    }
)
public class TerraformCLI extends AbstractExecScript implements RunnableTask<ScriptOutput> {
    private static final String DEFAULT_IMAGE = "hashicorp/terraform";

    @Schema(
        title = "Primary Terraform CLI commands",
        description = "Main commands run through the `interpreter` (`/bin/sh -c` by default), e.g., `terraform plan` or `terraform apply -auto-approve`."
    )
    @NotNull
    @PluginProperty(group = "main")
    protected Property<List<String>> commands;

    // Kept here: core has no docker property from 2.0, flows using it would fail validation
    @Schema(
        title = "Deprecated Docker options",
        description = "Use `taskRunner` instead of this legacy Docker configuration."
    )
    @PluginProperty(group = "deprecated")
    @Deprecated
    private DockerOptions docker;

    @Schema(
        title = "The task runner to use",
        description = "Task runners are provided by plugins, each have their own properties."
    )
    @PluginProperty(group = "execution")
    @Builder.Default
    @Valid
    private TaskRunner<?> taskRunner = Docker.instance();

    @Schema(
        title = "Task runner container image",
        description = "Used only when the task runner is container-based; defaults to `hashicorp/terraform`."
    )
    @Builder.Default
    @PluginProperty(group = "execution")
    private Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        return this.commands(runContext)
            .withInterpreter(this.getInterpreter())
            .withBeforeCommands(this.getBeforeCommands())
            .withBeforeCommandsWithOptions(true)
            .withCommands(this.commands)
            .run();
    }

    @Override
    protected DockerOptions injectDefaults(RunContext runContext, DockerOptions original) {
        if (original == null) {
            return null;
        }

        var builder = original.toBuilder();
        if (original.getImage() == null) {
            builder.image(DEFAULT_IMAGE);
        }
        if (original.getEntryPoint() == null || original.getEntryPoint().isEmpty()) {
            builder.entryPoint(List.of(""));
        }

        return builder.build();
    }
}
