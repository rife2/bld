/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife;

import rife.bld.BaseProject;
import rife.bld.BuildExecutor;
import rife.bld.dependencies.Repository;
import rife.bld.dependencies.VersionNumber;
import rife.bld.operations.AbstractOperation;
import rife.tools.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

/**
 * Releases bld together with RIFE2, RIFE2/core and the extensions bld
 * builds with, in the order their dependencies demand.
 * <p>
 * <b>What this is.</b> The bash script this would otherwise be: the release
 * steps, run by the one person who has the permissions to release these
 * projects, from a terminal, watching the output.
 * <p>
 * <b>What this deliberately isn't.</b> It keeps no state between runs, is
 * not resumable, and never repairs, rewinds or reconciles anything. Each
 * phase runs from its start every time. When it stops halfway, the person
 * running it reads the message, sorts out the cause and runs the phase
 * again.
 * <p>
 * It also doesn't police the workspace. It refuses only where the mistake
 * would be public and permanent: publishing a version that is already out,
 * publishing something other than what was built, publishing from a checkout
 * whose push would then fail, releasing a build that depends on a snapshot,
 * and a simulated run that could reach the outside world. Everything else is
 * visible in the output to whoever is watching it.
 * <p>
 * <b>The order</b> is the extensions, then core, then RIFE2, then bld.
 * Generated projects resolve from Maven Central only, so the RIFE2 the
 * blueprint points at has to be there before bld is published. An extension
 * compiles against bld and bld's wrapper resolves the extensions, which is a
 * cycle, broken by building everything against the local repository until
 * each piece is published. The extensions go first because bld is what
 * names them: a bld released before them would carry a wrapper naming
 * extension versions that aren't public, and anyone building it from its
 * own tag would fail. The reverse window costs nothing that anyone can see: a
 * just published extension can't be resolved until bld is public, since its
 * pom names bld in compile scope, but no published artifact depends on it
 * before then and no tag is public either, because the pushes wait until the
 * whole set is out.
 * <p>
 * The versions live in {@code release-train.properties}. The shape argument
 * decides which of them a run releases: {@code bld}, {@code bld+rife2},
 * {@code bld+core} or {@code bld+rife2+core}. Everything outside the shape
 * is only put on the new bld. Setting {@code train.simulate} confines the
 * publications and the pushes to this machine, which is how this can be
 * rehearsed. See {@code RELEASE-TRAIN.md}.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 */
public class ReleaseTrainOperation extends AbstractOperation<ReleaseTrainOperation> {
    private static final String CONFIG_FILE = "release-train.properties";
    private static final String WRAPPER_PROPERTIES = "lib/bld/bld-wrapper.properties";
    private static final String WRAPPER_BACKUP_SUFFIX = ".train-orig";
    private static final String EXTENSION_PREFIX = "bld.extension";
    private static final String GROUP = "com.uwyn.rife2";
    private static final String VERSION_DIRECTORY = "src/main/resources/";
    private static final String CORE_VERSION_FILE = "CORE_VERSION";
    private static final String BLD_VERSION_FILE = "BLD_VERSION";
    private static final String RIFE2_VERSION_FILE = "RIFE_VERSION";
    private static final String BLUEPRINT_SOURCE = "src/main/java/rife/bld/blueprints/Rife2ProjectBlueprint.java";
    // a version reaches a build file as a version(1, 2, 3) literal and the
    // repositories as a List.of call, so what a project publishes is edited
    // in its source rather than configured anywhere
    private static final String REPOSITORIES = "repositories = List.of(";
    private static final String LOCAL_REPOSITORIES = "repositories = List.of(MAVEN_LOCAL, ";
    private static final String JVM_PROPERTY_SUFFIX = " through a -D of this JVM";

    private static final Pattern BLD_DEPENDENCY = Pattern.compile(
        "(dependency\\(\"com\\.uwyn\\.rife2\",\\s*\"bld\",\\s*version\\()[^)]+(\\)\\))");
    private static final Pattern RIFE2_BLUEPRINT_DEPENDENCY = Pattern.compile(
        "(dependency\\(\"com\\.uwyn\\.rife2\",\\s*\"rife2\",\\s*version\\()[^)]+(\\)\\))");
    // anchored to the start of the line so that fields like
    // some_tool_version = version(...) can never match
    private static final Pattern EXTENSION_OWN_VERSION = Pattern.compile(
        "(?m)^(\\s*version\\s*=\\s*version\\()[^)]+(\\))");
    // a snapshot version, as a qualifier or spelled out, wherever a build
    // source keeps it
    private static final List<String> ARTIFACTS = List.of(".pom", ".jar", "-sources.jar", "-javadoc.jar");
    private static final Pattern POM_DEPENDENCY = Pattern.compile(
        "(?s)<dependency>\\s*<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>\\s*<version>([^<]+)</version>");
    private static final Pattern SNAPSHOT_LITERAL = Pattern.compile("\"[^\"\\n]*-SNAPSHOT\"|\"SNAPSHOT\"");

    private File workspace_;
    private File bldDir_;
    private File coreDir_;
    private File rife2Dir_;
    private File rife2CoreDir_;
    private String bldVersion_;
    private String rife2Version_;
    private String coreVersion_;
    private String releasesRepository_;
    private String publishedRepository_;
    private boolean simulate_;
    private boolean tests_;
    private boolean releaseRife2_;
    private boolean releaseCore_;
    private final LinkedHashMap<String, String> extensionVersions_ = new LinkedHashMap<>();
    private final List<PendingPush> pushes_ = new ArrayList<>();
    private final List<String> followers_ = new ArrayList<>();
    private List<String> arguments_ = new ArrayList<>();

    /**
     * Points the train at the workspace: the bld project directory this runs
     * from, with core inside it and the other repositories beside it.
     */
    public ReleaseTrainOperation fromProject(BaseProject project) {
        bldDir_ = project.workDirectory();
        workspace_ = bldDir_.getParentFile();
        coreDir_ = new File(bldDir_, "core");
        rife2Dir_ = new File(workspace_, "rife2");
        rife2CoreDir_ = new File(rife2Dir_, "core");
        return this;
    }

    /**
     * The phase and the shape, as they were typed after {@code release-train}.
     * This is the list bld is still working through, and taking them off it
     * is what says they belong to this command rather than being commands of
     * their own.
     */
    public ReleaseTrainOperation arguments(List<String> arguments) {
        arguments_ = arguments;
        return this;
    }

    /**
     * Runs one phase against one shape. The phase defaults to {@code plan},
     * which only reports, and every phase needs a shape.
     */
    public void execute()
    throws Exception {
        var command = arguments_.isEmpty() ? "plan" : arguments_.remove(0);
        var shape = arguments_.isEmpty() ? null : arguments_.remove(0);

        loadConfig();
        resolveShape(shape);

        switch (command) {
            case "plan" -> plan();
            case "review" -> review();
            case "local" -> local();
            case "publish" -> publish();
            case "converge" -> converge();
            default -> throw new IllegalStateException("Unknown release-train command '" + command +
                                                       "', use: plan, local, review, publish, converge");
        }
    }

    /*
     * Configuration
     */

    private void loadConfig()
    throws IOException {
        var config_file = new File(bldDir_, CONFIG_FILE);
        if (!config_file.exists()) {
            throw new IllegalStateException("Missing " + CONFIG_FILE + " in the bld project directory, declare the versions there first.");
        }
        var config = new Properties();
        try (var reader = Files.newBufferedReader(config_file.toPath())) {
            config.load(reader);
        }
        bldVersion_ = version(config, "train.bld.version");
        rife2Version_ = version(config, "train.rife2.version");
        coreVersion_ = version(config, "train.core.version");
        config.stringPropertyNames().stream()
            .filter(name -> name.startsWith("train.extension."))
            .sorted()
            .forEach(name -> extensionVersions_.put(name.substring("train.extension.".length()), version(config, name)));
        releasesRepository_ = config.getProperty("train.releases.repository", "").trim();
        publishedRepository_ = config.getProperty("train.published.repository", "").trim();
        simulate_ = flag(config, "train.simulate", false);
        tests_ = flag(config, "train.tests", true);
        for (var follower : config.getProperty("train.followers", "").split(",")) {
            if (!follower.isBlank()) {
                followers_.add(follower.trim());
            }
        }
    }

    private void resolveShape(String shape) {
        if (shape == null) {
            throw new IllegalStateException(
                "Specify the shape of the release:\n" +
                "  bld             release bld and the extensions\n" +
                "  bld+rife2       also release RIFE2\n" +
                "  bld+rife2+core  also release RIFE2 and core\n" +
                "  bld+core        also release core\n" +
                "For example: ./bld release-train plan bld+rife2");
        }
        var includes_bld = false;
        for (var token : shape.split("\\+")) {
            switch (token) {
                case "bld" -> includes_bld = true;
                case "rife2" -> releaseRife2_ = true;
                case "core" -> releaseCore_ = true;
                default -> throw new IllegalStateException("Unknown member '" + token + "' in shape '" + shape + "'.");
            }
        }
        if (!includes_bld) {
            throw new IllegalStateException("Every release includes bld, the shape has to name it.");
        }
    }

    private static boolean flag(Properties config, String name, boolean fallback) {
        var value = config.getProperty(name, String.valueOf(fallback)).trim();
        if (value.isEmpty() || value.equals("false")) {
            return false;
        }
        if (value.equals("true")) {
            return true;
        }
        throw new IllegalStateException("'" + name + "' has to be 'true' or 'false' in " + CONFIG_FILE + ", not '" + value + "'.");
    }

    /**
     * A version of the release, which also has to be writable as a
     * {@code version(1, 2, 3)} literal, since that is how it ends up in the
     * build sources and the blueprint.
     */
    private static String version(Properties config, String name) {
        var value = config.getProperty(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing '" + name + "' in " + CONFIG_FILE + ".");
        }
        value = value.trim();
        var parsed = VersionNumber.parse(value);
        if (parsed.equals(VersionNumber.UNKNOWN) || !parsed.qualifier().isEmpty() ||
            parsed.minor() == null || parsed.revision() == null) {
            throw new IllegalStateException("'" + name + "' has to be a plain major.minor.revision version, not '" + value + "'.");
        }
        return value;
    }

    private static String versionLiteral(String version) {
        var parsed = VersionNumber.parse(version);
        return parsed.major() + "," + parsed.minor() + "," + parsed.revision();
    }

    /*
     * Phases
     */

    private void plan() {
        System.out.println("Release plan:");

        var rows = new ArrayList<PlanRow>();
        rows.add(memberRow("core", coreDir_, currentVersion(coreDir_, CORE_VERSION_FILE), releaseCore_ ? coreVersion_ : null));
        rows.add(memberRow("bld", bldDir_, currentVersion(bldDir_, BLD_VERSION_FILE), bldVersion_));
        rows.add(memberRow("rife2", rife2Dir_, currentVersion(rife2Dir_, RIFE2_VERSION_FILE), releaseRife2_ ? rife2Version_ : null));
        extensionVersions_.forEach((name, version) -> {
            var dir = new File(workspace_, name);
            rows.add(memberRow(name, dir, currentExtensionVersion(dir), version));
        });
        followers_.forEach(follower -> rows.add(new PlanRow("follower", null, follower + "  (converge only)", null)));
        rows.add(new PlanRow("tests", null, tests_ ? "run by every build in the train" : "left to CI, the builds only compile", null));

        var versioned = rows.stream().filter(row -> row.current() != null).toList();
        var labels = width(rows, PlanRow::label);
        var currents = width(versioned, PlanRow::current);
        var changes = width(versioned, PlanRow::change);
        for (var row : rows) {
            var line = new StringBuilder("  ").append(pad(row.label(), labels));
            if (row.current() == null) {
                line.append("  ").append(row.change());
            } else {
                line.append("  ").append(pad(row.current(), currents))
                    .append("  ").append(pad(row.change(), changes))
                    .append("  ").append(row.trailer());
            }
            System.out.println(line.toString().stripTrailing());
        }

        System.out.println();
        problems().forEach(problem -> System.out.println("  ! " + problem));
        System.out.println();
        System.out.println("Phases: local -> publish -> converge, run each with './bld release-train <phase> <shape>'.");
        System.out.println("'review' shows what a phase changed and what it published, without changing anything.");
    }

    private record PlanRow(String label, String current, String change, String trailer) {
    }

    private PlanRow memberRow(String label, File dir, String current, String version) {
        return new PlanRow(label, current,
            version == null ? "(not in this release, converge only)" : "->  " + version,
            "(" + dir + ")" + (dir.exists() ? "" : "  MISSING CHECKOUT"));
    }

    private static int width(List<PlanRow> rows, Function<PlanRow, String> cell) {
        return rows.stream().mapToInt(row -> cell.apply(row).length()).max().orElse(0);
    }

    private static String pad(String text, int width) {
        return text.length() >= width ? text : text + " ".repeat(width - text.length());
    }

    /**
     * The version an extension declares for itself, which is where its
     * version lives rather than in a version file.
     */
    private String currentExtensionVersion(File dir) {
        try {
            var matcher = EXTENSION_OWN_VERSION.matcher(FileUtils.readString(findBuildFile(dir)));
            if (!matcher.find()) {
                return "?";
            }
            // the pattern brackets the arguments without capturing them, since
            // replacing a version relies on those two groups being the ends
            var literal = matcher.group();
            return literal.substring(matcher.group(1).length(), literal.length() - matcher.group(2).length())
                .replace(" ", "").replace(",", ".");
        } catch (Exception e) {
            return "?";
        }
    }

    /**
     * Shows what is waiting to be committed in every repository the release
     * touches, with the rewrites the phases made separated from whatever was
     * already there. A release commits everything a repository holds, so the
     * second group rides along with it.
     * <p>
     * The two are told apart by path, so an edit of your own in a file the
     * phases rewrite goes unmarked. What it catches is the file a release has
     * no reason to touch at all.
     * <p>
     * It only reads, which makes it safe to run between any two phases.
     */
    private void review() {
        var pending = new LinkedHashMap<File, List<String>>();
        for (var repo : reviewDirs()) {
            if (!repo.exists()) {
                continue;
            }
            var changes = git(repo).status();
            if (changes != null && !changes.isBlank()) {
                pending.put(repo, changes.lines().toList());
            }
        }

        // the note trails the longest of them all, so it reads as one column
        // instead of following each path around
        var marked = pending.values().stream().flatMap(List::stream)
            .filter(line -> !isTrainRewrite(statusPath(line)))
            .mapToInt(String::length).max().orElse(0);

        for (var entry : pending.entrySet()) {
            System.out.println(entry.getKey().getName() + "  (" + entry.getKey() + ")");
            var foreign = new ArrayList<String>();
            for (var line : entry.getValue()) {
                if (isTrainRewrite(statusPath(line))) {
                    System.out.println("   " + line);
                } else {
                    foreign.add(line);
                }
            }
            foreign.forEach(line -> System.out.println("   " + pad(line, marked) +
                                                       "   <- not from the release, it gets committed too"));
            System.out.println();
        }
        if (!pending.isEmpty()) {
            System.out.println("The unmarked paths are the ones a release rewrites. An edit of your own");
            System.out.println("in one of them is unmarked as well, and gets committed with them.");
        } else {
            System.out.println("Nothing is waiting to be committed.");
        }
        System.out.println();
        reviewPublications();
    }

    /**
     * Shows what the local phase left in the local repository, which is the
     * preview of what the publish phase uploads. Core isn't listed because the
     * local phase doesn't publish it there. It does have a publication of its
     * own, which the publish phase makes when the shape includes core.
     * <p>
     * A pom that names a snapshot is what a consumer would resolve, so a
     * build source the local phase failed to rewrite surfaces here rather
     * than after the release is public.
     */
    private void reviewPublications() {
        var published = new LinkedHashMap<String, String>();
        published.put("bld", bldVersion_);
        extensionVersions_.forEach(published::put);
        if (releaseRife2_) {
            published.put("rife2", rife2Version_);
        }

        System.out.println("Published into " + localRepository() + ":");
        var names = published.keySet().stream().mapToInt(String::length).max().orElse(0);
        var versions = published.values().stream().mapToInt(String::length).max().orElse(0);
        for (var entry : published.entrySet()) {
            var directory = new File(localRepository(), GROUP.replace('.', File.separatorChar) +
                                                        File.separator + entry.getKey() + File.separator + entry.getValue());
            var line = new StringBuilder("  ").append(pad(entry.getKey(), names))
                .append("  ").append(pad(entry.getValue(), versions)).append("  ");
            if (!directory.isDirectory()) {
                System.out.println(line + "not published locally");
                continue;
            }
            var files = List.of(directory.list() == null ? new String[0] : directory.list());
            var base = entry.getKey() + "-" + entry.getValue();
            var missing = ARTIFACTS.stream().filter(artifact -> !files.contains(base + artifact)).toList();
            System.out.println(line + (missing.isEmpty() ? "pom, jar, sources, javadoc"
                                                         : "missing " + String.join(", ", missing)));
            snapshotsInPom(new File(directory, base + ".pom"))
                .forEach(dependency -> System.out.println("     ! its pom depends on the snapshot " + dependency));
        }
    }

    private File localRepository() {
        return new File(System.getProperty("user.home"), ".m2" + File.separator + "repository");
    }

    private static List<String> snapshotsInPom(File pom) {
        var snapshots = new ArrayList<String>();
        try {
            var source = FileUtils.readString(pom);
            var matcher = POM_DEPENDENCY.matcher(source);
            while (matcher.find()) {
                if (matcher.group(3).contains("SNAPSHOT")) {
                    snapshots.add(matcher.group(1) + ":" + matcher.group(2) + ":" + matcher.group(3));
                }
            }
        } catch (Exception e) {
            snapshots.add("(couldn't read " + pom + ": " + e.getMessage() + ")");
        }
        return snapshots;
    }

    private static String statusPath(String statusLine) {
        return statusLine.substring(Math.min(3, statusLine.length()));
    }

    private List<File> reviewDirs() {
        var dirs = new ArrayList<File>();
        dirs.add(bldDir_);
        dirs.add(coreDir_);
        dirs.add(rife2Dir_);
        dirs.add(rife2CoreDir_);
        extensionVersions_.keySet().forEach(name -> dirs.add(new File(workspace_, name)));
        followers_.forEach(follower -> dirs.add(new File(workspace_, follower)));
        return dirs;
    }

    /**
     * Whether a path is one the phases rewrite themselves. The wrapper also
     * regenerates the IDE files it maintains, and a version file inside a
     * submodule moves the pointer that records it.
     */
    private static boolean isTrainRewrite(String path) {
        return path.equals(VERSION_DIRECTORY + BLD_VERSION_FILE) ||
               path.equals(VERSION_DIRECTORY + CORE_VERSION_FILE) ||
               path.equals(VERSION_DIRECTORY + RIFE2_VERSION_FILE) ||
               path.equals(BLUEPRINT_SOURCE) ||
               path.equals("core") ||
               path.equals("bld") || path.equals("bld.bat") ||
               path.startsWith("lib/bld/") ||
               path.startsWith(".idea/") || path.startsWith(".vscode/") ||
               path.startsWith("src/bld/java/") && path.endsWith("Build.java");
    }

    /**
     * Builds everything against local publications. Nothing leaves the
     * machine, so this can be run as often as needed.
     */
    private void local()
    throws Exception {
        requireNoProblems();

        if (releaseCore_) {
            step("set core version " + coreVersion_);
            writeVersion(coreDir_, CORE_VERSION_FILE, coreVersion_);
            // RIFE2 compiles core from its own checkout
            writeVersion(rife2CoreDir_, CORE_VERSION_FILE, coreVersion_);
            step(tests_ ? "build and test core" : "build core");
            build(coreDir_);
        }

        step("set bld version " + bldVersion_);
        writeVersion(bldDir_, BLD_VERSION_FILE, bldVersion_);

        step("bootstrap build of bld with the previous wrapper");
        build(bldDir_);
        bld(bldDir_, "publish-local");

        for (var extension : extensionVersions_.entrySet()) {
            var dir = new File(workspace_, extension.getKey());
            step("update, build and locally publish " + extension.getKey() + " " + extension.getValue());
            pointWrapperAtLocalBld(dir);
            var build_file = findBuildFile(dir);
            replaceVersion(build_file, BLD_DEPENDENCY, bldVersion_, "the bld dependency");
            replaceVersion(build_file, EXTENSION_OWN_VERSION, extension.getValue(), "the project version");
            // an extension compiles against the bld being released, which is
            // only in the local repository until the publish phase
            addLocalRepository(dir, build_file);
            build(dir);
            bld(dir, "publish-local");
        }

        step("rebuild bld with itself and the new extensions");
        pointWrapperAtLocalBld(bldDir_);
        pinExtensions(bldDir_);
        build(bldDir_);
        bld(bldDir_, "publish-local");

        if (releaseRife2_) {
            step((tests_ ? "build, test and locally publish" : "build and locally publish") + " rife2 with the new bld and extensions");
            writeVersion(rife2Dir_, RIFE2_VERSION_FILE, rife2Version_);
            pointWrapperAtLocalBld(rife2Dir_);
            pinExtensions(rife2Dir_);
            build(rife2Dir_);
            // the smoke project resolves this version
            bld(rife2Dir_, "publish-local");
        }

        // the local repository stays in these wrappers: nothing they now
        // reference is public yet
        var members = allMemberDirs();
        for (var repo : convergeDirs()) {
            step("put " + repo.getName() + " on bld " + bldVersion_);
            makeWrapperCoherent(repo);
            if (members.contains(repo)) {
                build(repo);
            } else {
                bld(repo, "clean", "compile");
            }
        }

        if (releaseRife2_) {
            // bld's tests generate projects from the blueprint and resolve
            // them publicly, so it can only name a RIFE2 that is out. This
            // points it at the new one for the scratch project alone
            step("point the blueprint at rife2 " + rife2Version_ + " and rebuild bld");
            bumpBlueprint();
            bld(bldDir_, "clean", "compile");
            bld(bldDir_, "publish-local");
        }

        step("generate and build a scratch project against the local publications");
        smokeTest();

        if (releaseRife2_) {
            // so that running this phase again starts from a bld whose tests
            // can pass
            step("put the blueprint back until rife2 " + rife2Version_ + " is public");
            git(bldDir_).run("checkout", "--", BLUEPRINT_SOURCE);
        }

        // the local repository stays in the wrappers, since the phases after
        // this one are launched by them and the extensions aren't public yet

        System.out.println();
        System.out.println("Local phase done. Review the changes, then './bld release-train publish <shape>'.");
    }

    /**
     * Commits, tags, publishes and pushes, in the order the dependencies
     * demand. Every publication asks first.
     */
    private void publish()
    throws Exception {
        requireNoProblems();

        step("verify that the local phase left everything ready to release");
        requireReady();

        // extensions first: bld is what names them, so a bld released
        // before them would tag a wrapper naming versions that aren't out
        for (var extension : extensionVersions_.entrySet()) {
            releaseRepo(new File(workspace_, extension.getKey()), extension.getKey(), extension.getValue(),
                "Released " + extension.getKey() + " " + extension.getValue());
        }

        if (releaseCore_) {
            var core_commit = releaseRepo(coreDir_, "rife2-core", coreVersion_, "Released RIFE2/core " + coreVersion_);
            // whether or not RIFE2 is released here, or the two checkouts
            // are left recording different cores
            step("put the core checkout of rife2 on the released core commit");
            advanceRife2Core(core_commit, "refs/tags/" + coreVersion_);
        }
        if (releaseRife2_) {
            releaseRepo(rife2Dir_, "rife2", rife2Version_, "Released RIFE2 " + rife2Version_);
            // the blueprint of the bld that follows names this version, and
            // generated projects resolve from Central only
            step("wait until rife2 " + rife2Version_ + " is resolvable");
            waitForRelease("rife2", rife2Version_);
            // only now can bld build against it, see the local phase
            step("point the blueprint at rife2 " + rife2Version_ + (tests_ ? ", build and test bld" : " and build bld"));
            bumpBlueprint();
            build(bldDir_);
        }
        releaseRepo(bldDir_, "bld", bldVersion_, "Released bld " + bldVersion_);
        step("wait until bld " + bldVersion_ + " is resolvable");
        waitForRelease("bld", bldVersion_);

        pushReleases();

        System.out.println();
        System.out.println("Publish phase done. Continue with './bld release-train converge <shape>'.");
    }

    /**
     * Commits and pushes what the release itself didn't: the members outside
     * the shape and the followers, which only moved to the new bld.
     */
    private void converge()
    throws Exception {
        requireNoProblems();

        // RIFE2 compiles core from its own checkout, so it follows the commit
        // converge makes in bld's, but only when the two already agreed. A
        // shape that never required them to match keeps them as they are
        // rather than moving RIFE2 onto core sources it wasn't built against
        var bld_core_before = coreDir_.exists() ? git(coreDir_).head() : null;
        var rife2_core_before = rife2CoreDir_.exists() ? git(rife2CoreDir_).head() : null;
        var cores_agreed = bld_core_before != null && bld_core_before.equals(rife2_core_before);

        for (var repo : convergeDirs()) {
            var git = git(repo);
            // whatever wasn't released still carries the local repository
            finalizeWrapper(repo);
            if (extensionVersions_.containsKey(repo.getName())) {
                removeLocalRepository(repo, findBuildFile(repo));
            }
            var changes = git.status();
            if (!changes.isBlank()) {
                step("commit and push the bld " + bldVersion_ + " update in " + repo.getName());
                changes.lines().forEach(line -> System.out.println("  " + line));
                if (!confirm("Commit and push this in " + repo.getName() + "?")) {
                    throw new IllegalStateException("The update of " + repo.getName() + " wasn't confirmed.");
                }
                if (!git.commitEverything("Updated to bld " + bldVersion_)) {
                    System.out.println("   nothing to commit, only content inside a submodule differs");
                }
            } else {
                step("push " + repo.getName());
            }
            // a run that committed and then failed to push leaves a clean
            // repository whose commit hasn't arrived
            git.pushBranch();
            if (repo.equals(coreDir_) && cores_agreed &&
                !git.head().equals(git(rife2CoreDir_).head())) {
                advanceRife2Core(git.head(), git.requireBranch());
            }
        }

        step("verify that nothing is left on a local build or a snapshot");
        requireReleasedVersionsOnly();

        System.out.println();
        System.out.println("Release complete.");
    }

    /**
     * Commits the release, tags it, publishes it and queues the push of the
     * commit and the tag for {@link #pushReleases}. Returns the commit that
     * was released.
     */
    private String releaseRepo(File dir, String artifactId, String version, String message)
    throws Exception {
        var git = git(dir);

        // the tag is pushed with the release commit right after publishing,
        // so finding it there says this one is out
        var released = git.releasedCommit(version);
        if (released != null) {
            step(dir.getName() + " " + version + " is already released as " + released);
            return released;
        }

        step("release " + dir.getName() + " " + version);

        // what it needs is public by now: an extension uses none of the
        // extensions this releases, everything else follows all of them.
        // The publish build below still resolves from the local repository
        finalizeWrapper(dir);
        if (extensionVersions_.containsKey(dir.getName())) {
            removeLocalRepository(dir, findBuildFile(dir));
        }
        var problems = releasedVersionProblems(List.of(dir));
        if (!problems.isEmpty()) {
            problems.forEach(problem -> System.out.println("  ! " + problem));
            throw new IllegalStateException(dir.getName() + " isn't ready to be released.");
        }

        // a tag of this version says an earlier run of this phase got as far
        // as tagging, which is the only thing tying a publication to these
        // sources. Both are read before anything is committed, so a refusal
        // doesn't leave behind the tag that would change the answer next time
        var tagged_before = git.hasTag(version);
        var already_published = publicationAlreadyHappened(dir, artifactId, version, tagged_before);

        // committed first, so that a tag left by an earlier attempt is judged
        // against the commit that will actually be published
        var changes = git.status();
        if (!changes.isBlank()) {
            System.out.println("The release commit will contain:");
            changes.lines().forEach(line -> System.out.println("  " + line));
            if (!git.commitEverything(message)) {
                System.out.println("   nothing to commit, only content inside a submodule differs");
            }
        }
        if (!tagged_before) {
            git.run("tag", "-a", "-m", message, version);
        } else if (!git.head().equals(git.commitOf(version))) {
            throw new IllegalStateException("The tag " + version + " in " + dir.getName() + " is on " + git.commitOf(version) +
                                            " instead of the commit that would be published, " + git.head() +
                                            ". Remove or move it yourself, then run this again.");
        }

        // a branch that moved on would otherwise only fail the push after the
        // release is public
        git.requireFastForward();

        if (already_published) {
            step(artifactId + " " + version + " is already published, it only has to be pushed");
        } else {
            if (!confirm("Publish " + dir.getName() + " " + version + (simulate_ ? " to the simulated repository?" : " publicly?"))) {
                throw new IllegalStateException("Publication of " + dir.getName() + " wasn't confirmed.");
            }
            // publishing builds again, against a release that is still only in
            // the local repository. The commit just made is the clean one
            useLocalRepositoryFor(dir);
            try {
                bld(dir, "clean", "publish");
            } catch (Exception e) {
                // released coordinates are immutable, so a publication that
                // reached some of its repositories can't be repeated
                System.out.println();
                System.out.println("The publication of " + dir.getName() + " " + version + " failed. It publishes to");
                System.out.println("several repositories in one command, so check which of them already have it");
                System.out.println("before running this phase again.");
                throw e;
            } finally {
                restoreCommitted(dir);
            }
        }
        pushes_.add(new PendingPush(dir, version));
        return git.head();
    }

    /**
     * Whether this release was already published, which a rerun after a
     * failed push has to know, since released coordinates can't be written
     * twice. The gate reads one repository, and a coordinate sitting there
     * says nothing about the sources it was built from, so the only
     * publication this skips is one an earlier run left behind: its tag is
     * still on the commit being published, and the gate can see the release.
     * Anything else stops the phase and says what to check.
     */
    private boolean publicationAlreadyHappened(File dir, String artifactId, String version, boolean taggedBefore) {
        var published = publicationState(artifactId, version);

        // a publication can't be undone, so it never happens while the gate
        // can't be read. Nothing has been committed or tagged by this point,
        // so running the phase again is the whole of the recovery
        if (published == Published.UNKNOWN) {
            System.out.println();
            System.out.println(publishedGate() + " couldn't be reached, so whether " + artifactId + " " + version);
            System.out.println("is already published can't be established. Nothing has been committed or");
            System.out.println("tagged yet, so run this phase again once it answers.");
            throw new IllegalStateException("Couldn't reach " + publishedGate() + " to see whether " +
                                            artifactId + " " + version + " is published.");
        }

        if (!taggedBefore) {
            if (published == Published.YES) {
                System.out.println();
                System.out.println(artifactId + " " + version + " is already in " + publishedGate() + ",");
                System.out.println("and " + dir.getName() + " carries no tag for it, so nothing says that what is");
                System.out.println("published there came from these sources. Find the commit it was built");
                System.out.println("from before releasing this version.");
                throw new IllegalStateException(artifactId + " " + version + " is published from an unknown commit.");
            }
            return false;
        }
        if (published == Published.YES) {
            return true;
        }

        System.out.println();
        System.out.println(dir.getName() + " already carries the tag " + version + " from an earlier run of this phase,");
        System.out.println("and " + publishedGate() + " doesn't have " + artifactId + " " + version + ".");
        System.out.println("If that run never published, remove the tag with");
        System.out.println("  git -C " + dir + " tag -d " + version);
        System.out.println("and run this phase again. If it did publish, don't push the tag by hand: the");
        System.out.println("pushes wait until the whole set is out, and a tag pushed before bld is public");
        System.out.println("starts builds that can't resolve it. Point train.published.repository at a");
        System.out.println("repository that has the release and run this phase again instead.");
        throw new IllegalStateException("Can't establish whether " + artifactId + " " + version + " is already published.");
    }

    /**
     * What the gate can say about a release, where not being able to reach a
     * repository is its own answer rather than an absence.
     */
    private enum Published {
        YES, NO, UNKNOWN
    }

    private Published publicationState(String artifactId, String version) {
        var repository = publishedRepository().isBlank() ? Repository.MAVEN_CENTRAL : new Repository(publishedRepository());
        var directory = repository.getArtifactLocation(GROUP, artifactId) + version + "/";
        var pom = artifactId + "-" + version + ".pom";
        if (repository.isLocal()) {
            return new File(directory + pom).exists() ? Published.YES : Published.NO;
        }
        return availability(directory + pom);
    }

    /**
     * Where the gate looks to see whether a publication happened, which isn't
     * the question {@link #waitForRelease} asks. That one watches the
     * repository the next step resolves from, Maven Central for bld and
     * RIFE2. This one only needs a repository every member publishes to, and
     * the extensions never reach Central at all.
     */
    private String publishedRepository() {
        return publishedRepository_.isBlank() ? releasesRepository_ : publishedRepository_;
    }

    private String publishedGate() {
        return publishedRepository().isBlank() ? "Maven Central" : publishedRepository();
    }

    /**
     * Pushes every release that was published, which is what makes the tags
     * public and starts the builds that resolve them. Held back until the
     * whole set is out, so that no build starts against a release that only
     * some of it can see.
     */
    private void pushReleases()
    throws Exception {
        if (pushes_.isEmpty()) {
            return;
        }
        step("push the releases now that they are all published");
        for (var i = 0; i < pushes_.size(); ++i) {
            var push = pushes_.get(i);
            var git = git(push.dir());
            // together or not at all, so the tag can't name a commit that
            // never arrived
            try {
                git.pushRelease(push.version());
            } catch (Exception e) {
                System.out.println();
                System.out.println(push.dir().getName() + " " + push.version() + " IS PUBLISHED, only pushing it failed.");
                System.out.println("Everything is published, the pushes are all that's left. Run the ones");
                System.out.println("that didn't go through yourself:");
                for (var left : pushes_.subList(i, pushes_.size())) {
                    var left_git = git(left.dir());
                    System.out.println("  git -C " + left.dir() + " push --atomic " + left_git.pushRemote() +
                                       " HEAD:refs/heads/" + left_git.requireBranch() + " refs/tags/" + left.version());
                }
                throw e;
            }
        }
    }

    private record PendingPush(File dir, String version) {
    }

    /**
     * Puts the local repository back into a repository that was just made
     * clean for its release commit, so that the build behind its publication
     * can resolve the rest of this release.
     */
    private void useLocalRepositoryFor(File dir)
    throws Exception {
        pointWrapperAtLocalBld(dir);
        if (extensionVersions_.containsKey(dir.getName())) {
            addLocalRepository(dir, findBuildFile(dir));
        }
    }

    /**
     * Takes it back out by checking out what was committed, which is exact
     * since these files are part of the release commit.
     */
    private void restoreCommitted(File dir) {
        var git = git(dir);
        git.run("checkout", "--", WRAPPER_PROPERTIES);
        if (extensionVersions_.containsKey(dir.getName())) {
            var build_file = findBuildFile(dir);
            var relative = dir.toPath().relativize(build_file.toPath()).toString().replace(File.separatorChar, '/');
            git.run("checkout", "--", relative);
        }
        wrapperBackup(dir).delete();
    }

    private void bumpBlueprint()
    throws Exception {
        replaceVersion(new File(bldDir_, BLUEPRINT_SOURCE), RIFE2_BLUEPRINT_DEPENDENCY, rife2Version_, "the rife2 dependency");
    }

    /**
     * Puts RIFE2's core checkout on a commit of bld's: the one a core release
     * was published from, so that the RIFE2 release commit records the core
     * it was built against, or the one converge just made there. The version
     * file the local phase wrote is what a release commit holds, so it makes
     * way for the checkout.
     */
    private void advanceRife2Core(String commit, String ref)
    throws Exception {
        var rife2_core = git(rife2CoreDir_);
        var unexpected = rife2_core.uncommittedPaths().stream()
            .filter(path -> !path.equals(VERSION_DIRECTORY + CORE_VERSION_FILE))
            .toList();
        if (!unexpected.isEmpty()) {
            throw new IllegalStateException("The core checkout of rife2 holds changes of its own (" +
                                            String.join(", ", unexpected) + "), sort those out first.");
        }
        rife2_core.run("checkout", "--", ".");
        rife2_core.run("fetch", coreDir_.getAbsolutePath(), ref);
        rife2_core.run("checkout", "--detach", commit);
    }

    /*
     * The few things worth refusing over
     */

    private void requireNoProblems() {
        var problems = problems();
        if (!problems.isEmpty()) {
            problems.forEach(problem -> System.out.println("  ! " + problem));
            throw new IllegalStateException("Sort the problems above out first.");
        }
    }

    private List<String> problems() {
        var problems = new ArrayList<String>();

        for (var dir : convergeDirs()) {
            if (!dir.exists()) {
                problems.add("missing checkout: " + dir);
            } else if (git(dir).branch() == null) {
                problems.add(dir.getName() + " is on a detached HEAD, a push from there does nothing, check out a branch");
            }
        }

        // releasing core moves this checkout onto the released commit, after
        // core is public, so a missing one has to stop the run before it
        if (releaseCore_ && !rife2CoreDir_.exists()) {
            problems.add("missing checkout: " + rife2CoreDir_ + ", releasing core has to put it on the released commit");
        }

        // bld and RIFE2 each compile core from their own checkout, so those
        // are what ends up in the artifacts
        var both_cores = releaseCore_ || releaseRife2_;
        var cores = new ArrayList<File>();
        cores.add(coreDir_);
        if (both_cores) {
            cores.add(rife2CoreDir_);
        }
        for (var core : cores) {
            if (!core.exists()) {
                continue;
            }
            // only the sources: the phases rewrite the wrappers themselves,
            // and the version file when core is part of the release
            var pending = git(core).uncommittedPaths().stream()
                .filter(path -> path.startsWith("src/"))
                .filter(path -> !releaseCore_ || !path.equals(VERSION_DIRECTORY + CORE_VERSION_FILE))
                .toList();
            if (!pending.isEmpty()) {
                problems.add(core + " holds uncommitted sources (" + String.join(", ", pending) +
                             "), so it isn't the core that would be released");
            }
        }
        if (both_cores && coreDir_.exists() && rife2CoreDir_.exists()) {
            var bld_core = git(coreDir_).readLine("rev-parse", "HEAD");
            var rife2_core = git(rife2CoreDir_).readLine("rev-parse", "HEAD");
            if (bld_core == null || rife2_core == null || !bld_core.equals(rife2_core)) {
                // publishing again moves rife2's checkout onto the released
                // core, so a run that stopped in between isn't a state to
                // refuse over, it is one this phase finishes
                if (!coreAlreadyReleased(bld_core)) {
                    problems.add("the core checkouts aren't on the same commit, " + bld_core + " in " + coreDir_ +
                                 " and " + rife2_core + " in " + rife2CoreDir_ + ", check out the same one in both");
                }
            }
        }

        problems.addAll(snapshotExtensionProblems());
        problems.addAll(snapshotDependencyProblems());
        problems.addAll(simulationProblems());
        return problems;
    }

    /**
     * A wrapper that names a snapshot extension this release doesn't publish
     * would tag a repository against something that can still change. Whether
     * that is so doesn't depend on how far a release got, so it is refused
     * before anything is published rather than between two publications.
     */
    private List<String> snapshotExtensionProblems() {
        var problems = new ArrayList<String>();
        for (var repo : convergeDirs()) {
            if (!repo.exists()) {
                continue;
            }
            for (var line : committedWrapperLines(repo)) {
                if (line.startsWith(EXTENSION_PREFIX)) {
                    forEachExtension(line, (artifact, version, declaration) -> {
                        if (!extensionVersions_.containsKey(artifact) && declaration.contains("-SNAPSHOT")) {
                            problems.add(repo.getName() + " uses the snapshot extension '" + declaration +
                                         "', release it here or pin it to a released version first");
                        }
                    });
                }
            }
        }
        return problems;
    }

    /**
     * Walks the extension coordinates of a wrapper line, which can hold
     * several of them separated by commas, and whose version can carry a type.
     */
    private static void forEachExtension(String line, ExtensionConsumer consumer) {
        for (var declaration : line.substring(line.indexOf('=') + 1).split(",")) {
            var coordinate = declaration.trim().split(":");
            if (coordinate.length < 3 || !coordinate[0].trim().equals(GROUP)) {
                continue;
            }
            consumer.accept(coordinate[1].trim(), coordinate[2].trim().split("@")[0], declaration.trim());
        }
    }

    @FunctionalInterface
    private interface ExtensionConsumer {
        void accept(String artifact, String version, String declaration);
    }

    /**
     * A release built from sources that depend on a snapshot can't be rebuilt
     * from its tag once that snapshot moves on. The builds of bld and RIFE2
     * extend core's, so core's build sources count for every release, RIFE2's
     * only when RIFE2 is released. A member's own version and the bld
     * dependency of an extension are exempt, the phases set those.
     */
    private List<String> snapshotDependencyProblems() {
        var problems = new ArrayList<String>();
        var dirs = new ArrayList<File>();
        dirs.add(coreDir_);
        dirs.add(bldDir_);
        if (releaseRife2_) {
            dirs.add(rife2Dir_);
        }
        extensionVersions_.keySet().forEach(name -> dirs.add(new File(workspace_, name)));
        for (var dir : dirs) {
            var sources = new File(dir, "src/bld/java");
            if (!sources.exists()) {
                continue;
            }
            for (var name : FileUtils.getFileList(sources)) {
                if (!name.endsWith("Build.java")) {
                    continue;
                }
                var build_file = new File(sources, name);
                try {
                    var source = FileUtils.readString(build_file);
                    source = BLD_DEPENDENCY.matcher(source).replaceAll("$1$2");
                    source = EXTENSION_OWN_VERSION.matcher(source).replaceAll("$1$2");
                    for (var line : source.lines().toList()) {
                        if (SNAPSHOT_LITERAL.matcher(line).find()) {
                            problems.add(dir.getName() + " depends on a snapshot in " + build_file + ": " + line.trim() +
                                         ", pin it to a released version first");
                        }
                    }
                } catch (Exception e) {
                    problems.add("couldn't read " + build_file + ": " + e.getMessage());
                }
            }
        }
        return problems;
    }

    /**
     * Whether core carries its release tag on the checkout bld builds from,
     * which is what says a publish already got past it and will move RIFE2's
     * checkout itself.
     */
    private boolean coreAlreadyReleased(String coreCommit) {
        if (!releaseCore_ || coreCommit == null) {
            return false;
        }
        var git = git(coreDir_);
        return git.hasTag(coreVersion_) && coreCommit.equals(git.commitOf(coreVersion_));
    }

    /**
     * A simulated release never leaves this machine. Where it ends up isn't
     * up to this operation, it follows the {@code bld.repo.*} properties of
     * every repository and their remotes, so a run that claims to be a
     * simulation has to be shown to be one.
     */
    private List<String> simulationProblems() {
        var problems = new ArrayList<String>();
        if (!simulate_) {
            if (!releasesRepository_.isBlank() && staysOnThisMachine(releasesRepository_)) {
                problems.add("train.releases.repository stays on this machine while train.simulate is false");
            }
            if (!publishedRepository_.isBlank() && staysOnThisMachine(publishedRepository_)) {
                problems.add("train.published.repository stays on this machine while train.simulate is false");
            }
            return problems;
        }
        if (releasesRepository_.isBlank() || !staysOnThisMachine(releasesRepository_)) {
            problems.add("train.simulate is set but train.releases.repository isn't an absolute path or a URL on this machine");
        }
        if (!publishedRepository_.isBlank() && !staysOnThisMachine(publishedRepository_)) {
            problems.add("train.simulate is set but train.published.repository isn't an absolute path or a URL on this machine");
        }
        for (var dir : convergeDirs()) {
            if (!dir.exists()) {
                continue;
            }
            publicationDestinations(dir).forEach((name, location) -> {
                if (!staysOnThisMachine(location)) {
                    problems.add(dir.getName() + " publishes to " + name + " at " + location +
                                 (name.endsWith(JVM_PROPERTY_SUFFIX) ? ", drop that -D" :
                                  ", override it in its " + BuildExecutor.LOCAL_PROPERTIES));
                }
            });
            var push = git(dir).pushUrl();
            if (push == null) {
                problems.add("couldn't read where " + dir.getName() + " pushes, so it can't be shown to stay local");
            } else if (!staysOnThisMachine(push)) {
                problems.add(dir.getName() + " pushes to " + push + ", point it at a local clone");
            }
        }
        return problems;
    }

    /**
     * A destination that can't leave this machine: a path on the file system,
     * or a server on the loopback interface. The second one is what lets a
     * rehearsal publish over HTTP, which is the only way to exercise the
     * upload, its authentication and the checksums that go with it.
     */
    private static boolean staysOnThisMachine(String location) {
        if (new Repository(location).isLocal()) {
            return true;
        }
        try {
            var host = URI.create(location).getHost();
            return host != null &&
                   (host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]") || host.equals("::1"));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Where a repository would publish to: its own {@code local.properties}
     * on top of the ones declared for the user, plus every {@code bld.repo.}
     * property of this JVM, listed separately since a {@code -D} doesn't
     * reach the builds this launches. Every name declared anywhere counts,
     * since which of them a build file reaches for isn't visible from here.
     */
    private LinkedHashMap<String, String> publicationDestinations(File dir) {
        var destinations = new LinkedHashMap<String, String>();
        var user_properties = new File(BuildExecutor.BLD_USER_DIR, BuildExecutor.BLD_PROPERTIES);
        if (!user_properties.exists()) {
            user_properties = new File(BuildExecutor.RIFE2_USER_DIR, BuildExecutor.BLD_PROPERTIES);
        }
        for (var file : List.of(user_properties, new File(dir, BuildExecutor.LOCAL_PROPERTIES))) {
            if (!file.exists()) {
                continue;
            }
            var properties = new Properties();
            try (var reader = Files.newBufferedReader(file.toPath())) {
                properties.load(reader);
            } catch (IOException e) {
                destinations.put("the properties in " + file, "an unreadable file");
                continue;
            }
            for (var name : properties.stringPropertyNames()) {
                if (name.startsWith(Repository.PROPERTY_BLD_REPO_PREFIX) &&
                    !name.endsWith(Repository.PROPERTY_BLD_REPO_USERNAME_SUFFIX) &&
                    !name.endsWith(Repository.PROPERTY_BLD_REPO_PASSWORD_SUFFIX)) {
                    destinations.put(name.substring(Repository.PROPERTY_BLD_REPO_PREFIX.length()),
                        properties.getProperty(name).trim());
                }
            }
        }
        // a -D outranks both files in this JVM, and reaches the builds launched
        // from here only when it came in through the environment, so it is
        // checked next to the files rather than in place of them
        var system = System.getProperties();
        for (var name : system.stringPropertyNames()) {
            if (name.startsWith(Repository.PROPERTY_BLD_REPO_PREFIX) &&
                !name.endsWith(Repository.PROPERTY_BLD_REPO_USERNAME_SUFFIX) &&
                !name.endsWith(Repository.PROPERTY_BLD_REPO_PASSWORD_SUFFIX)) {
                destinations.put(name.substring(Repository.PROPERTY_BLD_REPO_PREFIX.length()) + JVM_PROPERTY_SUFFIX,
                    system.getProperty(name).trim());
            }
        }
        return destinations;
    }

    /**
     * Each project derives what it publishes from its own version file or
     * build source, while the tag and the confirmations use the configured
     * version. Publishing without a local phase before it would tag one
     * version and publish another, which is why every one of them is read
     * back here.
     */
    private void requireReady() {
        var problems = new ArrayList<String>();
        requireVersion(problems, bldDir_, BLD_VERSION_FILE, bldVersion_);
        if (releaseCore_) {
            requireVersion(problems, coreDir_, CORE_VERSION_FILE, coreVersion_);
        }
        if (releaseRife2_) {
            requireVersion(problems, rife2Dir_, RIFE2_VERSION_FILE, rife2Version_);
            // the blueprint isn't checked: it names the new RIFE2 only from
            // the moment that RIFE2 is public, which happens in this phase
        }
        extensionVersions_.forEach((name, version) -> {
            var dir = new File(workspace_, name);
            if (!dir.exists()) {
                problems.add("missing checkout: " + dir);
                return;
            }
            // an extension has no version file, its build source carries it
            var build_file = findBuildFile(dir);
            requireDeclaration(problems, build_file, EXTENSION_OWN_VERSION, version, "the version of " + name);
            requireDeclaration(problems, build_file, BLD_DEPENDENCY, bldVersion_, "the bld dependency of " + name);
        });
        if (!problems.isEmpty()) {
            problems.forEach(problem -> System.out.println("  ! " + problem));
            throw new IllegalStateException("Run './bld release-train local <shape>' first, that is what establishes these.");
        }
        // the wrappers still resolve from the local repository here, each is
        // checked when it is released
        System.out.println("   every version file and build source names what is being released");
    }

    private void requireVersion(List<String> problems, File dir, String versionFile, String expected) {
        var current = currentVersion(dir, versionFile);
        if (!expected.equals(current)) {
            problems.add(dir.getName() + " has " + versionFile + " " + current + " instead of " + expected);
        }
    }

    /**
     * Reads a version back out of the declaration that carries it, with the
     * same pattern that writes it. The version has to be in that declaration,
     * not merely somewhere in the file, since what a project publishes comes
     * from this one and any other could hold it by coincidence.
     */
    private void requireDeclaration(List<String> problems, File file, Pattern pattern, String version, String what) {
        try {
            var matcher = pattern.matcher(FileUtils.readString(file));
            if (!matcher.find()) {
                problems.add(file + " doesn't declare " + what + " any more");
                return;
            }
            var declaration = matcher.group().replaceAll("\\s+", "");
            if (!declaration.contains("version(" + versionLiteral(version) + ")")) {
                problems.add(file + " names " + what + " as " + matcher.group().trim() + " instead of " + version);
            }
        } catch (Exception e) {
            problems.add("couldn't read " + file + ": " + e.getMessage());
        }
    }

    private void requireReleasedVersionsOnly() {
        var problems = releasedVersionProblems();
        if (!problems.isEmpty()) {
            problems.forEach(problem -> System.out.println("  ! " + problem));
            throw new IllegalStateException("The release didn't fully converge.");
        }
        System.out.println("   every repository is on bld " + bldVersion_ + " with released versions only");
    }

    /**
     * Every wrapper names the bld and the extensions being released, and
     * nothing points at a local build.
     */
    private List<String> releasedVersionProblems() {
        return releasedVersionProblems(convergeDirs());
    }

    private List<String> releasedVersionProblems(List<File> dirs) {
        var problems = new ArrayList<String>();
        for (var name : extensionVersions_.keySet()) {
            if (!dirs.contains(new File(workspace_, name))) {
                continue;
            }
            var dir = new File(workspace_, name);
            if (!dir.exists()) {
                continue;
            }
            var build_file = findBuildFile(dir);
            try {
                if (!declaresLocalRepository(dir, build_file) &&
                    FileUtils.readString(build_file).contains(LOCAL_REPOSITORIES)) {
                    problems.add(name + " still resolves from the local repository in " + build_file);
                }
            } catch (Exception e) {
                problems.add("couldn't read " + build_file + ": " + e.getMessage());
            }
        }
        for (var repo : dirs) {
            for (var line : wrapperLines(repo)) {
                if (line.startsWith("bld.version=") && !line.equals("bld.version=" + bldVersion_)) {
                    problems.add(repo.getName() + " is not on bld " + bldVersion_ + ": " + line);
                } else if (line.startsWith("bld.repositories=") && line.contains("MAVEN_LOCAL")) {
                    problems.add(repo.getName() + " still resolves from the local repository");
                } else if (line.startsWith("bld.downloadLocation=") && !line.substring("bld.downloadLocation=".length()).isBlank()) {
                    problems.add(repo.getName() + " still has a download location override");
                } else if (line.startsWith(EXTENSION_PREFIX)) {
                    forEachExtension(line, (artifact, version, declaration) -> {
                        var released = extensionVersions_.get(artifact);
                        if (released == null) {
                            if (declaration.contains("-SNAPSHOT")) {
                                problems.add(repo.getName() + " uses the snapshot extension '" + declaration +
                                             "', release it here or pin it to a released version first");
                            }
                        } else if (!released.equals(version)) {
                            problems.add(repo.getName() + " uses " + artifact + " " + version +
                                         " instead of the " + released + " being released");
                        }
                    });
                }
            }
        }
        return problems;
    }

    private static final long PROMPT_INTERVAL_MS = 15 * 60 * 1000L;

    /**
     * Waits until the release can actually be resolved, pom and jar both,
     * since what is published next depends on it.
     */
    private void waitForRelease(String artifactId, String version)
    throws Exception {
        var repository = releasesRepository_.isBlank() ? Repository.MAVEN_CENTRAL : new Repository(releasesRepository_);
        var directory = repository.getArtifactLocation(GROUP, artifactId) + version + "/";
        var artifacts = List.of(artifactId + "-" + version + ".pom", artifactId + "-" + version + ".jar");

        if (repository.isLocal()) {
            for (var artifact : artifacts) {
                if (!new File(directory + artifact).exists()) {
                    throw new IllegalStateException("The publication of " + artifactId + " " + version +
                                                    " didn't arrive at " + directory + artifact + ".");
                }
            }
            System.out.println("   " + directory + " holds the release");
            return;
        }

        System.out.println("   waiting for " + directory);
        var waited = 0L;
        while (true) {
            var missing = artifacts.stream().filter(artifact -> !isAvailable(directory + artifact)).toList();
            if (missing.isEmpty()) {
                System.out.println("   resolvable");
                return;
            }
            if (waited >= PROMPT_INTERVAL_MS) {
                if (confirm("Still not resolvable after 15 minutes, keep waiting?")) {
                    waited = 0;
                } else if (!confirm("Continue without it?")) {
                    throw new IllegalStateException("Stopped waiting for " + artifactId + " " + version + ".");
                } else {
                    return;
                }
            }
            System.out.println("   still missing " + String.join(", ", missing) + ", checking again in 30 seconds");
            Thread.sleep(30_000);
            waited += 30_000;
        }
    }

    /**
     * Whether the artifact is there. The waits keep waiting for anything that
     * isn't a plain yes.
     */
    private boolean isAvailable(String url) {
        return availability(url) == Published.YES;
    }

    private Published availability(String url) {
        try {
            var connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("HEAD");
            connection.setConnectTimeout(10_000);
            connection.setReadTimeout(10_000);
            var code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                return Published.YES;
            }
            // only a plain "it isn't there" is an answer. A repository that
            // hides what it holds behind authentication, or that is having a
            // bad day, says nothing about whether the release exists
            if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                return Published.NO;
            }
            return Published.UNKNOWN;
        } catch (IOException e) {
            return Published.UNKNOWN;
        }
    }

    /*
     * The repositories
     */

    private List<File> allMemberDirs() {
        var dirs = new ArrayList<File>();
        dirs.add(coreDir_);
        dirs.add(bldDir_);
        dirs.add(rife2Dir_);
        extensionVersions_.keySet().forEach(name -> dirs.add(new File(workspace_, name)));
        return dirs;
    }

    private List<File> convergeDirs() {
        var dirs = allMemberDirs();
        followers_.forEach(follower -> dirs.add(new File(workspace_, follower)));
        return dirs;
    }

    private File localBldJar() {
        return new File(System.getProperty("user.home"),
            ".m2/repository/com/uwyn/rife2/bld/" + bldVersion_ + "/bld-" + bldVersion_ + ".jar");
    }

    /*
     * Editing versions, build sources and wrappers
     */

    private String currentVersion(File dir, String versionFile) {
        try {
            return FileUtils.readString(new File(dir, VERSION_DIRECTORY + versionFile)).trim();
        } catch (Exception e) {
            return "?";
        }
    }

    private void writeVersion(File dir, String versionFile, String version)
    throws Exception {
        FileUtils.writeString(version, new File(dir, VERSION_DIRECTORY + versionFile));
    }

    /**
     * Rewrites a version literal in a source file. The pattern matches
     * whatever version is in there, including the one being written, so not
     * matching means the declaration this is meant to update isn't there any
     * more, and a silent miss would release the old version.
     */
    private static void replaceVersion(File file, Pattern pattern, String version, String what)
    throws Exception {
        var source = FileUtils.readString(file);
        var matcher = pattern.matcher(source);
        if (!matcher.find()) {
            throw new IllegalStateException("Couldn't find " + what + " in " + file + ", update it manually.");
        }
        FileUtils.writeString(matcher.replaceFirst("$1" + versionLiteral(version) + "$2"), file);
    }

    private static File findBuildFile(File dir) {
        var sources = new File(dir, "src/bld/java");
        var matches = FileUtils.getFileList(sources).stream().filter(name -> name.endsWith("Build.java")).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException("Expected exactly one *Build.java in " + sources + ", found " + matches.size() + ".");
        }
        return new File(sources, matches.get(0));
    }

    private static File wrapperProperties(File dir) {
        return new File(dir, WRAPPER_PROPERTIES);
    }

    private static File wrapperBackup(File dir) {
        return new File(wrapperProperties(dir).getAbsolutePath() + WRAPPER_BACKUP_SUFFIX);
    }

    /**
     * The wrapper lines a release would commit: the backup a local phase took
     * when there is one, the file itself otherwise. {@link #finalizeWrapper}
     * puts that backup back before the release commit, so it is what a check
     * has to look at rather than whatever the file says now.
     */
    private List<String> committedWrapperLines(File dir) {
        var backup = wrapperBackup(dir);
        return backup.exists() ? readLines(backup) : wrapperLines(dir);
    }

    private List<String> wrapperLines(File dir) {
        return readLines(wrapperProperties(dir));
    }

    private static List<String> readLines(File wrapper) {
        if (!wrapper.exists()) {
            return List.of();
        }
        try {
            return List.of(FileUtils.readString(wrapper).split("\n"));
        } catch (Exception e) {
            throw new IllegalStateException("Couldn't read " + wrapper + ".", e);
        }
    }

    /**
     * Makes a repository build with the bld that was published locally,
     * keeping a copy of its wrapper properties for the coherent build later.
     */
    private void pointWrapperAtLocalBld(File dir)
    throws Exception {
        var wrapper = wrapperProperties(dir);
        if (!wrapperBackup(dir).exists()) {
            FileUtils.copy(wrapper, wrapperBackup(dir));
        }
        var local_repo = new File(System.getProperty("user.home"), ".m2/repository").getAbsolutePath();
        var local_location = "file://" + local_repo + "/com/uwyn/rife2/bld/${version}/";
        setWrapperProperty(dir, "bld.version", bldVersion_);
        setWrapperProperty(dir, "bld.downloadLocation", local_location);

        var content = FileUtils.readString(wrapper);
        if (!content.contains("bld.downloadLocation=")) {
            content = "bld.downloadLocation=" + local_location + "\n" + content;
        }
        if (!content.contains("MAVEN_LOCAL")) {
            content = content.replace("bld.repositories=", "bld.repositories=MAVEN_LOCAL,");
        }
        FileUtils.writeString(content, wrapper);
    }

    /**
     * Puts a repository on the released bld and the released extensions, and
     * regenerates its wrapper files with the released bld itself. Both the
     * distribution and the artifacts are only local at this point, so the
     * local repository stays in the wrapper until {@link #finalizeWrapper}
     * takes it out again.
     */
    private void makeWrapperCoherent(File repo)
    throws Exception {
        pointWrapperAtLocalBld(repo);
        pinExtensions(repo);
        // regenerating keeps everything but the bld version, so the local
        // repository survives it
        java(repo, localBldJar(), "upgrade");
    }

    /**
     * Leaves a repository the way it gets committed: on the released bld and
     * the released extensions, resolving from the repositories it declared
     * before the release started.
     */
    private void finalizeWrapper(File repo)
    throws Exception {
        var backup = wrapperBackup(repo);
        if (backup.exists()) {
            FileUtils.copy(backup, wrapperProperties(repo));
            backup.delete();
        }
        setWrapperProperty(repo, "bld.version", bldVersion_);
        pinExtensions(repo);
    }

    /**
     * Adds the local repository to a project's own repositories, unless the
     * project already declares it. What the commit holds decides that, so a
     * phase that stopped halfway and gets run again reaches the same answer.
     */
    private void addLocalRepository(File dir, File buildFile)
    throws Exception {
        if (declaresLocalRepository(dir, buildFile)) {
            return;
        }
        var source = FileUtils.readString(buildFile);
        if (source.contains(LOCAL_REPOSITORIES)) {
            return;
        }
        if (!source.contains(REPOSITORIES)) {
            throw new IllegalStateException("Couldn't find the repositories of " + buildFile + ", add " +
                                            "MAVEN_LOCAL to them manually for the local phase.");
        }
        FileUtils.writeString(source.replace(REPOSITORIES, LOCAL_REPOSITORIES), buildFile);
    }

    /**
     * Takes the local repository back out, leaving one the project declares
     * itself alone.
     */
    private void removeLocalRepository(File dir, File buildFile)
    throws Exception {
        if (declaresLocalRepository(dir, buildFile)) {
            return;
        }
        var source = FileUtils.readString(buildFile);
        if (source.contains(LOCAL_REPOSITORIES)) {
            FileUtils.writeString(source.replace(LOCAL_REPOSITORIES, REPOSITORIES), buildFile);
        }
    }

    /**
     * Whether the committed build file resolves from the local repository of
     * its own accord, which is what says the phase didn't put it there.
     */
    private boolean declaresLocalRepository(File dir, File buildFile) {
        var relative = dir.toPath().relativize(buildFile.toPath()).toString().replace(File.separatorChar, '/');
        var committed = git(dir).read("show", "HEAD:" + relative);
        return committed != null && committed.contains(LOCAL_REPOSITORIES);
    }

    private void setWrapperProperty(File dir, String key, String value)
    throws Exception {
        rewriteWrapper(dir, line -> line.startsWith(key + "=") ? key + "=" + value : line);
    }

    private void pinExtensions(File dir)
    throws Exception {
        rewriteWrapper(dir, this::pinExtensionLine);
    }

    private String pinExtensionLine(String line) {
        if (!line.startsWith(EXTENSION_PREFIX)) {
            return line;
        }
        // the value can hold several comma separated coordinates and a
        // coordinate can carry a type, so only the version itself is
        // replaced, for every extension of this release
        var result = line;
        for (var extension : extensionVersions_.entrySet()) {
            var coordinate = GROUP + ":" + extension.getKey() + ":";
            var start = result.indexOf(coordinate);
            if (start == -1) {
                continue;
            }
            var version_start = start + coordinate.length();
            var version_end = version_start;
            while (version_end < result.length() &&
                   result.charAt(version_end) != ',' &&
                   result.charAt(version_end) != '@' &&
                   !Character.isWhitespace(result.charAt(version_end))) {
                ++version_end;
            }
            result = result.substring(0, version_start) + extension.getValue() + result.substring(version_end);
        }
        return result;
    }

    private void rewriteWrapper(File dir, UnaryOperator<String> rewrite)
    throws Exception {
        var wrapper = wrapperProperties(dir);
        if (!wrapper.exists()) {
            return;
        }
        var lines = FileUtils.readString(wrapper).split("\n", -1);
        for (var i = 0; i < lines.length; i++) {
            lines[i] = rewrite.apply(lines[i]);
        }
        FileUtils.writeString(String.join("\n", lines), wrapper);
    }

    /**
     * Generates a project the way somebody starting out would and builds it
     * against the local publications, which is the only check that covers the
     * blueprint, the wrapper and the poms together.
     */
    private void smokeTest()
    throws Exception {
        var scratch = Files.createTempDirectory("release-train-smoke").toFile();
        try {
            // the base name has to be a plain java identifier. Creating ends
            // by downloading, which can't find the RIFE2 being released, but
            // the project is complete before that runs
            var created = java(scratch, releaseRife2_, localBldJar(),
                "create-rife2", "train.smoke", "smoketest", "Smoketest");
            var project_dir = new File(scratch, "smoketest");
            if (created != 0 && !project_dir.isDirectory()) {
                throw new IllegalStateException("The scratch project wasn't generated in " + scratch + ".");
            }

            // so that it resolves the local publications
            var build_file = findBuildFile(project_dir);
            var source = FileUtils.readString(build_file);
            FileUtils.writeString(source.replace("repositories = List.of(", "repositories = List.of(MAVEN_LOCAL, "), build_file);
            pointWrapperAtLocalBld(project_dir);

            bld(project_dir, "download", "compile", "test");
        } finally {
            FileUtils.deleteDirectory(scratch);
        }
    }

    /*
     * Running things
     */

    private void step(String description) {
        System.out.println("== " + description);
    }

    /**
     * A clean build of a repository, with its test suite when the train is
     * configured to run them. CI runs the suites on every push, so a release
     * doesn't have to run them again.
     */
    private void build(File dir)
    throws Exception {
        if (tests_) {
            bld(dir, "clean", "compile", "test");
        } else {
            bld(dir, "clean", "compile");
        }
    }

    /**
     * Runs a bld command through a repository's own wrapper, since each of
     * them builds with the bld its wrapper names, and during a release that
     * isn't the same one for all of them.
     */
    private void bld(File dir, String... commands)
    throws Exception {
        exec(dir, "bld " + String.join(" ", commands),
            command(List.of(new File(dir, "bld").getAbsolutePath()), commands));
    }

    private void java(File dir, File jar, String... commands)
    throws Exception {
        java(dir, false, jar, commands);
    }

    /**
     * Runs a jar, returning its exit status instead of failing on it when the
     * caller knows a failure is possible and can tell what it means.
     */
    private int java(File dir, boolean toleratingFailure, File jar, String... commands)
    throws Exception {
        var description = "java -jar " + jar.getName() + " " + String.join(" ", commands);
        var command = command(List.of("java", "-jar", jar.getAbsolutePath()), commands);
        if (!toleratingFailure) {
            exec(dir, description, command);
            return 0;
        }
        System.out.println("   [" + dir.getName() + "] " + description);
        return new ProcessBuilder(command).directory(dir).inheritIO().start().waitFor();
    }

    /**
     * Runs a command with its output going straight to the terminal, since
     * these are the builds and their output is what you watch.
     */
    private void exec(File dir, String description, List<String> command)
    throws Exception {
        System.out.println("   [" + dir.getName() + "] " + description);
        var status = new ProcessBuilder(command)
            .directory(dir)
            .inheritIO()
            .start()
            .waitFor();
        if (status != 0) {
            throw new IllegalStateException("Command failed with status " + status + " in " + dir + ": " + String.join(" ", command));
        }
    }

    private static List<String> command(List<String> executable, String... arguments) {
        var command = new ArrayList<>(executable);
        command.addAll(List.of(arguments));
        return command;
    }

    private boolean confirm(String question) {
        System.out.print(question + " [y/N] ");
        System.out.flush();
        var answer = new Scanner(System.in).nextLine().trim().toLowerCase();
        return answer.equals("y") || answer.equals("yes");
    }

    private Git git(File dir) {
        return new Git(dir);
    }

    /**
     * The git commands this needs, in one place, so that the phases read as
     * what they do instead of as process plumbing. A read returns null when
     * git fails or answers with nothing.
     */
    private final class Git {
        private final File dir_;

        private Git(File dir) {
            dir_ = dir;
        }

        private String read(String... arguments) {
            try {
                var process = new ProcessBuilder(command(List.of("git"), arguments))
                    .directory(dir_)
                    .redirectErrorStream(true)
                    .start();
                var output = new String(process.getInputStream().readAllBytes());
                if (process.waitFor() != 0) {
                    System.out.print(output);
                    return null;
                }
                return output;
            } catch (IOException | InterruptedException e) {
                throw new IllegalStateException("git " + String.join(" ", arguments) + " failed in " + dir_, e);
            }
        }

        private String readLine(String... arguments) {
            var output = read(arguments);
            return output == null || output.isBlank() ? null : output.trim();
        }

        private void run(String... arguments) {
            if (read(arguments) == null) {
                throw new IllegalStateException("git " + String.join(" ", arguments) + " failed in " + dir_);
            }
        }

        private String status() {
            // blank is a clean repository, null is git failing
            var status = read("status", "--porcelain");
            if (status == null) {
                throw new IllegalStateException("Couldn't read the status of " + dir_.getName() + ".");
            }
            return status;
        }

        private List<String> uncommittedPaths() {
            return status().lines()
                .filter(line -> line.length() > 3)
                .map(line -> line.substring(3).trim())
                .toList();
        }

        /**
         * Commits everything in the worktree, and says whether there was
         * anything to commit: a submodule whose content differs makes the
         * repository around it look modified while nothing stages, since its
         * recorded commit didn't move.
         */
        private boolean commitEverything(String message) {
            run("add", "-A");
            var staged = read("diff", "--cached", "--name-only");
            if (staged == null) {
                throw new IllegalStateException("git diff --cached failed in " + dir_ + ", so it can't be told " +
                                                "whether there is anything to commit.");
            }
            if (staged.isBlank()) {
                return false;
            }
            run("commit", "-m", message);
            return true;
        }

        private String head() {
            var head = readLine("rev-parse", "HEAD");
            if (head == null) {
                throw new IllegalStateException("Couldn't read the current commit of " + dir_.getName() + ".");
            }
            return head;
        }

        private String branch() {
            return readLine("symbolic-ref", "--short", "-q", "HEAD");
        }

        private String requireBranch() {
            var branch = branch();
            if (branch == null) {
                throw new IllegalStateException(dir_.getName() + " is on a detached HEAD, its commit can't be pushed.");
            }
            return branch;
        }

        private boolean hasTag(String tag) {
            return readLine("tag", "-l", tag) != null;
        }

        private String commitOf(String revision) {
            var commit = readLine("rev-list", "-n", "1", revision);
            if (commit == null) {
                throw new IllegalStateException("Couldn't resolve " + revision + " in " + dir_.getName() + ".");
            }
            return commit;
        }

        /**
         * The commit this version was released as, or null when its tag isn't
         * at the destination yet. A checkout that doesn't match the release it
         * finds there is a question for whoever is running this.
         */
        private String releasedCommit(String version) {
            var url = pushUrl();
            if (url == null) {
                throw new IllegalStateException("Couldn't read where " + dir_.getName() + " pushes, so it can't be told " +
                                                "whether " + version + " was released before.");
            }
            // these tags are annotated, so the plain ref names the tag object
            var found = read("ls-remote", url, "refs/tags/" + version, "refs/tags/" + version + "^{}");
            if (found == null) {
                throw new IllegalStateException("Couldn't read the tags of " + url + ", so it can't be told whether " +
                                                version + " was released before.");
            }
            var released = sha(found, "refs/tags/" + version + "^{}");
            if (released == null) {
                released = sha(found, "refs/tags/" + version);
            }
            if (released == null) {
                return null;
            }
            if (!head().equals(released) || !status().isBlank()) {
                throw new IllegalStateException(dir_.getName() + " " + version + " was released as " + released +
                                                ", but this checkout isn't on that commit or holds changes it doesn't. " +
                                                "Sort that out before continuing.");
            }
            return released;
        }

        private String sha(String lsRemoteOutput, String ref) {
            return lsRemoteOutput.lines()
                .filter(line -> line.endsWith("\t" + ref))
                .map(line -> line.split("\\s+")[0].trim())
                .findFirst()
                .orElse(null);
        }

        /**
         * The branch at the destination has to be reachable from what would be
         * pushed, or the push after the publication fails on a release that is
         * already out.
         */
        private void requireFastForward() {
            var branch = requireBranch();
            var url = pushUrl();
            var found = url == null ? null : read("ls-remote", url, "refs/heads/" + branch);
            if (found == null) {
                throw new IllegalStateException("Couldn't read " + branch + " at the destination of " + dir_.getName() +
                                                ", so it can't be told whether the release would push cleanly.");
            }
            var remote = sha(found, "refs/heads/" + branch);
            if (remote != null && read("merge-base", "--is-ancestor", remote, "HEAD") == null) {
                throw new IllegalStateException(branch + " at " + url + " is on " + remote + ", which isn't part of what " +
                                                "would be pushed. Bring them together before releasing, the push after " +
                                                "publishing would fail.");
            }
        }

        /**
         * The remote a plain {@code git push} would use, resolved the way git
         * resolves it, since the pushes name it explicitly.
         */
        private String pushRemote() {
            var branch = branch();
            if (branch != null) {
                for (var key : List.of("branch." + branch + ".pushRemote", "remote.pushDefault", "branch." + branch + ".remote")) {
                    var remote = readLine("config", "--get", key);
                    if (remote != null) {
                        return remote;
                    }
                }
            }
            return "origin";
        }

        private String pushUrl() {
            return readLine("remote", "get-url", "--push", pushRemote());
        }

        /**
         * The branch it is on, by name, so that no push configuration widens
         * this to other branches.
         */
        private void pushBranch() {
            run("push", pushRemote(), "HEAD:refs/heads/" + requireBranch());
        }

        /**
         * The release commit and its tag in one push, by name.
         */
        private void pushRelease(String tag) {
            run("push", "--atomic", pushRemote(), "HEAD:refs/heads/" + requireBranch(), "refs/tags/" + tag);
        }
    }
}
