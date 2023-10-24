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
        String envKey = "MY_KEY";
        String envValue = "MY_VALUE";

        TerraformCLI runner = TerraformCLI.builder()
            .id(IdUtils.create())
            .type(TerraformCLI.class.getName())
            .docker(DockerOptions.builder().networkMode("host").image("hashicorp/terraform").entryPoint(Collections.emptyList()).build())
            .env(Map.of("{{ inputs.envKey }}", "{{ outputs.envValue }}"))
            .commands(List.of("terraform version"))
            .build();

        RunContext runContext = TestsUtils.mockRunContext(runContextFactory, runner, Map.of("envKey", envKey, "envValue", envValue));

        ScriptOutput scriptOutput = runner.run(runContext);
        assertThat(scriptOutput.getExitCode(), is(0));
    }
}