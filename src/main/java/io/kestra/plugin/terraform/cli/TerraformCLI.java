package io.kestra.plugin.terraform.cli;

import io.kestra.core.models.annotations.Example;
import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.*;
import io.kestra.core.models.tasks.runners.ScriptService;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.RunnerType;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static io.kestra.core.utils.Rethrow.throwFunction;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Orchestrate Infrastructure as Code by executing Terraform CLI commands in a Docker container. This task assumes that you use a remote backend for storing Terraform state files, such as AWS S3, GCS, or Terraform Cloud."
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
        )
    }
)
public class TerraformCLI extends Task implements RunnableTask<ScriptOutput>, NamespaceFilesInterface, InputFilesInterface, OutputFilesInterface {
    private static final String DEFAULT_IMAGE = "hashicorp/terraform";

    @Schema(
        title = "The setup commands to initialize the environment before executing the main list of commands such as `terraform init`."
    )
    @PluginProperty(dynamic = true)
    protected List<String> beforeCommands;

    @Schema(
        title = "The commands to run such as `terraform apply -auto-approve`."
    )
    @PluginProperty(dynamic = true)
    @NotNull
    @NotEmpty
    protected List<String> commands;

    @Schema(
        title = "Additional environment variables such as credentials and configuration for the Terraform provider."
    )
    @PluginProperty(
        additionalProperties = String.class,
        dynamic = true
    )
    protected Map<String, String> env;

    @Schema(
        title = "Docker options when for the `DOCKER` runner",
        defaultValue = "{image=" + DEFAULT_IMAGE + ", pullPolicy=ALWAYS}"
    )
    @PluginProperty
    @Builder.Default
    protected DockerOptions docker = DockerOptions.builder().build();

    private NamespaceFiles namespaceFiles;

    private Object inputFiles;

    private List<String> outputFiles;

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        return new CommandsWrapper(runContext)
            .withWarningOnStdErr(true)
            .withRunnerType(RunnerType.DOCKER)
            .withDockerOptions(injectDefaults(getDocker()))
            .withEnv(Optional.ofNullable(this.env != null ? runContext.renderMap(this.env) : null).orElse(new HashMap<>()))
            .withNamespaceFiles(namespaceFiles)
            .withInputFiles(inputFiles)
            .withOutputFiles(outputFiles)
            .withCommands(
                ScriptService.scriptCommands(
                    List.of("/bin/sh", "-c"),
                    Optional.ofNullable(this.beforeCommands).map(throwFunction(runContext::render)).orElse(null),
                    runContext.render(this.commands)
                )
            )
            .run();
    }

    private DockerOptions injectDefaults(DockerOptions original) {
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
