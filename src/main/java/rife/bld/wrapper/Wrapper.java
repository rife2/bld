/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.wrapper;

import rife.tools.*;
import rife.tools.exceptions.FileUtilsErrorException;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.jar.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static rife.tools.FileUtils.JAR_FILE_PATTERN;
import static rife.tools.FileUtils.JAVA_FILE_PATTERN;

/**
 * Wrapper implementation for the build system that ensures the bld
 * jar gets downloaded locally and that the classpath for running the
 * build logic is properly setup.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class Wrapper {
    private enum LaunchMode {
        Cli,
        Build
    }

    public static final String BUILD_ARGUMENT = "--build";
    public static final String OFFLINE_ARGUMENT = "--offline";

    public static final String WRAPPER_PREFIX = "bld-wrapper";
    public static final String WRAPPER_PROPERTIES = WRAPPER_PREFIX + ".properties";

    static final String MAVEN_CENTRAL = "https://repo1.maven.org/maven2/";
    static final String CENTRAL_SNAPSHOTS = "https://central.sonatype.com/repository/maven-snapshots/";
    static final String DOWNLOAD_LOCATION = MAVEN_CENTRAL + "com/uwyn/rife2/bld/${version}/";
    static final String DOWNLOAD_LOCATION_SNAPSHOT = CENTRAL_SNAPSHOTS + "com/uwyn/rife2/bld/${version}/";
    static final String BLD_CACHE = "bld.cache";
    static final String BLD_FILENAME = "bld-${version}.jar";
    static final String BLD_SOURCES_FILENAME = "bld-${version}-sources.jar";
    static final String BLD_VERSION = "BLD_VERSION";
    static final String WRAPPER_JAR = WRAPPER_PREFIX + ".jar";
    static final String BLD_PROPERTY_VERSION = "bld.version";
    static final String RIFE2_PROPERTY_DOWNLOAD_LOCATION = "rife2.downloadLocation";
    static final String BLD_PROPERTY_DOWNLOAD_LOCATION = "bld.downloadLocation";
    static final String PROPERTY_REPOSITORIES = "bld.repositories";
    static final String PROPERTY_EXTENSION_PREFIX = "bld.extension";
    static final String PROPERTY_DOWNLOAD_EXTENSION_SOURCES = "bld.downloadExtensionSources";
    static final String PROPERTY_DOWNLOAD_EXTENSION_JAVADOC = "bld.downloadExtensionJavadoc";
    static final String PROPERTY_SOURCE_DIRECTORIES = "bld.sourceDirectories";
    static final String PROPERTY_JAVAC_OPTIONS = "bld.javacOptions";
    static final String PROPERTY_JAVA_OPTIONS = "bld.javaOptions";
    static final File BLD_USER_DIR = new File(System.getProperty("user.home"), ".bld");
    static final File DISTRIBUTIONS_DIR = new File(BLD_USER_DIR, "dist");
    static final Pattern META_DATA_LOCAL_COPY = Pattern.compile("<localCopy>\\s*true\\s*</localCopy>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    static final Pattern META_DATA_SNAPSHOT_VERSION = Pattern.compile("<snapshotVersion>.*?<value>([^<]+)</value>", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);
    static final Pattern OPTIONS_PATTERN = Pattern.compile("\"[^\"]+\"|\\S+");
    static final Pattern JVM_PROPERTY_PATTERN = Pattern.compile("-D(.+?)=(.*)");

    private static final Pattern JAR_EXCLUDE_SOURCES_PATTERN = Pattern.compile("^.*-sources\\.jar$", Pattern.CASE_INSENSITIVE);
    private static final Pattern JAR_EXCLUDE_JAVADOC_PATTERN = Pattern.compile("^.*-javadoc\\.jar$", Pattern.CASE_INSENSITIVE);
    private static final Pattern[] CLASSPATH_INCLUDED_JARS = new Pattern[]{JAR_FILE_PATTERN};
    private static final Pattern[] CLASSPATH_EXCLUDED_JARS = new Pattern[]{JAR_EXCLUDE_SOURCES_PATTERN, JAR_EXCLUDE_JAVADOC_PATTERN, Pattern.compile(WRAPPER_JAR)};

    private File currentDir_ = new File(System.getProperty("user.dir"));
    private LaunchMode launchMode_ = LaunchMode.Cli;
    private boolean offline_ = false;

    private final Properties jvmProperties_ = new Properties();
    private final Properties wrapperProperties_ = new Properties();
    private File wrapperPropertiesFile_ = null;
    private final Set<String> repositories_ = new LinkedHashSet<>();
    private final Set<String> extensions_ = new LinkedHashSet<>();
    private boolean downloadExtensionSources_ = false;
    private boolean downloadExtensionJavadoc_ = false;

    private final byte[] buffer_ = new byte[1024];
    private WrapperClassLoader classloader_;

    /**
     * Launches the wrapper.
     *
     * @param arguments the command line arguments to pass on to the build logic
     * @since 1.5
     */
    public static void main(String[] arguments) {
        System.exit(new Wrapper().installAndLaunch(new ArrayList<>(Arrays.asList(arguments))));
    }

    /**
     * Creates the files required to use the wrapper.
     *
     * @param destinationDirectory the directory to put those files in
     * @param version              the bld version they should be using
     * @throws IOException when an error occurred during the creation of the wrapper files
     * @since 1.5
     */
    public void createWrapperFiles(File destinationDirectory, String version)
    throws IOException {
        createWrapperProperties(destinationDirectory, version);
        createWrapperJar(destinationDirectory);
    }

    private static final Pattern RIFE2_JAR_PATTERN = Pattern.compile("/\\.rife2/dist/rife2-[^\"/!]+(?<!sources)\\.jar");
    private static final Pattern RIFE2_SOURCES_JAR_PATTERN = Pattern.compile("/\\.rife2/dist/rife2-[^\"/!]+-sources\\.jar");
    private static final Pattern RIFE2_PROPERTY_VERSION_PATTERN = Pattern.compile(".*rife2\\.version.*");
    private static final Pattern BLD_JAR_PATTERN = Pattern.compile("/\\.bld/dist/bld-[^\"/!]+(?<!sources)\\.jar");
    private static final Pattern BLD_SOURCES_JAR_PATTERN = Pattern.compile("/\\.bld/dist/bld-[^\"/!]+-sources\\.jar");
    private static final Pattern BLD_PROPERTY_VERSION_PATTERN = Pattern.compile(".*bld\\.version.*");
    private static final Pattern JAR_DIRECTORY_LIB_COMPILE_RECURSIVE_PATTERN = Pattern.compile("<jarDirectory\\s+url=\"file://\\$PROJECT_DIR\\$/lib/compile\"\\s+recursive=\"false\"");
    private static final Pattern JAR_DIRECTORY_LIB_PROVIDED_RECURSIVE_PATTERN = Pattern.compile("<jarDirectory\\s+url=\"file://\\$PROJECT_DIR\\$/lib/provided\"\\s+recursive=\"false\"");
    private static final Pattern JAR_DIRECTORY_LIB_RUNTIME_RECURSIVE_PATTERN = Pattern.compile("<jarDirectory\\s+url=\"file://\\$PROJECT_DIR\\$/lib/runtime\"\\s+recursive=\"false\"");
    private static final Pattern JAR_DIRECTORY_LIB_TEST_RECURSIVE_PATTERN = Pattern.compile("<jarDirectory\\s+url=\"file://\\$PROJECT_DIR\\$/lib/test\"\\s+recursive=\"false\"");

    /**
     * Upgraded the IDEA bld files that were generated with a previous version.
     *
     * @param destinationDirectory the directory with the IDEA files
     * @param version              the bld version they should be using
     * @throws IOException when an error occurred during the upgrade of the IDEA files
     * @since 1.5.2
     */
    public void upgradeIdeaBldLibrary(File destinationDirectory, String version)
    throws IOException {
        var libraries_bld = new File(destinationDirectory, Path.of("libraries", "bld.xml").toString());
        if (libraries_bld.exists()) {
            try {
                var content = FileUtils.readString(libraries_bld);
                content = BLD_JAR_PATTERN.matcher(content).replaceAll("/.bld/dist/bld-" + version + ".jar");
                content = BLD_SOURCES_JAR_PATTERN.matcher(content).replaceAll("/.bld/dist/bld-" + version + "-sources.jar");
                content = RIFE2_JAR_PATTERN.matcher(content).replaceAll("/.bld/dist/bld-" + version + ".jar");
                content = RIFE2_SOURCES_JAR_PATTERN.matcher(content).replaceAll("/.bld/dist/bld-" + version + "-sources.jar");
                FileUtils.writeString(content, libraries_bld);
            } catch (FileUtilsErrorException e) {
                throw new IOException(e);
            }
        }

        var libraries_compile = new File(destinationDirectory, Path.of("libraries", "compile.xml").toString());
        if (libraries_compile.exists()) {
            try {
                var content = FileUtils.readString(libraries_compile);
                content = JAR_DIRECTORY_LIB_COMPILE_RECURSIVE_PATTERN.matcher(content).replaceAll("<jarDirectory url=\"file://\\$PROJECT_DIR\\$/lib/compile\" recursive=\"true\"");
                content = JAR_DIRECTORY_LIB_PROVIDED_RECURSIVE_PATTERN.matcher(content).replaceAll("<jarDirectory url=\"file://\\$PROJECT_DIR\\$/lib/provided\" recursive=\"true\"");
                FileUtils.writeString(content, libraries_compile);
            } catch (FileUtilsErrorException e) {
                throw new IOException(e);
            }
        }

        var libraries_runtime = new File(destinationDirectory, Path.of("libraries", "runtime.xml").toString());
        if (libraries_runtime.exists()) {
            try {
                var content = FileUtils.readString(libraries_runtime);
                content = JAR_DIRECTORY_LIB_RUNTIME_RECURSIVE_PATTERN.matcher(content).replaceAll("<jarDirectory url=\"file://\\$PROJECT_DIR\\$/lib/runtime\" recursive=\"true\"");
                FileUtils.writeString(content, libraries_runtime);
            } catch (FileUtilsErrorException e) {
                throw new IOException(e);
            }
        }

        var libraries_test = new File(destinationDirectory, Path.of("libraries", "test.xml").toString());
        if (libraries_test.exists()) {
            try {
                var content = FileUtils.readString(libraries_test);
                content = JAR_DIRECTORY_LIB_PROVIDED_RECURSIVE_PATTERN.matcher(content).replaceAll("<jarDirectory url=\"file://\\$PROJECT_DIR\\$/lib/provided\" recursive=\"true\"");
                content = JAR_DIRECTORY_LIB_TEST_RECURSIVE_PATTERN.matcher(content).replaceAll("<jarDirectory url=\"file://\\$PROJECT_DIR\\$/lib/test\" recursive=\"true\"");
                FileUtils.writeString(content, libraries_test);
            } catch (FileUtilsErrorException e) {
                throw new IOException(e);
            }
        }
    }

    /**
     * Upgraded the vscode settings files that were generated with a previous version.
     *
     * @param destinationDirectory the directory with the vscode files
     * @param version              the bld version they should be using
     * @throws IOException when an error occurred during the upgrade of the IDEA files
     * @since 1.5.6
     */
    public void upgradeVscodeSettings(File destinationDirectory, String version)
    throws IOException {
        var file = new File(destinationDirectory, Path.of("settings.json").toString());
        if (file.exists()) {
            try {
                var content = FileUtils.readString(file);
                content = BLD_JAR_PATTERN.matcher(content).replaceAll("/.bld/dist/bld-" + version + ".jar");
                content = RIFE2_JAR_PATTERN.matcher(content).replaceAll("/.bld/dist/bld-" + version + ".jar");
                FileUtils.writeString(content, file);
            } catch (FileUtilsErrorException e) {
                throw new IOException(e);
            }
        }
    }

    private void createWrapperProperties(File destinationDirectory, String version)
    throws IOException {
        var file = new File(destinationDirectory, WRAPPER_PROPERTIES);
        if (file.exists()) {
            try {
                var contents = FileUtils.readString(file);
                contents = contents.replace(RIFE2_PROPERTY_DOWNLOAD_LOCATION, BLD_PROPERTY_DOWNLOAD_LOCATION);
                contents = BLD_PROPERTY_VERSION_PATTERN.matcher(contents).replaceAll(BLD_PROPERTY_VERSION + "=" + version);
                contents = RIFE2_PROPERTY_VERSION_PATTERN.matcher(contents).replaceAll(BLD_PROPERTY_VERSION + "=" + version);
                FileUtils.writeString(contents, file);
            } catch (FileUtilsErrorException e) {
                throw new IOException(e);
            }
        } else {
            var properties_blueprint = """
                bld.downloadExtensionJavadoc=false
                bld.downloadExtensionSources=true
                bld.downloadLocation=
                bld.extensions=
                bld.javaOptions=
                bld.javacOptions=
                bld.repositories=MAVEN_CENTRAL,RIFE2_RELEASES
                bld.sourceDirectories=
                bld.version=${version}
                """
                .replace("${version}", version);

            Files.createDirectories(file.getAbsoluteFile().toPath().getParent());
            Files.deleteIfExists(file.toPath());
            try {
                FileUtils.writeString(properties_blueprint, file);
            } catch (FileUtilsErrorException e) {
                throw new IOException(e);
            }
        }
    }

    private void createWrapperJar(File destinationDirectory)
    throws IOException {
        var manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, getClass().getName());

        try (var jar = new JarOutputStream(new FileOutputStream(new File(destinationDirectory, WRAPPER_JAR)), manifest)) {
            addClassToJar(jar, Wrapper.class);
            addClassToJar(jar, Wrapper.LaunchMode.class);
            addClassToJar(jar, WrapperClassLoader.class);
            addClassToJar(jar, FileUtils.class);
            addClassToJar(jar, FileUtilsErrorException.class);
            addClassToJar(jar, InnerClassException.class);
            addFileToJar(jar, BLD_VERSION);
            jar.flush();
        }
    }

    /**
     * Sets the current directory for the wrapper.
     *
     * @param dir the directory to set as the current directory
     * @since 2.0
     */
    public void currentDir(File dir) {
        currentDir_ = dir;
    }

    /**
     * Initializes the properties set for the wrapper.
     *
     * @param version the bld version they should be using
     * @throws IOException when an error occurred during the creation of the wrapper files
     * @since 2.0
     */
    public void initWrapperProperties(String version)
    throws IOException {
        // ensure required properties are available
        wrapperProperties_.put(PROPERTY_REPOSITORIES, MAVEN_CENTRAL);
        wrapperProperties_.put(BLD_PROPERTY_VERSION, version);

        // retrieve properties from possible locations
        var config = libBldDirectory(WRAPPER_PROPERTIES);
        if (config.exists()) {
            wrapperPropertiesFile_ = config;
            wrapperProperties_.load(new FileReader(config));
        } else {
            config = libDirectory(WRAPPER_PROPERTIES);
            if (config.exists()) {
                wrapperPropertiesFile_ = config;
                wrapperProperties_.load(new FileReader(config));
            }
        }

        // extract repositories
        if (wrapperProperties_.containsKey(PROPERTY_REPOSITORIES)) {
            for (var repository : wrapperProperties_.getProperty(PROPERTY_REPOSITORIES).split(",")) {
                repository = repository.trim();
                if (!repository.isBlank()) {
                    repositories_.add(repository);
                }
            }
        }
        // extract wrapper extension specifications
        for (var property : wrapperProperties_.entrySet()) {
            if (property.getKey().toString().startsWith(PROPERTY_EXTENSION_PREFIX)) {
                for (var extension : property.getValue().toString().split(",")) {
                    extension = extension.trim();
                    if (!extension.isBlank()) {
                        extensions_.add(extension);
                    }
                }
            }
        }
        // check whether extension sources or javadoc should be downloaded
        downloadExtensionSources_ = Boolean.parseBoolean(wrapperProperties_.getProperty(PROPERTY_DOWNLOAD_EXTENSION_SOURCES, "false"));
        downloadExtensionJavadoc_ = Boolean.parseBoolean(wrapperProperties_.getProperty(PROPERTY_DOWNLOAD_EXTENSION_JAVADOC, "false"));
    }

    /**
     * Returns the set of extension repositories.
     *
     * @return the set of extension repositories
     * @since 2.0
     */
    public Set<String> repositories() {
        return repositories_;
    }

    /**
     * Returns the set of extensions.
     *
     * @return the set of extensions
     * @since 2.0
     */
    public Set<String> extensions() {
        return extensions_;
    }

    /**
     * Returns the wrapper properties.
     *
     * @return the wrapper properties
     * @since 2.0
     */
    public Properties wrapperProperties() {
        return wrapperProperties_;
    }

    /**
     * Returns the wrapper properties file.
     *
     * @return the wrapper properties file
     * @since 2.0
     */
    public File wrapperPropertiesFile() {
        return wrapperPropertiesFile_;
    }

    private void addClassToJar(JarOutputStream jar, Class klass)
    throws IOException {
        addFileToJar(jar, klass.getName().replace('.', '/') + ".class");
    }

    private void addFileToJar(JarOutputStream jar, String name)
    throws IOException {
        var resource = getClass().getResource("/" + name);
        if (resource == null) {
            throw new IOException("Couldn't find resource '" + name + "'");
        }

        InputStream stream = null;
        try {
            var connection = resource.openConnection();
            connection.setUseCaches(false);
            stream = connection.getInputStream();
            var entry = new JarEntry(name);
            jar.putNextEntry(entry);

            try (var in = new BufferedInputStream(stream)) {
                int count;
                while ((count = in.read(buffer_)) != -1) {
                    jar.write(buffer_, 0, count);
                }
                jar.closeEntry();
            }
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    // couldn't close stream since it probably already has been
                    // closed after an exception
                    // proceed without reporting an error message.
                }
            }
        }
    }

    private String getVersion()
    throws IOException {
        try (InputStream in = getClass().getResource("/" + BLD_VERSION).openStream()) {
            byte[] bytes = in.readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }

    private int installAndLaunch(List<String> arguments) {
        if (!arguments.isEmpty()) {
            File current_file;
            try {
                current_file = new File(arguments.remove(0)).getCanonicalFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            currentDir_ = new File(current_file.getParent());

            if (!arguments.isEmpty() &&
                BUILD_ARGUMENT.equals(arguments.get(0))) {
                launchMode_ = LaunchMode.Build;
                arguments.remove(0);
            }

            // first argument after the --build argument is the main build file
            // arguments.get(0)

            // check if the next argument enables offline mode
            if (arguments.size() >= 2 &&
                OFFLINE_ARGUMENT.equals(arguments.get(1))) {
                offline_ = true;
            }
        }

        try {
            extractJvmProperties(arguments);
            initWrapperProperties(getVersion());
            var distribution = installDistribution();
            return launchMain(distribution, arguments);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void extractJvmProperties(List<String> arguments) {
        for (var arg : arguments) {
            var matcher = JVM_PROPERTY_PATTERN.matcher(arg);
            if (matcher.matches()) {
                jvmProperties_.put(matcher.group(1), matcher.group(2));
            }
        }
    }

    private File buildBldDirectory() {
        return Path.of(currentDir_.getAbsolutePath(), "build", "bld").toFile();
    }

    private File srcBldJavaDirectory() {
        return Path.of(currentDir_.getAbsolutePath(), "src", "bld", "java").toFile();
    }

    private File srcBldResourcesDirectory() {
        return Path.of(currentDir_.getAbsolutePath(), "src", "bld", "resources").toFile();
    }

    private File libBldDirectory() {
        return Path.of(currentDir_.getAbsolutePath(), "lib", "bld").toFile();
    }

    private File libDirectory(String path) {
        return Path.of(currentDir_.getAbsolutePath(), "..", "lib", path).toFile();
    }

    private File libBldDirectory(String path) {
        return Path.of(currentDir_.getAbsolutePath(), "lib", "bld", path).toFile();
    }

    private String getWrapperVersion()
    throws IOException {
        return wrapperProperties_.getProperty(BLD_PROPERTY_VERSION, getVersion());
    }

    private boolean isSnapshot(String version) {
        return version.endsWith("-SNAPSHOT");
    }

    private String getWrapperDownloadLocation(String version) {
        var default_location = DOWNLOAD_LOCATION;
        if (isSnapshot(version)) {
            default_location = DOWNLOAD_LOCATION_SNAPSHOT;
        }
        var location = wrapperProperties_.getProperty(BLD_PROPERTY_DOWNLOAD_LOCATION, default_location);
        if (location == null || location.trim().isBlank()) {
            location = default_location;
        }
        return replaceVersion(location, version);
    }

    private String downloadUrl(String version, String fileName) {
        var location = getWrapperDownloadLocation(version);
        var result = new StringBuilder(location);
        if (!location.endsWith("/")) {
            result.append('/');
        }
        result.append(fileName);
        return result.toString();
    }

    private String bldFileName(String version) {
        return replaceVersion(BLD_FILENAME, version);
    }

    private String bldSourcesFileName(String version) {
        return replaceVersion(BLD_SOURCES_FILENAME, version);
    }

    private String replaceVersion(String text, String version) {
        return text.replaceAll("\\$\\{version}", version);
    }

    private File installDistribution()
    throws IOException {
        try {
            Files.createDirectories(DISTRIBUTIONS_DIR.toPath());
        } catch (IOException e) {
            System.err.println("Failed to create distribution directory " + DISTRIBUTIONS_DIR.getAbsolutePath() + ".");
            throw e;
        }

        String version;
        try {
            version = getWrapperVersion();
        } catch (IOException e) {
            System.err.println("Failed to retrieve wrapper version number.");
            throw e;
        }

        var download_version = version;
        var is_snapshot = isSnapshot(version);
        var is_local = false;
        if (offline_) {
            System.out.println("Offline mode: no artifacts will be checked nor downloaded");
            System.out.flush();
        }
        else {
            if (is_snapshot) {
                var meta_data = "";
                try {
                    meta_data = readString(version, new URL(downloadUrl(version, "maven-metadata.xml")));
                } catch (IOException e) {
                    try {
                        meta_data = readString(version, new URL(downloadUrl(version, "maven-metadata-local.xml")));
                    } catch (IOException e2) {
                        throw e;
                    }
                }
                var local_matcher = META_DATA_LOCAL_COPY.matcher(meta_data);
                is_local = local_matcher.find();
                if (!is_local) {
                    var version_matcher = META_DATA_SNAPSHOT_VERSION.matcher(meta_data);
                    if (version_matcher.find()) {
                        download_version = version_matcher.group(1);
                    }
                }
            }
        }

        var distribution_file = new File(DISTRIBUTIONS_DIR, bldFileName(version));
        var distribution_sources_file = new File(DISTRIBUTIONS_DIR, bldSourcesFileName(version));

        if (!offline_) {
            // if this is a snapshot and the distribution file exists,
            // ensure that it's the latest by comparing hashes
            if (is_snapshot && distribution_file.exists()) {
                boolean delete_distribution_files = is_local;
                if (!delete_distribution_files) {
                    var download_md5 = readString(version, new URL(downloadUrl(version, bldFileName(download_version)) + ".md5"));
                    try {
                        var digest = MessageDigest.getInstance("MD5");
                        digest.update(FileUtils.readBytes(distribution_file));
                        if (!download_md5.equals(encodeHexLower(digest.digest()))) {
                            delete_distribution_files = true;
                        }
                    } catch (NoSuchAlgorithmException ignore) {
                    }
                }

                if (delete_distribution_files) {
                    distribution_file.delete();
                    if (distribution_sources_file.exists()) {
                        distribution_sources_file.delete();
                    }
                }

            }

            // download distribution jars if necessary
            if (!distribution_file.exists()) {
                downloadDistribution(distribution_file, downloadUrl(version, bldFileName(download_version)));
            }
            if (!distribution_sources_file.exists()) {
                try {
                    downloadDistribution(distribution_sources_file, downloadUrl(version, bldSourcesFileName(download_version)));
                } catch (IOException e) {
                    // this is not critical, ignore
                }
            }
        }

        // find the wrapper classloader in the hierarchy and add the bld jar to it
        classloader_ = new WrapperClassLoader();
        classloader_.add(distribution_file.toURI().toURL());

        return distribution_file;
    }

    private void downloadDistribution(File file, String downloadUrl)
    throws IOException {
        try {
            System.out.print("Downloading: " + downloadUrl + " ... ");
            System.out.flush();
            var url = new URL(downloadUrl);
            var readableByteChannel = Channels.newChannel(url.openStream());
            try (var fileOutputStream = new FileOutputStream(file)) {
                var fileChannel = fileOutputStream.getChannel();
                fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);

                System.out.print("done");
            }
        } catch (FileNotFoundException e) {
            System.err.println("not found");
            System.err.println("Failed to download file " + file + ".");
            throw e;
        } catch (IOException e) {
            System.err.println("error");
            System.err.println("Failed to download file " + file + " due to I/O issue.");
            Files.deleteIfExists(file.toPath());
            throw e;
        } finally {
            System.out.println();
        }
    }

    private void resolveExtensions() {
        if (null == classloader_ ||
            null == wrapperPropertiesFile_ ||
            offline_) {
            return;
        }

        try {
            var resolver_class = classloader_.loadClass("rife.bld.wrapper.WrapperExtensionResolver");
            var constructor = resolver_class.getConstructor(File.class, File.class, Properties.class, Properties.class, Collection.class, Collection.class, boolean.class, boolean.class);
            var update_method = resolver_class.getMethod("updateExtensions");
            var resolver = constructor.newInstance(currentDir_, libBldDirectory(),
                jvmProperties_, wrapperProperties_,
                repositories_, extensions_,
                downloadExtensionSources_, downloadExtensionJavadoc_);
            update_method.invoke(resolver);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e.getCause());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int launchMain(File jarFile, List<String> arguments)
    throws IOException, InterruptedException, FileUtilsErrorException {
        if (launchMode_ == LaunchMode.Cli) {
            return launchMainCli(jarFile, arguments);
        }
        return launchMainBuild(jarFile, arguments);
    }

    private int launchMainCli(File jarFile, List<String> arguments)
    throws IOException, InterruptedException {
        var args = new ArrayList<String>();
        args.add(findJavaExecutable());
        includeJvmProperties(arguments, args);

        args.add("-cp");
        args.add(jarFile.getAbsolutePath());

        args.add("-jar");
        args.add(jarFile.getAbsolutePath());

        args.addAll(bldJavaOptions());
        args.addAll(arguments);

        var process_builder = new ProcessBuilder(args);
        process_builder.inheritIO();
        var process = process_builder.start();
        return process.waitFor();
    }

    private int launchMainBuild(File jarFile, List<String> arguments)
    throws IOException, InterruptedException {
        resolveExtensions();

        var build_bld_dir = buildBldDirectory();
        if (build_bld_dir.exists()) {
            FileUtils.deleteDirectory(buildBldDirectory());
        }
        buildBldDirectory().mkdirs();

        var bld_classpath = bldClasspathJars();
        bld_classpath.add(jarFile);
        bld_classpath.add(buildBldDirectory());
        bld_classpath.add(srcBldResourcesDirectory());
        var classpath = FileUtils.joinPaths(FileUtils.combineToAbsolutePaths(bld_classpath));

        var compiler = ToolProvider.getSystemJavaCompiler();
        try (var file_manager = compiler.getStandardFileManager(null, null, null)) {
            var compilation_units = file_manager.getJavaFileObjectsFromFiles(bldSourceFiles());
            var diagnostics = new DiagnosticCollector<JavaFileObject>();
            var options = new ArrayList<>(List.of("-d", buildBldDirectory().getAbsolutePath(), "-cp", classpath));
            options.addAll(bldJavacOptions());
            var compilation_task = compiler.getTask(null, file_manager, diagnostics, options, null, compilation_units);
            if (!compilation_task.call()) {
                if (!diagnostics.getDiagnostics().isEmpty()) {
                    for (var diagnostic : diagnostics.getDiagnostics()) {
                        System.err.print(diagnostic.toString() + System.lineSeparator());
                    }

                    return 1;
                }
            }
        }

        var java_args = new ArrayList<String>();
        java_args.add(findJavaExecutable());
        includeJvmProperties(arguments, java_args);

        java_args.add("-cp");
        java_args.add(classpath);

        java_args.addAll(bldJavaOptions());
        java_args.addAll(arguments);

        var process_builder = new ProcessBuilder(java_args);
        process_builder.directory(currentDir_);
        process_builder.inheritIO();
        var process = process_builder.start();

        return process.waitFor();
    }

    private static String findJavaExecutable() {
        var executable = System.getProperty("os.name").toLowerCase().contains("win") ? "java.exe" : "java";
        var java_home = System.getProperty("java.home");
        if (null == java_home) {
            return executable;
        }
        return java_home + File.separator + "bin" + File.separator + executable;
    }

    private static void includeJvmProperties(List<String> arguments, List<String> javaArgs) {
        var i = arguments.iterator();
        while (i.hasNext()) {
            var arg = i.next();
            if (JVM_PROPERTY_PATTERN.matcher(arg).matches()) {
                javaArgs.add(arg);
                i.remove();
            }
        }
    }

    private List<File> bldClasspathJars() {
        // detect the jar files in the compile lib directory
        var dir_abs = libBldDirectory().getAbsoluteFile();
        var jar_files = FileUtils.getFileList(dir_abs, CLASSPATH_INCLUDED_JARS, CLASSPATH_EXCLUDED_JARS);

        // build the compilation classpath
        return new ArrayList<>(jar_files.stream().map(file -> new File(dir_abs, file)).toList());
    }

    public List<File> bldSourceFiles() {
        var source_directories = new ArrayList<File>();
        source_directories.add(srcBldJavaDirectory().getAbsoluteFile());

        // extract wrapper source directories specifications
        if (wrapperProperties_.containsKey(PROPERTY_SOURCE_DIRECTORIES)) {
            var source_directories_property = wrapperProperties_.get(PROPERTY_SOURCE_DIRECTORIES).toString();
            for (var source_directory : source_directories_property.split(",")) {
                source_directory = source_directory.trim();
                if (!source_directory.isBlank()) {
                    source_directories.add(new File(source_directory).getAbsoluteFile());
                }
            }
        }

        // get all the bld java sources
        var source_files = new ArrayList<File>();
        for (var source_directory : source_directories) {
            source_files.addAll(FileUtils.getFileList(source_directory, JAVA_FILE_PATTERN, null)
                .stream().map(file -> new File(source_directory, file)).toList());
        }
        return source_files;
    }

    public List<String> bldJavacOptions() {
        if (!wrapperProperties_.containsKey(PROPERTY_JAVAC_OPTIONS)) {
            return Collections.emptyList();
        }

        return OPTIONS_PATTERN.matcher(wrapperProperties_.get(PROPERTY_JAVAC_OPTIONS).toString())
            .results()
            .map(MatchResult::group)
            .toList();
    }

    public List<String> bldJavaOptions() {
        if (!wrapperProperties_.containsKey(PROPERTY_JAVA_OPTIONS)) {
            return Collections.emptyList();
        }

        return OPTIONS_PATTERN.matcher(wrapperProperties_.get(PROPERTY_JAVA_OPTIONS).toString())
            .results()
            .map(MatchResult::group)
            .toList();
    }

    private String readString(String version, URL url)
    throws IOException {
        var connection = url.openConnection();
        connection.setUseCaches(false);
        connection.setRequestProperty("User-Agent", "bld " + version);
        try (var in = connection.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public static final char[] HEX_DIGITS_LOWER = "0123456789abcdef".toCharArray();

    public static void appendHexDigitLower(StringBuilder out, int number) {
        out.append(HEX_DIGITS_LOWER[number & 0x0F]);
    }

    public static String encodeHexLower(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        var out = new StringBuilder();
        for (var b : bytes) {
            appendHexDigitLower(out, b >> 4);
            appendHexDigitLower(out, b);
        }
        return out.toString();
    }
}
