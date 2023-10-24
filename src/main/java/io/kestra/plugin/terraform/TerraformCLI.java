package io.kestra.plugin.terraform;

import io.kestra.core.models.annotations.Plugin;
import io.kestra.core.models.annotations.PluginProperty;
import io.kestra.core.models.tasks.RunnableTask;
import io.kestra.core.models.tasks.Task;
import io.kestra.core.runners.RunContext;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.RunnerType;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.kestra.plugin.scripts.exec.scripts.runners.CommandsWrapper;
import io.kestra.plugin.scripts.exec.scripts.services.ScriptService;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuperBuilder
@ToString
@EqualsAndHashCode
@Getter
@NoArgsConstructor
@Schema(
    title = "Execute terraform command"
)
@Plugin(
    examples = {
        @io.kestra.core.models.annotations.Example(
            title = "Simple revert",
            code = {
                "env: " +
                "  AWS_ACCESS_KEY_ID: \"{{ secret('AWS_ACCESS_KEY_ID') }}\"",
                "  AWS_ACCESS_KEY_ID: \"{{ secret('AWS_ACCESS_KEY_ID') }}\"",
                "  AWS_DEFAULT_REGION: \"{{ secret('AWS_DEFAULT_REGION') }}\"",
                "beforeCommands: " +
                "  - terraform init",
                "commands: " +
                "  - terraform apply -auto-approve"
            }
        )
    }
)
public class TerraformCLI extends Task implements RunnableTask<ScriptOutput> {
    private static final String DEFAULT_IMAGE = "hashicorp/terraform";

    @Schema(
        title = "The commands to run before main list of commands"
    )
    @PluginProperty(dynamic = true)
    protected List<String> beforeCommands;

    @Schema(
        title = "The commands to run"
    )
    @PluginProperty(dynamic = true)
    @NotNull
    @NotEmpty
    protected List<String> commands;

    @Schema(
        title = "Additional environment variables for the current process."
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

    @Override
    public ScriptOutput run(RunContext runContext) throws Exception {
        if (this.beforeCommands != null) {
            return new CommandsWrapper(runContext)
                .withWarningOnStdErr(true)
                .withRunnerType(RunnerType.DOCKER)
                .withDockerOptions(injectDefaults(getDocker()))
                .withEnv(Optional.ofNullable(env).orElse(new HashMap<>()))
                .withCommands(ScriptService.scriptCommands(List.of("/bin/sh", "-c"), runContext.render(this.beforeCommands), runContext.render(this.commands)))
                .run();
        } else {
            return new CommandsWrapper(runContext)
                .withWarningOnStdErr(true)
                .withRunnerType(RunnerType.DOCKER)
                .withDockerOptions(injectDefaults(getDocker()))
                .withEnv(Optional.ofNullable(env).orElse(new HashMap<>()))
                .withCommands(ScriptService.scriptCommands(List.of("/bin/sh", "-c"), null, runContext.render(this.commands)))
                .run();
        }

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
