package dev.snowdrop.treesitter.jtree;

import dev.snowdrop.treesitter.jtree.command.JtGroupCommand;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.aesh.AeshRuntimeRunner;

@QuarkusMain
public class Main implements QuarkusApplication {

    @Override
    public int run(String... args) {
        AeshRuntimeRunner.builder()
                .command(JtGroupCommand.class)
                .args(args)
                .execute();
        return 0;
    }
}
