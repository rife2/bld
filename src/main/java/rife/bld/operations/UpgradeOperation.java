package rife.bld.operations;

import rife.bld.BldVersion;
import rife.bld.wrapper.Wrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Upgrades the project's bld wrapper to the version of the running
 * RIFE2 framework.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class UpgradeOperation extends AbstractOperation<UpgradeOperation> {
    /**
     * Performs the upgrade operation.
     *
     * @throws IOException when an error occurred during the upgrade operation
     * @since 1.5
     */
    public void execute()
    throws IOException {
        new Wrapper().createWrapperFiles(Path.of("lib", "bld").toFile(), BldVersion.getVersion());
        new Wrapper().upgradeIdeaBldLibrary(new File(".idea"), BldVersion.getVersion());
        new Wrapper().upgradeVscodeSettings(new File(".vscode"), BldVersion.getVersion());
        if (!silent()) {
            System.out.println("The wrapper was successfully upgraded to " + BldVersion.getVersion() + ".");
        }
    }
}
