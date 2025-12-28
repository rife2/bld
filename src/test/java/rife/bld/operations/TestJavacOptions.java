/*
 * Copyright 2025 Erik C. Thauvin (https://erik.thauvin.net/)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import rife.bld.operations.JavacOptions.XLintKey;

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

            assertEquals(2, options.size());
            assertEquals("--add-exports", options.get(0));
            assertEquals("", options.get(1));
        }

        @Test
        void testAddExportsWithEmptyVarargs() {
            options.addExports();

            assertEquals(2, options.size());
            assertEquals("--add-exports", options.get(0));
            assertEquals("", options.get(1));
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

            assertEquals(2, options.size());
            assertEquals("--add-reads", options.get(0));
            assertEquals("", options.get(1));
        }

        @Test
        void testAddReadsWithEmptyVarargs() {
            options.addReads();

            assertEquals(2, options.size());
            assertEquals("--add-reads", options.get(0));
            assertEquals("", options.get(1));
        }

        @Test
        void testAddReadsWithList() {
            List<String> modules = Arrays.asList("mod1=mod2", "mod3=mod4", "mod5=mod6");
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

            assertEquals(1, options.size());
            assertEquals("-Xlint:-", options.get(0));
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

            assertEquals(1, options.size());
            assertEquals("-Xlint:", options.get(0));
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
}
