package rife.bld.operations;


import rife.bld.BldVersion;

import java.io.IOException;

/**
 * Outputs the version of the build system.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.2
 */
public class VersionOperation extends AbstractOperation<VersionOperation> {
    /**
     * Performs the version operation.
     *
     * @throws IOException when an error occurred during the upgrade operation
     * @since 1.5.2
     */
    public void execute() {
        if (!silent()) {
            System.out.println("""
                     _     _     _
                    | |   | |   | |
                    | |__ | | __| |
                    | '_ \\| |/ _` |
                    | |_) | | (_| |
                    |_.__/|_|\\__,_|
                    """);
            System.out.println("bld " + BldVersion.getVersion());
        }
    }
}
