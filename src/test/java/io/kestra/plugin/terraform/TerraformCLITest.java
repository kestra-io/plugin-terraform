package io.kestra.plugin.terraform;

import io.kestra.core.runners.RunContext;
import io.kestra.core.runners.RunContextFactory;
import io.kestra.core.utils.IdUtils;
import io.kestra.core.utils.TestsUtils;
import io.kestra.plugin.scripts.exec.scripts.models.DockerOptions;
import io.kestra.plugin.scripts.exec.scripts.models.ScriptOutput;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@MicronautTest
class TerraformCLITest {

    @Inject
    private RunContextFactory runContextFactory;

    @Test
    @SuppressWarnings("unchecked")
    void run() throws Exception {
        String environmentKey = "MY_KEY";
        String environmentValue = "MY_VALUE";

        TerraformCLI.TerraformCLIBuilder<?, ?> terraformBuilder = TerraformCLI.builder()
            .id(IdUtils.create())
            .type(TerraformCLI.class.getName())
            .docker(DockerOptions.builder().image("hashicorp/terraform").entryPoint(Collections.emptyList()).build())
            .commands(List.of("terraform version"));

        TerraformCLI runner = terraformBuilder.build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, runner, Map.of("environmentKey", environmentKey, "environmentValue", environmentValue));

        ScriptOutput scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));

        runner = terraformBuilder
            .env(Map.of("{{ inputs.environmentKey }}", "{{ inputs.environmentValue }}"))
            .beforeCommands(List.of("terraform init"))
            .commands(List.of(
                "echo \"::{\\\"outputs\\\":{" +
                    "\\\"customEnv\\\":\\\"$" + environmentKey + "\\\"" +
                    "}}::\"",
                "terraform validate | tr -d ' \n' | xargs -0 -I {} echo '::{\"outputs\":{}}::'"
                             ))
            .build();

        scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));
        assertThat(scriptOutput.getVars().get("customEnv"), is(environmentValue));
    }
}