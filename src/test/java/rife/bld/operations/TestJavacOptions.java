/*
 * Copyright 2025 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rife.bld.operations.JavacOptions.XLintKey;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.operations.JavacOptions.XLintKey.*;

/**
 * Unit tests for JavacOptions class
 */
class TestJavacOptions {
    private JavacOptions options;

    @BeforeEach
    void setUp() {
        options = new JavacOptions();
    }

    @Nested
    class AddExportsTests {
        @Test
        void testAddExportsMethodChaining() {
            options.addExports("mod1/pkg1=mod2")
                    .addExports("mod3/pkg3=ALL-UNNAMED")
                    .parameters();

            assertEquals(5, options.size());
            assertTrue(options.contains("--add-exports"));
            assertTrue(options.contains("-parameters"));
        }

        @Test
        void testAddExportsMultipleCalls() {
            options.addExports("module1/package1=module2")
                    .addExports("module3/package3=ALL-UNNAMED");

            assertEquals(4, options.size());
            assertEquals("--add-exports", options.get(0));
            assertEquals("module1/package1=module2", options.get(1));
            assertEquals("--add-exports", options.get(2));
            assertEquals("module3/package3=ALL-UNNAMED", options.get(3));
        }

        @Test
        void testAddExportsReturnsThis() {
            var result = options.addExports("module1/package1=module2");
            assertSame(options, result);
        }

        @Test
        void testAddExportsWithComplexModulePaths() {
            options.addExports(
                    "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                    "jdk.compiler/com.sun.tools.javac.tree=mymodule"
            );

            assertEquals(2, options.size());
            assertTrue(options.get(1).contains("jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"));
            assertTrue(options.get(1).contains("jdk.compiler/com.sun.tools.javac.tree=mymodule"));
        }

        @Test
        void testAddExportsWithEmptyList() {
            options.addExports(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testAddExportsWithEmptyVarargs() {
            options.addExports();

            assertEquals(0, options.size());
        }

        @Test
        void testAddExportsWithList() {
            var modules = Arrays.asList(
                    "mod1/pkg1=mod2",
                    "mod3/pkg3=ALL-UNNAMED",
                    "mod4/pkg4=mod5"
            );
            options.addExports(modules);

            assertEquals(2, options.size());
            assertEquals("--add-exports", options.get(0));
            assertEquals("mod1/pkg1=mod2,mod3/pkg3=ALL-UNNAMED,mod4/pkg4=mod5", options.get(1));
        }

        @Test
        void testAddExportsWithSingleModule() {
            options.addExports("java.base/sun.security.util=ALL-UNNAMED");

            assertEquals(2, options.size());
            assertEquals("--add-exports", options.get(0));
            assertEquals("java.base/sun.security.util=ALL-UNNAMED", options.get(1));
        }

        @Test
        void testAddExportsWithVarargs() {
            options.addExports("module1/package1=module2", "module3/package3=ALL-UNNAMED");

            assertEquals(2, options.size());
            assertEquals("--add-exports", options.get(0));
            assertEquals("module1/package1=module2,module3/package3=ALL-UNNAMED", options.get(1));
        }
    }

    @Nested
    class AddReadsTests {
        @Test
        void testAddReadsMethodChaining() {
            options.addReads("mod1=mod2")
                    .addReads("mod3=mod4")
                    .deprecation();

            assertEquals(5, options.size());
            assertTrue(options.contains("--add-reads"));
            assertTrue(options.contains("-deprecation"));
        }

        @Test
        void testAddReadsMultipleCalls() {
            options.addReads("module1=module2")
                    .addReads("module3=module4");

            assertEquals(4, options.size());
            assertEquals("--add-reads", options.get(0));
            assertEquals("module1=module2", options.get(1));
            assertEquals("--add-reads", options.get(2));
            assertEquals("module3=module4", options.get(3));
        }

        @Test
        void testAddReadsReturnsThis() {
            var result = options.addReads("module1=module2");
            assertSame(options, result);
        }

        @Test
        void testAddReadsWithEmptyList() {
            options.addReads(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testAddReadsWithEmptyVarargs() {
            options.addReads();

            assertEquals(0, options.size());
        }

        @Test
        void testAddReadsWithList() {
            var modules = Arrays.asList("mod1=mod2", "mod3=mod4", "mod5=mod6");
            options.addReads(modules);

            assertEquals(2, options.size());
            assertEquals("--add-reads", options.get(0));
            assertEquals("mod1=mod2,mod3=mod4,mod5=mod6", options.get(1));
        }

        @Test
        void testAddReadsWithSingleModule() {
            options.addReads("mymodule=java.base");

            assertEquals(2, options.size());
            assertEquals("--add-reads", options.get(0));
            assertEquals("mymodule=java.base", options.get(1));
        }

        @Test
        void testAddReadsWithVarargs() {
            options.addReads("module1=module2", "module3=module4");

            assertEquals(2, options.size());
            assertEquals("--add-reads", options.get(0));
            assertEquals("module1=module2,module3=module4", options.get(1));
        }
    }

    @Nested
    class CombinedModuleOptionsTests {
        @Test
        void testAddReadsAndAddExportsTogether() {
            options.addReads("module1=module2")
                    .addExports("module3/package3=ALL-UNNAMED");

            assertEquals(4, options.size());
            assertTrue(options.contains("--add-reads"));
            assertTrue(options.contains("--add-exports"));
        }

        @Test
        void testAllModuleOptionsWithRelease() {
            options.release(17)
                    .addReads("mod1=mod2")
                    .addExports("mod3/pkg3=ALL-UNNAMED")
                    .addModules("java.sql", "java.xml");

            assertTrue(options.containsRelease());
            assertTrue(options.contains("--add-reads"));
            assertTrue(options.contains("--add-exports"));
            assertTrue(options.contains("--add-modules"));
        }

        @Test
        void testModuleOptionsMethodChaining() {
            var result = options
                    .addReads("mod1=mod2")
                    .addExports("mod3/pkg3=mod4")
                    .release(11)
                    .deprecation();

            assertSame(options, result);
            assertEquals(7, options.size());
        }
    }

    @Nested
    class DefaultModuleForCreatedFilesTests {
        @Test
        void testDefaultModuleForCreatedFilesMethodChaining() {
            options.defaultModuleForCreatedFiles("mymodule")
                    .parameters()
                    .deprecation();

            assertEquals(4, options.size());
            assertTrue(options.contains("--default-module-for-created-files"));
            assertTrue(options.contains("-parameters"));
            assertTrue(options.contains("-deprecation"));
        }

        @Test
        void testDefaultModuleForCreatedFilesReturnsThis() {
            var result = options.defaultModuleForCreatedFiles("mymodule");
            assertSame(options, result);
        }

        @Test
        void testDefaultModuleForCreatedFilesWithAnnotationProcessing() {
            options.defaultModuleForCreatedFiles("annotations.generated")
                    .sourceOutput("generated/sources")
                    .processorPath("lib/processors.jar");

            assertEquals(6, options.size());
            assertTrue(options.contains("--default-module-for-created-files"));
            assertTrue(options.contains("-s"));
            assertTrue(options.contains("--processor-path"));
        }

        @Test
        void testDefaultModuleForCreatedFilesWithModulePath() {
            options.defaultModuleForCreatedFiles("mymodule")
                    .modulePath("lib/modules")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--default-module-for-created-files"));
            assertTrue(options.contains("--module-path"));
            assertTrue(options.containsRelease());
        }

        @Test
        void testDefaultModuleForCreatedFilesWithProcessing() {
            options.defaultModuleForCreatedFiles("mymodule")
                    .process(JavacOptions.Processing.ONLY)
                    .processors("com.example.MyProcessor");

            assertEquals(5, options.size());
            assertTrue(options.contains("--default-module-for-created-files"));
            assertTrue(options.contains("-proc:only"));
            assertTrue(options.contains("-processor"));
        }

        @Test
        void testDefaultModuleForCreatedFilesWithQualifiedName() {
            options.defaultModuleForCreatedFiles("com.example.mymodule");

            assertEquals(2, options.size());
            assertEquals("--default-module-for-created-files", options.get(0));
            assertEquals("com.example.mymodule", options.get(1));
        }

        @Test
        void testDefaultModuleForCreatedFilesWithSingleModule() {
            options.defaultModuleForCreatedFiles("mymodule");

            assertEquals(2, options.size());
            assertEquals("--default-module-for-created-files", options.get(0));
            assertEquals("mymodule", options.get(1));
        }

        @Test
        void testMultipleDefaultModuleForCreatedFilesCalls() {
            options.defaultModuleForCreatedFiles("module1")
                    .defaultModuleForCreatedFiles("module2");

            assertEquals(4, options.size());
            assertEquals("--default-module-for-created-files", options.get(0));
            assertEquals("module1", options.get(1));
            assertEquals("--default-module-for-created-files", options.get(2));
            assertEquals("module2", options.get(3));
        }
    }

    @Nested
    class PatchModuleTests {
        @Test
        void testMultiplePatchModuleCalls() {
            options.patchModule("module1=path1")
                    .patchModule("module2=path2");

            assertEquals(4, options.size());
            assertEquals("--patch-module", options.get(0));
            assertEquals("module1=path1", options.get(1));
            assertEquals("--patch-module", options.get(2));
            assertEquals("module2=path2", options.get(3));
        }

        @Test
        void testPatchModuleMethodChaining() {
            options.patchModule("module1=path1")
                    .patchModule("module2=path2")
                    .deprecation();

            assertEquals(5, options.size());
            assertTrue(options.contains("--patch-module"));
            assertTrue(options.contains("-deprecation"));
        }

        @Test
        void testPatchModuleReturnsThis() {
            var result = options.patchModule("mymodule=path/to/classes");
            assertSame(options, result);
        }

        @Test
        void testPatchModuleWithDirectoryPath() {
            options.patchModule("com.example.module=build/classes");

            assertEquals(2, options.size());
            assertEquals("--patch-module", options.get(0));
            assertEquals("com.example.module=build/classes", options.get(1));
        }

        @Test
        void testPatchModuleWithJarPath() {
            options.patchModule("java.base=mylib.jar");

            assertEquals(2, options.size());
            assertEquals("--patch-module", options.get(0));
            assertEquals("java.base=mylib.jar", options.get(1));
        }

        @Test
        void testPatchModuleWithMultiplePaths() {
            options.patchModule("mymodule=path1:path2:path3");

            assertEquals(2, options.size());
            assertEquals("--patch-module", options.get(0));
            assertEquals("mymodule=path1:path2:path3", options.get(1));
        }

        @Test
        void testPatchModuleWithOtherModuleOptions() {
            options.patchModule("mymodule=classes")
                    .addModules("java.sql")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--patch-module"));
            assertTrue(options.contains("--add-modules"));
            assertTrue(options.containsRelease());
        }

        @Test
        void testPatchModuleWithSingleModule() {
            options.patchModule("mymodule=path/to/classes");

            assertEquals(2, options.size());
            assertEquals("--patch-module", options.get(0));
            assertEquals("mymodule=path/to/classes", options.get(1));
        }
    }

    @Nested
    class ReleaseTests {
        @Test
        void testContainsReleaseAfterClear() {
            options.release(11);
            assertTrue(options.containsRelease());

            options.clear();
            assertFalse(options.containsRelease());
        }

        @Test
        void testContainsReleaseAfterMultipleOptions() {
            options.deprecation()
                    .parameters()
                    .release(17)
                    .warningError();

            assertTrue(options.containsRelease());
        }

        @Test
        void testContainsReleaseWhenNotSet() {
            assertFalse(options.containsRelease());
        }

        @Test
        void testContainsReleaseWhenSet() {
            options.release(11);
            assertTrue(options.containsRelease());
        }

        @Test
        void testContainsReleaseWithOnlyReleaseOption() {
            options.add("--release");
            assertTrue(options.containsRelease());
        }

        @Test
        void testContainsReleaseWithOtherOptions() {
            options.deprecation()
                    .parameters()
                    .warningError();

            assertFalse(options.containsRelease());
        }

        @Test
        void testMultipleReleaseCalls() {
            options.release(11)
                    .release(17);

            assertEquals(4, options.size());
            assertEquals("--release", options.get(0));
            assertEquals("11", options.get(1));
            assertEquals("--release", options.get(2));
            assertEquals("17", options.get(3));
            assertTrue(options.containsRelease());
        }

        @Test
        void testReleaseMethodChaining() {
            options.release(17)
                    .enablePreview()
                    .deprecation();

            assertEquals(4, options.size());
            assertTrue(options.contains("--release"));
            assertTrue(options.contains("--enable-preview"));
            assertTrue(options.contains("-deprecation"));
        }

        @Test
        void testReleaseReturnsThis() {
            var result = options.release(11);
            assertSame(options, result);
        }

        @Test
        void testReleaseWithEnablePreview() {
            options.release(21)
                    .enablePreview();

            assertTrue(options.containsRelease());
            assertTrue(options.contains("--enable-preview"));
            assertEquals(3, options.size());
        }

        @Test
        void testReleaseWithVersion11() {
            options.release(11);

            assertEquals(2, options.size());
            assertEquals("--release", options.get(0));
            assertEquals("11", options.get(1));
        }

        @Test
        void testReleaseWithVersion17() {
            options.release(17);

            assertEquals(2, options.size());
            assertEquals("--release", options.get(0));
            assertEquals("17", options.get(1));
        }

        @Test
        void testReleaseWithVersion21() {
            options.release(21);

            assertEquals(2, options.size());
            assertEquals("--release", options.get(0));
            assertEquals("21", options.get(1));
        }

        @Test
        void testReleaseWithVersion8() {
            options.release(8);

            assertEquals(2, options.size());
            assertEquals("--release", options.get(0));
            assertEquals("8", options.get(1));
        }
    }

    @Nested
    class SourceTests {
        @Test
        void testMultipleSourceCalls() {
            options.source(11)
                    .source(17);

            assertEquals(4, options.size());
            assertEquals("--source", options.get(0));
            assertEquals("11", options.get(1));
            assertEquals("--source", options.get(2));
            assertEquals("17", options.get(3));
        }

        @Test
        void testSourceMethodChaining() {
            options.source(17)
                    .deprecation()
                    .parameters();

            assertEquals(4, options.size());
            assertTrue(options.contains("--source"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
        }

        @Test
        void testSourceReturnsThis() {
            var result = options.source(11);
            assertSame(options, result);
        }

        @Test
        void testSourceWithTarget() {
            options.source(11)
                    .target(11);

            assertEquals(4, options.size());
            assertTrue(options.contains("--source"));
            assertTrue(options.contains("--target"));
        }

        @Test
        void testSourceWithVersion11() {
            options.source(11);

            assertEquals(2, options.size());
            assertEquals("--source", options.get(0));
            assertEquals("11", options.get(1));
        }

        @Test
        void testSourceWithVersion17() {
            options.source(17);

            assertEquals(2, options.size());
            assertEquals("--source", options.get(0));
            assertEquals("17", options.get(1));
        }

        @Test
        void testSourceWithVersion21() {
            options.source(21);

            assertEquals(2, options.size());
            assertEquals("--source", options.get(0));
            assertEquals("21", options.get(1));
        }

        @Test
        void testSourceWithVersion8() {
            options.source(8);

            assertEquals(2, options.size());
            assertEquals("--source", options.get(0));
            assertEquals("8", options.get(1));
        }
    }

    @Nested
    class TargetTests {
        @Test
        void testMultipleTargetCalls() {
            options.target(11)
                    .target(17);

            assertEquals(4, options.size());
            assertEquals("--target", options.get(0));
            assertEquals("11", options.get(1));
            assertEquals("--target", options.get(2));
            assertEquals("17", options.get(3));
        }

        @Test
        void testTargetMethodChaining() {
            options.target(17)
                    .deprecation()
                    .parameters();

            assertEquals(4, options.size());
            assertTrue(options.contains("--target"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
        }

        @Test
        void testTargetReturnsThis() {
            var result = options.target(11);
            assertSame(options, result);
        }

        @Test
        void testTargetWithRelease() {
            options.target(17)
                    .release(17)
                    .enablePreview();

            assertEquals(5, options.size());
            assertTrue(options.contains("--target"));
            assertTrue(options.contains("--release"));
            assertTrue(options.contains("--enable-preview"));
        }

        @Test
        void testTargetWithSource() {
            options.target(17)
                    .source(17);

            assertEquals(4, options.size());
            assertTrue(options.contains("--target"));
            assertTrue(options.contains("--source"));
        }

        @Test
        void testTargetWithVersion11() {
            options.target(11);

            assertEquals(2, options.size());
            assertEquals("--target", options.get(0));
            assertEquals("11", options.get(1));
        }

        @Test
        void testTargetWithVersion17() {
            options.target(17);

            assertEquals(2, options.size());
            assertEquals("--target", options.get(0));
            assertEquals("17", options.get(1));
        }

        @Test
        void testTargetWithVersion21() {
            options.target(21);

            assertEquals(2, options.size());
            assertEquals("--target", options.get(0));
            assertEquals("21", options.get(1));
        }

        @Test
        void testTargetWithVersion8() {
            options.target(8);

            assertEquals(2, options.size());
            assertEquals("--target", options.get(0));
            assertEquals("8", options.get(1));
        }
    }

    @Nested
    class XLintTests {
        @Test
        void testMultipleXLintCalls() {
            options.xLint(DEPRECATION)
                    .xLint(UNCHECKED);

            assertEquals(2, options.size());
            assertTrue(options.contains("-Xlint:deprecation"));
            assertTrue(options.contains("-Xlint:unchecked"));
        }

        @Test
        void testMultipleXLintDisableCalls() {
            options.xLintDisable(RAWTYPES)
                    .xLintDisable(SERIAL);

            assertEquals(2, options.size());
            assertTrue(options.contains("-Xlint:-rawtypes"));
            assertTrue(options.contains("-Xlint:-serial"));
        }

        @Test
        void testXLintAllKey() {
            options.xLint(ALL);

            assertTrue(options.contains("-Xlint:all"));
        }

        @Test
        void testXLintBasic() {
            options.xLint();

            assertTrue(options.contains("-Xlint"));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintCaseSensitivity() {
            options.xLint(DEPRECATION, UNCHECKED);

            var result = options.get(0);
            assertEquals("-Xlint:deprecation,unchecked", result);
            assertFalse(result.contains("DEPRECATION"));
            assertFalse(result.contains("UNCHECKED"));
        }

        @Test
        void testXLintCombinedWithOtherOptions() {
            options.deprecation()
                    .xLint(UNCHECKED, RAWTYPES)
                    .warningError();

            assertEquals(3, options.size());
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-Xlint:unchecked,rawtypes"));
            assertTrue(options.contains("-Werror"));
        }

        @Test
        void testXLintDisableAllKey() {
            options.xLintDisable(ALL);

            assertTrue(options.contains("-Xlint:-all"));
        }

        @Test
        void testXLintDisableCombinedWithOtherOptions() {
            options.xLint()
                    .xLintDisable(DEPRECATION)
                    .parameters();

            assertEquals(3, options.size());
            assertTrue(options.contains("-Xlint"));
            assertTrue(options.contains("-Xlint:-deprecation"));
            assertTrue(options.contains("-parameters"));
        }

        @Test
        void testXLintDisableEmptyList() {
            List<XLintKey> emptyList = List.of();
            options.xLintDisable(emptyList);

            assertEquals(0, options.size());
        }

        @Test
        void testXLintDisableNullList() {
            options.xLintDisable((List<XLintKey>) null);

            assertEquals(0, options.size());
        }

        @Test
        void testXLintDisableReturnsThis() {
            var result = options.xLintDisable(UNCHECKED);
            assertSame(options, result);
        }

        @Test
        void testXLintDisableWithList() {
            var keys = Arrays.asList(SERIAL, STATIC, STRICTFP);
            options.xLintDisable(keys);

            assertTrue(options.contains("-Xlint:-serial,-static,-strictfp"));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintDisableWithMultipleKeys() {
            options.xLintDisable(UNCHECKED, RAWTYPES, FALLTHROUGH);

            assertTrue(options.contains("-Xlint:-unchecked,-rawtypes,-fallthrough"));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintDisableWithSingleKey() {
            options.xLintDisable(DEPRECATION);

            assertTrue(options.contains("-Xlint:-deprecation"));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintDisableWithUnderscoreConversion() {
            options.xLintDisable(DEP_ANN, MISSING_EXPLICIT_CTOR);

            assertTrue(options.contains("-Xlint:-dep-ann,-missing-explicit-ctor"));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintEmptyList() {
            List<XLintKey> emptyList = List.of();
            options.xLint(emptyList);

            assertEquals(0, options.size());
        }

        @Test
        void testXLintMethodChaining() {
            var result = options
                    .xLint()
                    .xLint(DEPRECATION)
                    .xLintDisable(RAWTYPES)
                    .deprecation();

            assertSame(options, result);
            assertEquals(4, options.size());
        }

        @Test
        void testXLintNoneKey() {
            options.xLint(NONE);

            assertTrue(options.contains("-Xlint:none"));
        }

        @Test
        void testXLintNullList() {
            options.xLint((List<XLintKey>) null);
            assertEquals(0, options.size());
        }

        @Test
        void testXLintReturnsThis() {
            var result = options.xLint();
            assertSame(options, result);
        }

        @Test
        void testXLintWithAllAvailableKeys() {
            options.xLint(
                    ALL, AUXILIARYCLASS, CAST, CLASSFILE, DANGLING_DOC_COMMENTS,
                    DEP_ANN, DEPRECATION, DIVZERO, EMPTY, EXPORTS, FALLTHROUGH,
                    FINALLY, IDENTITY, INCUBATING, LOSSY_CONVERSIONS,
                    MISSING_EXPLICIT_CTOR, MODULE, NONE, OPENS, OPTIONS,
                    OUTPUT_FILE_CLASH, OVERLOADS, OVERRIDES, PATH, PREVIEW,
                    PROCESSING, RAWTYPES, REMOVAL, REQUIRES_AUTOMATIC,
                    REQUIRES_TRANSITIVE_AUTOMATIC, RESTRICTED, SERIAL, STATIC,
                    STRICTFP, SYNCHRONIZATION, TEXT_BLOCKS, THIS_ESCAPE,
                    TRY, UNCHECKED, VARARGS
            );

            String[] keys = {
                    "all", "auxiliaryclass", "cast", "classfile", "dangling-doc-comments",
                    "dep-ann", "deprecation", "divzero", "empty", "exports", "fallthrough",
                    "finally", "identity", "incubating", "lossy-conversions",
                    "missing-explicit-ctor", "module", "none", "opens", "options",
                    "output-file-clash", "overloads", "overrides", "path", "preview",
                    "processing", "rawtypes", "removal", "requires-automatic",
                    "requires-transitive-automatic", "restricted", "serial", "static",
                    "strictfp", "synchronization", "text-blocks", "this-escape",
                    "try", "unchecked", "varargs"
            };

            assertEquals(1, options.size());
            var result = options.get(0);
            assertTrue(result.startsWith("-Xlint:"));

            for (var key : keys) {
                assertTrue(result.contains(key));
            }
        }

        @Test
        void testXLintWithAllUnderscoreKeys() {
            options.xLint(
                    DANGLING_DOC_COMMENTS,
                    LOSSY_CONVERSIONS,
                    REQUIRES_AUTOMATIC,
                    REQUIRES_TRANSITIVE_AUTOMATIC,
                    TEXT_BLOCKS,
                    THIS_ESCAPE
            );

            var expected = "-Xlint:dangling-doc-comments,lossy-conversions,requires-automatic," +
                    "requires-transitive-automatic,text-blocks,this-escape";
            assertTrue(options.contains(expected));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintWithKeysReturnsThis() {
            var result = options.xLint(DEPRECATION);
            assertSame(options, result);
        }

        @Test
        void testXLintWithList() {
            var keys = Arrays.asList(CAST, DIVZERO, EMPTY);
            options.xLint(keys);

            assertTrue(options.contains("-Xlint:cast,divzero,empty"));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintWithMultipleKeys() {
            options.xLint(DEPRECATION, UNCHECKED, RAWTYPES);

            assertTrue(options.contains("-Xlint:deprecation,unchecked,rawtypes"));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintWithSingleKey() {
            options.xLint(ALL);

            assertTrue(options.contains("-Xlint:all"));
            assertEquals(1, options.size());
        }

        @Test
        void testXLintWithUnderscoreConversion() {
            options.xLint(DEP_ANN, OUTPUT_FILE_CLASH, MISSING_EXPLICIT_CTOR);

            assertTrue(options.contains("-Xlint:dep-ann,output-file-clash,missing-explicit-ctor"));
            assertEquals(1, options.size());
        }
    }

    @Nested
    class ModuleSourcePathTests {
        @Test
        void testModuleSourcePathWithFile() {
            var file = new File("src/modules");
            options.moduleSourcePath(file);

            assertEquals(2, options.size());
            assertEquals("--module-source-path", options.get(0));
            assertEquals(file.getPath(), options.get(1));
        }

        @Test
        void testModuleSourcePathWithMultipleFiles() {
            var file1 = new File("src/module1");
            var file2 = new File("src/module2");
            options.moduleSourcePath(file1, file2);

            assertEquals(2, options.size());
            assertEquals("--module-source-path", options.get(0));
            assertTrue(options.get(1).contains(file1.getPath()));
            assertTrue(options.get(1).contains(file2.getPath()));
        }

        @Test
        void testModuleSourcePathWithFileCollection() {
            var files = Arrays.asList(
                    new File("src/mod1"),
                    new File("src/mod2"),
                    new File("src/mod3")
            );
            options.moduleSourcePath(files);

            assertEquals(2, options.size());
            assertEquals("--module-source-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testModuleSourcePathWithPath() {
            var path = Path.of("src/modules");
            options.moduleSourcePath(path);

            assertEquals(2, options.size());
            assertEquals("--module-source-path", options.get(0));
            assertEquals(path.toString(), options.get(1));
        }

        @Test
        void testModuleSourcePathWithMultiplePaths() {
            var path1 = Path.of("src/module1");
            var path2 = Path.of("src/module2");
            options.moduleSourcePath(path1, path2);

            assertEquals(2, options.size());
            assertEquals("--module-source-path", options.get(0));
            assertTrue(options.get(1).contains(path1.toString()));
            assertTrue(options.get(1).contains(path2.toString()));
        }

        @Test
        void testModuleSourcePathPathsCollection() {
            var paths = Arrays.asList(
                    Path.of("src/mod1"),
                    Path.of("src/mod2"),
                    Path.of("src/mod3")
            );
            options.moduleSourcePathPaths(paths);

            assertEquals(2, options.size());
            assertEquals("--module-source-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testModuleSourcePathStringsCollection() {
            var paths = Arrays.asList("src/mod1", "src/mod2", "src/mod3");
            options.moduleSourcePathStrings(paths);

            assertEquals(2, options.size());
            assertEquals("--module-source-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testModuleSourcePathWithEmptyCollection() {
            options.moduleSourcePathStrings(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testModuleSourcePathReturnsThis() {
            var result = options.moduleSourcePath("src/modules");
            assertSame(options, result);
        }

        @Test
        void testModuleSourcePathMethodChaining() {
            options.moduleSourcePath("src/modules")
                    .modulePath("lib/modules")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--module-source-path"));
            assertTrue(options.contains("--module-path"));
            assertTrue(options.containsRelease());
        }

        @Test
        void testMultipleModuleSourcePathCalls() {
            options.moduleSourcePath("src/mod1")
                    .moduleSourcePath("src/mod2");

            assertEquals(4, options.size());
            assertEquals("--module-source-path", options.get(0));
            assertEquals("src/mod1", options.get(1));
            assertEquals("--module-source-path", options.get(2));
            assertEquals("src/mod2", options.get(3));
        }
    }

    @Nested
    class ProcessorModulePathTests {
        @Test
        void testProcessorModulePathWithFile() {
            var file = new File("lib/processor-modules");
            options.processorModulePath(file);

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            assertEquals(file.getAbsolutePath(), options.get(1));
        }

        @Test
        void testProcessorModulePathWithMultipleFiles() {
            var file1 = new File("lib/proc1");
            var file2 = new File("lib/proc2");
            options.processorModulePath(file1, file2);

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            assertTrue(options.get(1).contains(file1.getAbsolutePath()));
            assertTrue(options.get(1).contains(file2.getAbsolutePath()));
        }

        @Test
        void testProcessorModulePathWithFileCollection() {
            var files = Arrays.asList(
                    new File("lib/proc1"),
                    new File("lib/proc2"),
                    new File("lib/proc3")
            );
            options.processorModulePath(files);

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testProcessorModulePathWithPath() {
            var path = Path.of("lib/processor-modules");
            options.processorModulePath(path);

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            assertEquals(path.toString(), options.get(1));
        }

        @Test
        void testProcessorModulePathWithMultiplePaths() {
            var path1 = Path.of("lib/proc1");
            var path2 = Path.of("lib/proc2");
            options.processorModulePath(path1, path2);

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            assertTrue(options.get(1).contains(path1.toString()));
            assertTrue(options.get(1).contains(path2.toString()));
        }

        @Test
        void testProcessorModulePathPathsCollection() {
            var paths = Arrays.asList(
                    Path.of("lib/proc1"),
                    Path.of("lib/proc2"),
                    Path.of("lib/proc3")
            );
            options.processorModulePathPaths(paths);

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testProcessorModulePathWithString() {
            options.processorModulePath("lib/processors");

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            assertEquals("lib/processors", options.get(1));
        }

        @Test
        void testProcessorModulePathWithMultipleStrings() {
            options.processorModulePath("lib/proc1", "lib/proc2", "lib/proc3");

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testProcessorModulePathStringsCollection() {
            var paths = Arrays.asList("lib/proc1", "lib/proc2", "lib/proc3");
            options.processorModulePathStrings(paths);

            assertEquals(2, options.size());
            assertEquals("--processor-module-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testProcessorModulePathWithEmptyCollection() {
            options.processorModulePathStrings(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testProcessorModulePathReturnsThis() {
            var result = options.processorModulePath("lib/processors");
            assertSame(options, result);
        }

        @Test
        void testProcessorModulePathMethodChaining() {
            options.processorModulePath("lib/proc-modules")
                    .processors("com.example.Processor")
                    .deprecation();

            assertEquals(5, options.size());
            assertTrue(options.contains("--processor-module-path"));
            assertTrue(options.contains("-processor"));
            assertTrue(options.contains("-deprecation"));
        }

        @Test
        void testMultipleProcessorModulePathCalls() {
            options.processorModulePath("lib/proc1")
                    .processorModulePath("lib/proc2");

            assertEquals(4, options.size());
            assertEquals("--processor-module-path", options.get(0));
            assertEquals("lib/proc1", options.get(1));
            assertEquals("--processor-module-path", options.get(2));
            assertEquals("lib/proc2", options.get(3));
        }
    }

    @Nested
    class ProcessorPathTests {
        @Test
        void testProcessorPathWithString() {
            options.processorPath("lib/processors.jar");

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            assertEquals("lib/processors.jar", options.get(1));
        }

        @Test
        void testProcessorPathWithMultipleStrings() {
            options.processorPath("lib/proc1.jar", "lib/proc2.jar", "lib/proc3.jar");

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testProcessorPathStringsCollection() {
            var paths = Arrays.asList("lib/proc1.jar", "lib/proc2.jar", "lib/proc3.jar");
            options.processorPathStrings(paths);

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testProcessorPathWithFile() {
            var file = new File("lib/processors.jar");
            options.processorPath(file);

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            assertEquals(file.getAbsolutePath(), options.get(1));
        }

        @Test
        void testProcessorPathWithMultipleFiles() {
            var file1 = new File("lib/proc1.jar");
            var file2 = new File("lib/proc2.jar");
            options.processorPath(file1, file2);

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            assertTrue(options.get(1).contains(file1.getAbsolutePath()));
            assertTrue(options.get(1).contains(file2.getAbsolutePath()));
        }

        @Test
        void testProcessorPathWithFileCollection() {
            var files = Arrays.asList(
                    new File("lib/proc1.jar"),
                    new File("lib/proc2.jar"),
                    new File("lib/proc3.jar")
            );
            options.processorPath(files);

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testProcessorPathWithPath() {
            var path = Path.of("lib/processors.jar");
            options.processorPath(path);

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            assertEquals(path.toString(), options.get(1));
        }

        @Test
        void testProcessorPathWithMultiplePaths() {
            var path1 = Path.of("lib/proc1.jar");
            var path2 = Path.of("lib/proc2.jar");
            options.processorPath(path1, path2);

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            assertTrue(options.get(1).contains(path1.toString()));
            assertTrue(options.get(1).contains(path2.toString()));
        }

        @Test
        void testProcessorPathPathsCollection() {
            var paths = Arrays.asList(
                    Path.of("lib/proc1.jar"),
                    Path.of("lib/proc2.jar"),
                    Path.of("lib/proc3.jar")
            );
            options.processorPathPaths(paths);

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testProcessorPathWithEmptyCollection() {
            options.processorPathStrings(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testProcessorPathReturnsThis() {
            var result = options.processorPath("lib/processors.jar");
            assertSame(options, result);
        }

        @Test
        void testProcessorPathMethodChaining() {
            options.processorPath("lib/processors.jar")
                    .processors("com.example.Processor")
                    .process(JavacOptions.Processing.ONLY);

            assertEquals(5, options.size());
            assertTrue(options.contains("--processor-path"));
            assertTrue(options.contains("-processor"));
            assertTrue(options.contains("-proc:only"));
        }

        @Test
        void testMultipleProcessorPathCalls() {
            options.processorPath("lib/proc1.jar")
                    .processorPath("lib/proc2.jar");

            assertEquals(4, options.size());
            assertEquals("--processor-path", options.get(0));
            assertEquals("lib/proc1.jar", options.get(1));
            assertEquals("--processor-path", options.get(2));
            assertEquals("lib/proc2.jar", options.get(3));
        }

        @Test
        void testProcessorPathWithDirectoryAndJar() {
            options.processorPath("lib/processors", "lib/extra-processors.jar");

            assertEquals(2, options.size());
            assertEquals("--processor-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(2, paths.length);
        }
    }

    @Nested
    class UpgradeModulePathTests {
        @Test
        void testUpgradeModulePathWithFile() {
            var file = new File("lib/upgrades");
            options.upgradeModulePath(file);

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            assertEquals(file.getAbsolutePath(), options.get(1));
        }

        @Test
        void testUpgradeModulePathWithMultipleFiles() {
            var file1 = new File("lib/upgrade1");
            var file2 = new File("lib/upgrade2");
            options.upgradeModulePath(file1, file2);

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            assertTrue(options.get(1).contains(file1.getAbsolutePath()));
            assertTrue(options.get(1).contains(file2.getAbsolutePath()));
        }

        @Test
        void testUpgradeModulePathWithFileCollection() {
            var files = Arrays.asList(
                    new File("lib/upgrade1"),
                    new File("lib/upgrade2"),
                    new File("lib/upgrade3")
            );
            options.upgradeModulePath(files);

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testUpgradeModulePathWithPath() {
            var path = Path.of("lib/upgrades");
            options.upgradeModulePath(path);

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            assertEquals(path.toString(), options.get(1));
        }

        @Test
        void testUpgradeModulePathWithMultiplePaths() {
            var path1 = Path.of("lib/upgrade1");
            var path2 = Path.of("lib/upgrade2");
            options.upgradeModulePath(path1, path2);

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            assertTrue(options.get(1).contains(path1.toString()));
            assertTrue(options.get(1).contains(path2.toString()));
        }

        @Test
        void testUpgradeModulePathPathsCollection() {
            var paths = Arrays.asList(
                    Path.of("lib/upgrade1"),
                    Path.of("lib/upgrade2"),
                    Path.of("lib/upgrade3")
            );
            options.upgradeModulePathPaths(paths);

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testUpgradeModulePathWithString() {
            options.upgradeModulePath("lib/upgrades");

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            assertEquals("lib/upgrades", options.get(1));
        }

        @Test
        void testUpgradeModulePathWithMultipleStrings() {
            options.upgradeModulePath("lib/upgrade1", "lib/upgrade2", "lib/upgrade3");

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testUpgradeModulePathStringsCollection() {
            var paths = Arrays.asList("lib/upgrade1", "lib/upgrade2", "lib/upgrade3");
            options.upgradeModulePathStrings(paths);

            assertEquals(2, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testUpgradeModulePathWithEmptyCollection() {
            options.upgradeModulePathStrings(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testUpgradeModulePathReturnsThis() {
            var result = options.upgradeModulePath("lib/upgrades");
            assertSame(options, result);
        }

        @Test
        void testUpgradeModulePathMethodChaining() {
            options.upgradeModulePath("lib/upgrades")
                    .modulePath("lib/modules")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--upgrade-module-path"));
            assertTrue(options.contains("--module-path"));
            assertTrue(options.containsRelease());
        }

        @Test
        void testMultipleUpgradeModulePathCalls() {
            options.upgradeModulePath("lib/upgrade1")
                    .upgradeModulePath("lib/upgrade2");

            assertEquals(4, options.size());
            assertEquals("--upgrade-module-path", options.get(0));
            assertEquals("lib/upgrade1", options.get(1));
            assertEquals("--upgrade-module-path", options.get(2));
            assertEquals("lib/upgrade2", options.get(3));
        }

        @Test
        void testUpgradeModulePathWithSystem() {
            options.upgradeModulePath("lib/upgrades")
                    .system("none")
                    .addModules("java.sql");

            assertEquals(6, options.size());
            assertTrue(options.contains("--upgrade-module-path"));
            assertTrue(options.contains("--system"));
            assertTrue(options.contains("--add-modules"));
        }
    }

    @Nested
    class AnnotationOptionTests {
        @Test
        void testAnnotationOptionWithKeyValue() {
            options.annotationOption("debug", "true");

            assertEquals(1, options.size());
            assertEquals("-Adebug=true", options.get(0));
        }

        @Test
        void testAnnotationOptionMultipleCalls() {
            options.annotationOption("debug", "true")
                    .annotationOption("verbose", "false");

            assertEquals(2, options.size());
            assertEquals("-Adebug=true", options.get(0));
            assertEquals("-Averbose=false", options.get(1));
        }

        @Test
        void testAnnotationOptionReturnsThis() {
            var result = options.annotationOption("key", "value");
            assertSame(options, result);
        }

        @Test
        void testAnnotationOptionWithComplexValue() {
            options.annotationOption("outputDir", "/path/to/output");

            assertEquals(1, options.size());
            assertEquals("-AoutputDir=/path/to/output", options.get(0));
        }

        @Test
        void testAnnotationOptionMethodChaining() {
            options.annotationOption("debug", "true")
                    .processors("com.example.Processor")
                    .deprecation();

            assertEquals(4, options.size());
            assertTrue(options.contains("-Adebug=true"));
            assertTrue(options.contains("-processor"));
            assertTrue(options.contains("-deprecation"));
        }
    }

    @Nested
    class AddModulesTests {
        @Test
        void testAddModulesWithSingleModule() {
            options.addModules("java.sql");

            assertEquals(2, options.size());
            assertEquals("--add-modules", options.get(0));
            assertEquals("java.sql", options.get(1));
        }

        @Test
        void testAddModulesWithVarargs() {
            options.addModules("java.sql", "java.xml", "java.desktop");

            assertEquals(2, options.size());
            assertEquals("--add-modules", options.get(0));
            assertEquals("java.sql,java.xml,java.desktop", options.get(1));
        }

        @Test
        void testAddModulesWithList() {
            var modules = Arrays.asList("java.sql", "java.xml", "java.desktop");
            options.addModules(modules);

            assertEquals(2, options.size());
            assertEquals("--add-modules", options.get(0));
            assertEquals("java.sql,java.xml,java.desktop", options.get(1));
        }

        @Test
        void testAddModulesWithEmptyVarargs() {
            options.addModules();

            assertEquals(0, options.size());
        }

        @Test
        void testAddModulesWithEmptyList() {
            options.addModules(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testAddModulesReturnsThis() {
            var result = options.addModules("java.sql");
            assertSame(options, result);
        }

        @Test
        void testAddModulesMethodChaining() {
            options.addModules("java.sql")
                    .modulePath("lib/modules")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--add-modules"));
            assertTrue(options.contains("--module-path"));
            assertTrue(options.containsRelease());
        }

        @Test
        void testAddModulesWithAllModulePath() {
            options.addModules("ALL-MODULE-PATH");

            assertEquals(2, options.size());
            assertEquals("--add-modules", options.get(0));
            assertEquals("ALL-MODULE-PATH", options.get(1));
        }
    }

    @Nested
    class EncodingTests {
        @Test
        void testEncodingWithUTF8() {
            options.encoding("UTF-8");

            assertEquals(2, options.size());
            assertEquals("-encoding", options.get(0));
            assertEquals("UTF-8", options.get(1));
        }

        @Test
        void testEncodingWithISO88591() {
            options.encoding("ISO-8859-1");

            assertEquals(2, options.size());
            assertEquals("-encoding", options.get(0));
            assertEquals("ISO-8859-1", options.get(1));
        }

        @Test
        void testEncodingReturnsThis() {
            var result = options.encoding("UTF-8");
            assertSame(options, result);
        }

        @Test
        void testEncodingMethodChaining() {
            options.encoding("UTF-8")
                    .deprecation()
                    .parameters();

            assertEquals(4, options.size());
            assertTrue(options.contains("-encoding"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
        }

        @Test
        void testMultipleEncodingCalls() {
            options.encoding("UTF-8")
                    .encoding("ISO-8859-1");

            assertEquals(4, options.size());
            assertEquals("-encoding", options.get(0));
            assertEquals("UTF-8", options.get(1));
            assertEquals("-encoding", options.get(2));
            assertEquals("ISO-8859-1", options.get(3));
        }
    }

    @Nested
    class DeprecationTests {
        @Test
        void testDeprecation() {
            options.deprecation();

            assertEquals(1, options.size());
            assertTrue(options.contains("-deprecation"));
        }

        @Test
        void testDeprecationReturnsThis() {
            var result = options.deprecation();
            assertSame(options, result);
        }

        @Test
        void testDeprecationMethodChaining() {
            options.deprecation()
                    .parameters()
                    .warningError();

            assertEquals(3, options.size());
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
            assertTrue(options.contains("-Werror"));
        }
    }

    @Nested
    class EnablePreviewTests {
        @Test
        void testEnablePreview() {
            options.enablePreview();

            assertEquals(1, options.size());
            assertTrue(options.contains("--enable-preview"));
        }

        @Test
        void testEnablePreviewReturnsThis() {
            var result = options.enablePreview();
            assertSame(options, result);
        }

        @Test
        void testEnablePreviewWithRelease() {
            options.release(21)
                    .enablePreview();

            assertEquals(3, options.size());
            assertTrue(options.containsRelease());
            assertTrue(options.contains("--enable-preview"));
        }
    }

    @Nested
    class EndorsedDirsTests {
        @Test
        void testEndorsedDirsWithFile() {
            var file = new File("lib/endorsed");
            options.endorsedDirs(file);

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
            assertEquals(file.getAbsolutePath(), options.get(1));
        }

        @Test
        void testEndorsedDirsWithMultipleFiles() {
            var file1 = new File("lib/endorsed1");
            var file2 = new File("lib/endorsed2");
            options.endorsedDirs(file1, file2);

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
            assertTrue(options.get(1).contains(file1.getAbsolutePath()));
            assertTrue(options.get(1).contains(file2.getAbsolutePath()));
        }

        @Test
        void testEndorsedDirsWithFileCollection() {
            var files = Arrays.asList(
                    new File("lib/endorsed1"),
                    new File("lib/endorsed2"),
                    new File("lib/endorsed3")
            );
            options.endorsedDirs(files);

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
            var paths = options.get(1).split(",");
            assertEquals(3, paths.length);
        }

        @Test
        void testEndorsedDirsWithPath() {
            var path = Path.of("lib/endorsed");
            options.endorsedDirs(path);

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
        }

        @Test
        void testEndorsedDirsWithMultiplePaths() {
            var path1 = Path.of("lib/endorsed1");
            var path2 = Path.of("lib/endorsed2");
            options.endorsedDirs(path1, path2);

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
        }

        @Test
        void testEndorsedDirsPathsCollection() {
            var paths = Arrays.asList(
                    Path.of("lib/endorsed1"),
                    Path.of("lib/endorsed2"),
                    Path.of("lib/endorsed3")
            );
            options.endorsedDirsPaths(paths);

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
            var pathArray = options.get(1).split(",");
            assertEquals(3, pathArray.length);
        }

        @Test
        void testEndorsedDirsWithString() {
            options.endorsedDirs("lib/endorsed");

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
            assertEquals("lib/endorsed", options.get(1));
        }

        @Test
        void testEndorsedDirsWithMultipleStrings() {
            options.endorsedDirs("lib/endorsed1", "lib/endorsed2", "lib/endorsed3");

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
            var paths = options.get(1).split(",");
            assertEquals(3, paths.length);
        }

        @Test
        void testEndorsedDirsStringsCollection() {
            var paths = Arrays.asList("lib/endorsed1", "lib/endorsed2", "lib/endorsed3");
            options.endorsedDirsStrings(paths);

            assertEquals(2, options.size());
            assertEquals("-endorseddirs", options.get(0));
            var pathArray = options.get(1).split(",");
            assertEquals(3, pathArray.length);
        }

        @Test
        void testEndorsedDirsWithEmptyCollection() {
            options.endorsedDirsStrings(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testEndorsedDirsReturnsThis() {
            var result = options.endorsedDirs("lib/endorsed");
            assertSame(options, result);
        }
    }

    @Nested
    class ExtDirsTests {
        @Test
        void testExtDirsWithFile() {
            var file = new File("lib/ext");
            options.extDirs(file);

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
            assertEquals(file.getAbsolutePath(), options.get(1));
        }

        @Test
        void testExtDirsWithMultipleFiles() {
            var file1 = new File("lib/ext1");
            var file2 = new File("lib/ext2");
            options.extDirs(file1, file2);

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
            assertTrue(options.get(1).contains(file1.getAbsolutePath()));
            assertTrue(options.get(1).contains(file2.getAbsolutePath()));
        }

        @Test
        void testExtDirsWithFileCollection() {
            var files = Arrays.asList(
                    new File("lib/ext1"),
                    new File("lib/ext2"),
                    new File("lib/ext3")
            );
            options.extDirs(files);

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
            var paths = options.get(1).split(",");
            assertEquals(3, paths.length);
        }

        @Test
        void testExtDirsWithPath() {
            var path = Path.of("lib/ext");
            options.extDirs(path);

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
        }

        @Test
        void testExtDirsWithMultiplePaths() {
            var path1 = Path.of("lib/ext1");
            var path2 = Path.of("lib/ext2");
            options.extDirs(path1, path2);

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
        }

        @Test
        void testExtDirsPathsCollection() {
            var paths = Arrays.asList(
                    Path.of("lib/ext1"),
                    Path.of("lib/ext2"),
                    Path.of("lib/ext3")
            );
            options.extDirsPaths(paths);

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
            var pathArray = options.get(1).split(",");
            assertEquals(3, pathArray.length);
        }

        @Test
        void testExtDirsWithString() {
            options.extDirs("lib/ext");

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
            assertEquals("lib/ext", options.get(1));
        }

        @Test
        void testExtDirsWithMultipleStrings() {
            options.extDirs("lib/ext1", "lib/ext2", "lib/ext3");

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
            var paths = options.get(1).split(",");
            assertEquals(3, paths.length);
        }

        @Test
        void testExtDirsStringsCollection() {
            var paths = Arrays.asList("lib/ext1", "lib/ext2", "lib/ext3");
            options.extDirsStrings(paths);

            assertEquals(2, options.size());
            assertEquals("-extdirs", options.get(0));
            var pathArray = options.get(1).split(",");
            assertEquals(3, pathArray.length);
        }

        @Test
        void testExtDirsWithEmptyCollection() {
            options.extDirsStrings(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testExtDirsReturnsThis() {
            var result = options.extDirs("lib/ext");
            assertSame(options, result);
        }
    }

    @Nested
    class DebuggingInfoTests {
        @Test
        void testDebuggingInfoAll() {
            options.debuggingInfo(JavacOptions.DebuggingInfo.ALL);

            assertEquals(1, options.size());
            assertEquals("-g", options.get(0));
        }

        @Test
        void testDebuggingInfoNone() {
            options.debuggingInfo(JavacOptions.DebuggingInfo.NONE);

            assertEquals(1, options.size());
            assertEquals("-g:none", options.get(0));
        }

        @Test
        void testDebuggingInfoLines() {
            options.debuggingInfo(JavacOptions.DebuggingInfo.LINES);

            assertEquals(1, options.size());
            assertEquals("-g:lines", options.get(0));
        }

        @Test
        void testDebuggingInfoVar() {
            options.debuggingInfo(JavacOptions.DebuggingInfo.VAR);

            assertEquals(1, options.size());
            assertEquals("-g:var", options.get(0));
        }

        @Test
        void testDebuggingInfoSource() {
            options.debuggingInfo(JavacOptions.DebuggingInfo.SOURCE);

            assertEquals(1, options.size());
            assertEquals("-g:source", options.get(0));
        }

        @Test
        void testDebuggingInfoReturnsThis() {
            var result = options.debuggingInfo(JavacOptions.DebuggingInfo.ALL);
            assertSame(options, result);
        }

        @Test
        void testDebuggingInfoMethodChaining() {
            options.debuggingInfo(JavacOptions.DebuggingInfo.LINES)
                    .deprecation()
                    .parameters();

            assertEquals(3, options.size());
            assertTrue(options.contains("-g:lines"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
        }
    }

    @Nested
    class NativeHeadersTests {
        @Test
        void testNativeHeadersWithFile() {
            var file = new File("build/headers");
            options.nativeHeaders(file);

            assertEquals(2, options.size());
            assertEquals("-h", options.get(0));
            assertEquals(file.getAbsolutePath(), options.get(1));
        }

        @Test
        void testNativeHeadersWithPath() {
            var path = Path.of("build/headers");
            options.nativeHeaders(path);

            assertEquals(2, options.size());
            assertEquals("-h", options.get(0));
            assertEquals(path.toFile().getAbsolutePath(), options.get(1));
        }

        @Test
        void testNativeHeadersWithString() {
            options.nativeHeaders("build/headers");

            assertEquals(2, options.size());
            assertEquals("-h", options.get(0));
            assertEquals("build/headers", options.get(1));
        }

        @Test
        void testNativeHeadersReturnsThis() {
            var result = options.nativeHeaders("build/headers");
            assertSame(options, result);
        }

        @Test
        void testNativeHeadersMethodChaining() {
            options.nativeHeaders("build/headers")
                    .deprecation()
                    .parameters();

            assertEquals(4, options.size());
            assertTrue(options.contains("-h"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
        }
    }

    @Nested
    class ImplicitTests {
        @Test
        void testImplicitNone() {
            options.implicit(JavacOptions.Implicit.NONE);

            assertEquals(1, options.size());
            assertEquals("-implicit:none", options.get(0));
        }

        @Test
        void testImplicitClass() {
            options.implicit(JavacOptions.Implicit.CLASS);

            assertEquals(1, options.size());
            assertEquals("-implicit:class", options.get(0));
        }

        @Test
        void testImplicitReturnsThis() {
            var result = options.implicit(JavacOptions.Implicit.NONE);
            assertSame(options, result);
        }

        @Test
        void testImplicitMethodChaining() {
            options.implicit(JavacOptions.Implicit.CLASS)
                    .deprecation()
                    .parameters();

            assertEquals(3, options.size());
            assertTrue(options.contains("-implicit:class"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
        }
    }

    @Nested
    class LimitModulesTests {
        @Test
        void testLimitModulesWithSingleModule() {
            options.limitModules("java.base");

            assertEquals(2, options.size());
            assertEquals("--limit-modules", options.get(0));
            assertEquals("java.base", options.get(1));
        }

        @Test
        void testLimitModulesWithVarargs() {
            options.limitModules("java.base", "java.sql", "java.xml");

            assertEquals(2, options.size());
            assertEquals("--limit-modules", options.get(0));
            assertEquals("java.base,java.sql,java.xml", options.get(1));
        }

        @Test
        void testLimitModulesWithList() {
            var modules = Arrays.asList("java.base", "java.sql", "java.xml");
            options.limitModules(modules);

            assertEquals(2, options.size());
            assertEquals("--limit-modules", options.get(0));
            assertEquals("java.base,java.sql,java.xml", options.get(1));
        }

        @Test
        void testLimitModulesWithEmptyVarargs() {
            options.limitModules();

            assertEquals(0, options.size());
        }

        @Test
        void testLimitModulesWithEmptyList() {
            options.limitModules(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testLimitModulesReturnsThis() {
            var result = options.limitModules("java.base");
            assertSame(options, result);
        }

        @Test
        void testLimitModulesMethodChaining() {
            options.limitModules("java.base")
                    .modulePath("lib/modules")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--limit-modules"));
            assertTrue(options.contains("--module-path"));
            assertTrue(options.containsRelease());
        }
    }

    @Nested
    class ModuleTests {
        @Test
        void testModuleWithSingleModule() {
            options.module("com.example.myapp");

            assertEquals(2, options.size());
            assertEquals("--module", options.get(0));
            assertEquals("com.example.myapp", options.get(1));
        }

        @Test
        void testModuleWithVarargs() {
            options.module("com.example.module1", "com.example.module2", "com.example.module3");

            assertEquals(2, options.size());
            assertEquals("--module", options.get(0));
            assertEquals("com.example.module1,com.example.module2,com.example.module3", options.get(1));
        }

        @Test
        void testModuleWithList() {
            var modules = Arrays.asList("com.example.module1", "com.example.module2", "com.example.module3");
            options.module(modules);

            assertEquals(2, options.size());
            assertEquals("--module", options.get(0));
            assertEquals("com.example.module1,com.example.module2,com.example.module3", options.get(1));
        }

        @Test
        void testModuleWithEmptyVarargs() {
            options.module();

            assertEquals(0, options.size());
        }

        @Test
        void testModuleWithEmptyList() {
            options.module(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testModuleReturnsThis() {
            var result = options.module("com.example.myapp");
            assertSame(options, result);
        }

        @Test
        void testModuleMethodChaining() {
            options.module("com.example.myapp")
                    .modulePath("lib/modules")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--module"));
            assertTrue(options.contains("--module-path"));
            assertTrue(options.containsRelease());
        }
    }

    @Nested
    class ModulePathTests {
        @Test
        void testModulePathWithFile() {
            var file = new File("lib/modules");
            options.modulePath(file);

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            assertEquals(file.getAbsolutePath(), options.get(1));
        }

        @Test
        void testModulePathWithMultipleFiles() {
            var file1 = new File("lib/modules1");
            var file2 = new File("lib/modules2");
            options.modulePath(file1, file2);

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            assertTrue(options.get(1).contains(file1.getAbsolutePath()));
            assertTrue(options.get(1).contains(file2.getAbsolutePath()));
        }

        @Test
        void testModulePathWithFileCollection() {
            var files = Arrays.asList(
                    new File("lib/modules1"),
                    new File("lib/modules2"),
                    new File("lib/modules3")
            );
            options.modulePath(files);

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testModulePathWithPath() {
            var path = Path.of("lib/modules");
            options.modulePath(path);

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            assertEquals(path.toString(), options.get(1));
        }

        @Test
        void testModulePathWithMultiplePaths() {
            var path1 = Path.of("lib/modules1");
            var path2 = Path.of("lib/modules2");
            options.modulePath(path1, path2);

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            assertTrue(options.get(1).contains(path1.toString()));
            assertTrue(options.get(1).contains(path2.toString()));
        }

        @Test
        void testModulePathPathsCollection() {
            var paths = Arrays.asList(
                    Path.of("lib/modules1"),
                    Path.of("lib/modules2"),
                    Path.of("lib/modules3")
            );
            options.modulePathPaths(paths);

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testModulePathWithString() {
            options.modulePath("lib/modules");

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            assertEquals("lib/modules", options.get(1));
        }

        @Test
        void testModulePathWithMultipleStrings() {
            options.modulePath("lib/modules1", "lib/modules2", "lib/modules3");

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            var paths = options.get(1).split(File.pathSeparator);
            assertEquals(3, paths.length);
        }

        @Test
        void testModulePathStringsCollection() {
            var paths = Arrays.asList("lib/modules1", "lib/modules2", "lib/modules3");
            options.modulePathStrings(paths);

            assertEquals(2, options.size());
            assertEquals("--module-path", options.get(0));
            var pathArray = options.get(1).split(File.pathSeparator);
            assertEquals(3, pathArray.length);
        }

        @Test
        void testModulePathWithEmptyCollection() {
            options.modulePathStrings(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testModulePathReturnsThis() {
            var result = options.modulePath("lib/modules");
            assertSame(options, result);
        }

        @Test
        void testModulePathMethodChaining() {
            options.modulePath("lib/modules")
                    .addModules("java.sql")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--module-path"));
            assertTrue(options.contains("--add-modules"));
            assertTrue(options.containsRelease());
        }
    }

    @Nested
    class ModuleVersionTests {
        @Test
        void testModuleVersion() {
            options.moduleVersion("1.0.0");

            assertEquals(2, options.size());
            assertEquals("--module-version", options.get(0));
            assertEquals("1.0.0", options.get(1));
        }

        @Test
        void testModuleVersionReturnsThis() {
            var result = options.moduleVersion("1.0.0");
            assertSame(options, result);
        }

        @Test
        void testModuleVersionMethodChaining() {
            options.moduleVersion("1.0.0")
                    .module("com.example.myapp")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--module-version"));
            assertTrue(options.contains("--module"));
            assertTrue(options.containsRelease());
        }

        @Test
        void testMultipleModuleVersionCalls() {
            options.moduleVersion("1.0.0")
                    .moduleVersion("2.0.0");

            assertEquals(4, options.size());
            assertEquals("--module-version", options.get(0));
            assertEquals("1.0.0", options.get(1));
            assertEquals("--module-version", options.get(2));
            assertEquals("2.0.0", options.get(3));
        }
    }

    @Nested
    class NoWarnTests {
        @Test
        void testNoWarn() {
            options.noWarn();

            assertEquals(1, options.size());
            assertTrue(options.contains("-nowarn"));
        }

        @Test
        void testNoWarnReturnsThis() {
            var result = options.noWarn();
            assertSame(options, result);
        }

        @Test
        void testNoWarnMethodChaining() {
            options.noWarn()
                    .parameters()
                    .deprecation();

            assertEquals(3, options.size());
            assertTrue(options.contains("-nowarn"));
            assertTrue(options.contains("-parameters"));
            assertTrue(options.contains("-deprecation"));
        }
    }

    @Nested
    class ParametersTests {
        @Test
        void testParameters() {
            options.parameters();

            assertEquals(1, options.size());
            assertTrue(options.contains("-parameters"));
        }

        @Test
        void testParametersReturnsThis() {
            var result = options.parameters();
            assertSame(options, result);
        }

        @Test
        void testParametersMethodChaining() {
            options.parameters()
                    .deprecation()
                    .warningError();

            assertEquals(3, options.size());
            assertTrue(options.contains("-parameters"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-Werror"));
        }
    }

    @Nested
    class ProcessTests {
        @Test
        void testProcessFull() {
            options.process(JavacOptions.Processing.FULL);

            assertEquals(1, options.size());
            assertEquals("-proc:full", options.get(0));
        }

        @Test
        void testProcessNone() {
            options.process(JavacOptions.Processing.NONE);

            assertEquals(1, options.size());
            assertEquals("-proc:none", options.get(0));
        }

        @Test
        void testProcessOnly() {
            options.process(JavacOptions.Processing.ONLY);

            assertEquals(1, options.size());
            assertEquals("-proc:only", options.get(0));
        }

        @Test
        void testProcessReturnsThis() {
            var result = options.process(JavacOptions.Processing.ONLY);
            assertSame(options, result);
        }

        @Test
        void testProcessMethodChaining() {
            options.process(JavacOptions.Processing.ONLY)
                    .processors("com.example.Processor")
                    .deprecation();

            assertEquals(4, options.size());
            assertTrue(options.contains("-proc:only"));
            assertTrue(options.contains("-processor"));
            assertTrue(options.contains("-deprecation"));
        }
    }

    @Nested
    class ProcessorsTests {
        @Test
        void testProcessorsWithSingleProcessor() {
            options.processors("com.example.MyProcessor");

            assertEquals(2, options.size());
            assertEquals("-processor", options.get(0));
            assertEquals("com.example.MyProcessor", options.get(1));
        }

        @Test
        void testProcessorsWithVarargs() {
            options.processors("com.example.Processor1", "com.example.Processor2", "com.example.Processor3");

            assertEquals(2, options.size());
            assertEquals("-processor", options.get(0));
            assertEquals("com.example.Processor1,com.example.Processor2,com.example.Processor3", options.get(1));
        }

        @Test
        void testProcessorsWithList() {
            var processors = Arrays.asList("com.example.Processor1", "com.example.Processor2", "com.example.Processor3");
            options.processors(processors);

            assertEquals(2, options.size());
            assertEquals("-processor", options.get(0));
            assertEquals("com.example.Processor1,com.example.Processor2,com.example.Processor3", options.get(1));
        }

        @Test
        void testProcessorsWithEmptyVarargs() {
            options.processors();

            assertEquals(0, options.size());
        }

        @Test
        void testProcessorsWithEmptyList() {
            options.processors(List.of());

            assertEquals(0, options.size());
        }

        @Test
        void testProcessorsReturnsThis() {
            var result = options.processors("com.example.MyProcessor");
            assertSame(options, result);
        }

        @Test
        void testProcessorsMethodChaining() {
            options.processors("com.example.MyProcessor")
                    .processorPath("lib/processors.jar")
                    .deprecation();

            assertEquals(5, options.size());
            assertTrue(options.contains("-processor"));
            assertTrue(options.contains("--processor-path"));
            assertTrue(options.contains("-deprecation"));
        }
    }

    @Nested
    class ProfileTests {
        @Test
        void testProfile() {
            options.profile("compact1");

            assertEquals(2, options.size());
            assertEquals("-profile", options.get(0));
            assertEquals("compact1", options.get(1));
        }

        @Test
        void testProfileCompact2() {
            options.profile("compact2");

            assertEquals(2, options.size());
            assertEquals("-profile", options.get(0));
            assertEquals("compact2", options.get(1));
        }

        @Test
        void testProfileCompact3() {
            options.profile("compact3");

            assertEquals(2, options.size());
            assertEquals("-profile", options.get(0));
            assertEquals("compact3", options.get(1));
        }

        @Test
        void testProfileReturnsThis() {
            var result = options.profile("compact1");
            assertSame(options, result);
        }

        @Test
        void testProfileMethodChaining() {
            options.profile("compact1")
                    .deprecation()
                    .parameters();

            assertEquals(4, options.size());
            assertTrue(options.contains("-profile"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
        }
    }

    @Nested
    class SourceOutputTests {
        @Test
        void testSourceOutputWithString() {
            options.sourceOutput("generated/sources");

            assertEquals(2, options.size());
            assertEquals("-s", options.get(0));
            assertEquals("generated/sources", options.get(1));
        }

        @Test
        void testSourceOutputWithFile() {
            var file = new File("generated/sources");
            options.sourceOutput(file);

            assertEquals(2, options.size());
            assertEquals("-s", options.get(0));
            assertEquals(file.getAbsolutePath(), options.get(1));
        }

        @Test
        void testSourceOutputWithPath() {
            var path = Path.of("generated/sources");
            options.sourceOutput(path);

            assertEquals(2, options.size());
            assertEquals("-s", options.get(0));
            assertEquals(path.toFile().getAbsolutePath(), options.get(1));
        }

        @Test
        void testSourceOutputReturnsThis() {
            var result = options.sourceOutput("generated/sources");
            assertSame(options, result);
        }

        @Test
        void testSourceOutputMethodChaining() {
            options.sourceOutput("generated/sources")
                    .processors("com.example.Processor")
                    .deprecation();

            assertEquals(5, options.size());
            assertTrue(options.contains("-s"));
            assertTrue(options.contains("-processor"));
            assertTrue(options.contains("-deprecation"));
        }
    }

    @Nested
    class SystemTests {
        @Test
        void testSystemWithJdk() {
            options.system("jdk");

            assertEquals(2, options.size());
            assertEquals("--system", options.get(0));
            assertEquals("jdk", options.get(1));
        }

        @Test
        void testSystemWithNone() {
            options.system("none");

            assertEquals(2, options.size());
            assertEquals("--system", options.get(0));
            assertEquals("none", options.get(1));
        }

        @Test
        void testSystemReturnsThis() {
            var result = options.system("none");
            assertSame(options, result);
        }

        @Test
        void testSystemMethodChaining() {
            options.system("none")
                    .modulePath("lib/modules")
                    .release(17);

            assertEquals(6, options.size());
            assertTrue(options.contains("--system"));
            assertTrue(options.contains("--module-path"));
            assertTrue(options.containsRelease());
        }

        @Test
        void testMultipleSystemCalls() {
            options.system("none")
                    .system("jdk");

            assertEquals(4, options.size());
            assertEquals("--system", options.get(0));
            assertEquals("none", options.get(1));
            assertEquals("--system", options.get(2));
            assertEquals("jdk", options.get(3));
        }
    }

    @Nested
    class WarningErrorTests {
        @Test
        void testWarningError() {
            options.warningError();

            assertEquals(1, options.size());
            assertTrue(options.contains("-Werror"));
        }

        @Test
        void testWarningErrorReturnsThis() {
            var result = options.warningError();
            assertSame(options, result);
        }

        @Test
        void testWarningErrorMethodChaining() {
            options.warningError()
                    .deprecation()
                    .parameters();

            assertEquals(3, options.size());
            assertTrue(options.contains("-Werror"));
            assertTrue(options.contains("-deprecation"));
            assertTrue(options.contains("-parameters"));
        }

        @Test
        void testWarningErrorWithXLint() {
            options.xLint(UNCHECKED, DEPRECATION)
                    .warningError();

            assertEquals(2, options.size());
            assertTrue(options.contains("-Xlint:unchecked,deprecation"));
            assertTrue(options.contains("-Werror"));
        }
    }
}
