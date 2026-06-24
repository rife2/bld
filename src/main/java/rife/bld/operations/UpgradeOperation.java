package rife.bld.operations;

import rife.bld.BldVersion;
import rife.bld.wrapper.Wrapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Upgrades the project's bld wrapper to the version of the running
 * bld tool.
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
        var bld_dir = Path.of("lib", "bld").toFile();
        var idea_dir = new File(".idea");
        var vscode_dir = new File(".vscode");

        if (verbose()) {
            System.out.println("Creating wrapper files in '" + bld_dir.getAbsolutePath() + "'");
        }
        new Wrapper().createWrapperFiles(bld_dir, BldVersion.getVersion());

        if (verbose()) {
            System.out.println("Upgrading IDEA bld library in '" + idea_dir.getAbsolutePath() + "'");
        }
        new Wrapper().upgradeIdeaBldLibrary(idea_dir, BldVersion.getVersion());

        if (verbose()) {
            System.out.println("Upgrading VSCode settings in '" + vscode_dir.getAbsolutePath() + "'");
        }
        new Wrapper().upgradeVscodeSettings(vscode_dir, BldVersion.getVersion());

        if (!silent()) {
            System.out.println("The wrapper was successfully upgraded to " + BldVersion.getVersion() + ".");
        }
    }
}
