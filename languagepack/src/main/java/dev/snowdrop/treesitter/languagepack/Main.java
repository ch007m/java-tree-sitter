package dev.snowdrop.treesitter.languagepack;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.aesh.AeshRuntimeRunner;

@QuarkusMain
public class Main implements QuarkusApplication {

    @Override
    public int run(String... args) {
        AeshRuntimeRunner.builder()
                .command(TsGroupCommand.class)
                .args(args)
                .execute();
        return 0;
    }
}
