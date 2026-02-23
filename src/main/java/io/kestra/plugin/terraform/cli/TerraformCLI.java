package io.kestra.plugin.terraform.cli;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.property.Property;
import io.kestra.core.models.tasks.*;
import io.kestra.core.models.tasks.runners.ScriptService;
import io.kestra.core.models.tasks.runners.TaskRunner;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.kestra.plugin.scripts.runner.docker.Docker;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.*;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Run Terraform CLI commands in Docker",
    description = "Executes Terraform commands inside the task runner container. Defaults to the `hashicorp/terraform` image and assumes a remote state backend such as S3, GCS, or Terraform Cloud."
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
                        outputFiles:
                          - "*.txt"
                        commands:
                          - terraform plan 2>&1 | tee plan_output.txt
                          - terraform apply -auto-approve 2>&1 | tee apply_output.txt
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
public class TerraformCLI extends Task implements RunnableTask<ScriptOutput>, NamespaceFilesInterface, InputFilesInterface, OutputFilesInterface {
    private static final String DEFAULT_IMAGE = "hashicorp/terraform";

    @Schema(
        title = "Pre-run setup commands",
        description = "Commands executed before `commands`, typically `terraform init`."
    )
    protected Property<List<String>> beforeCommands;

    @Schema(
        title = "Primary Terraform CLI commands",
        description = "Main commands run with `/bin/sh -c`, e.g., `terraform plan` or `terraform apply -auto-approve`."
    )
    @NotNull
    protected Property<List<String>> commands;

    @Schema(
        title = "Environment variables for commands",
        description = "Extra variables passed to the process; use for provider credentials and configuration."
    )
    @PluginProperty(
        additionalProperties = String.class,
        dynamic = true
    )
    protected Map<String, String> env;

    @Schema(
        title = "Deprecated Docker options",
        description = "Use `taskRunner` instead of this legacy Docker configuration."
    )
    @PluginProperty
    @Deprecated
    private DockerOptions docker;

    @Schema(
        title = "The task runner to use.",
        description = "Task runners are provided by plugins, each have their own properties."
    )
    @PluginProperty
    @Builder.Default
    @Valid
    private TaskRunner<?> taskRunner = Docker.instance();

    @Schema(
        title = "Task runner container image",
        description = "Used only when the task runner is container-based; defaults to `hashicorp/terraform`."
    )
    @Builder.Default
    private Property<String> containerImage = Property.ofValue(DEFAULT_IMAGE);

    private NamespaceFiles namespaceFiles;

    private Object inputFiles;

    private Property<List<String>> outputFiles;

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        var renderedOutputFiles = runContext.render(this.outputFiles).asList(String.class);
        return new CommandsWrapper(runContext)
            .withWarningOnStdErr(true)
            .withDockerOptions(injectDefaults(getDocker()))
            .withTaskRunner(this.taskRunner)
            .withContainerImage(runContext.render(this.containerImage).as(String.class).orElse(null))
            .withEnv(Optional.ofNullable(this.env != null ? runContext.renderMap(this.env) : null).orElse(new HashMap<>()))
            .withNamespaceFiles(namespaceFiles)
            .withInputFiles(inputFiles)
            .withOutputFiles(renderedOutputFiles.isEmpty() ? null : renderedOutputFiles)
            .withInterpreter(Property.ofValue(List.of("/bin/sh", "-c")))
            .withBeforeCommands(this.beforeCommands)
            .withCommands(this.commands)
            .run();
    }

    private DockerOptions injectDefaults(DockerOptions original) {
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
