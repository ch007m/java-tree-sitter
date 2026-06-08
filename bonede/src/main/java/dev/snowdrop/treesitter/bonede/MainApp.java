package dev.snowdrop.treesitter.bonede;

import dev.snowdrop.treesitter.bonede.command.TsGroupCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.aesh.AeshRuntimeRunner;

@QuarkusMain
public class MainApp implements QuarkusApplication {

    @Override
    public int run(String... args) {
        AeshRuntimeRunner.builder()
                .command(TsGroupCommand.class)
                .args(args)
                .execute();
        return 0;
    }
}
