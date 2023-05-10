/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.junit.jupiter.api.Test;
import rife.tools.StringUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.compile;
import static rife.bld.dependencies.Scope.runtime;

public class TestDependencySet {
    @Test
    void testInstantiation() {
        var set = new DependencySet();
        assertNotNull(set);
        assertTrue(set.isEmpty());
    }

    @Test
    void testPopulation() {
        var set = new DependencySet();

        var dep1 = new Dependency("com.uwyn.rife2", "rife2");
        var dep2 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 3, 0));
        var dep3 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 4, 0));
        var dep4 = new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 3, 1));

        set.add(dep1);
        assertEquals(VersionNumber.UNKNOWN, set.get(dep1).version());
        set.add(dep2);
        assertEquals(dep2.version(), set.get(dep1).version());
        set.add(dep3);
        assertEquals(dep3.version(), set.get(dep2).version());
        set.add(dep4);
        assertEquals(dep3.version(), set.get(dep2).version());
    }

    @Test
    void testAddAll() {
        var set1 = new DependencySet()
            .include(new Dependency("org.eclipse.jetty", "jetty-server", VersionNumber.parse("11.0.14")))
            .include(new Dependency("org.eclipse.jetty.toolchain", "jetty-jakarta-servlet-api", VersionNumber.parse("5.0.2")))
            .include(new Dependency("org.eclipse.jetty", "jetty-http", VersionNumber.parse("11.0.14")))
            .include(new Dependency("org.eclipse.jetty", "jetty-io", VersionNumber.parse("11.0.14")))
            .include(new Dependency("org.eclipse.jetty", "jetty-util", VersionNumber.parse("11.0.14")))
            .include(new Dependency("org.slf4j", "slf4j-api", VersionNumber.parse("2.0.5")));

        var set2 = new DependencySet()
            .include(new Dependency("org.slf4j", "slf4j-simple", VersionNumber.parse("2.0.6")))
            .include(new Dependency("org.slf4j", "slf4j-api", VersionNumber.parse("2.0.6")));

        var set_union1 = new DependencySet(set1);
        set_union1.addAll(set2);
        assertEquals("""
            org.eclipse.jetty:jetty-server:11.0.14
            org.eclipse.jetty.toolchain:jetty-jakarta-servlet-api:5.0.2
            org.eclipse.jetty:jetty-http:11.0.14
            org.eclipse.jetty:jetty-io:11.0.14
            org.eclipse.jetty:jetty-util:11.0.14
            org.slf4j:slf4j-simple:2.0.6
            org.slf4j:slf4j-api:2.0.6""", StringUtils.join(set_union1, "\n"));

        var set_union2 = new DependencySet(set2);
        set_union2.addAll(set1);
        assertEquals("""
            org.slf4j:slf4j-simple:2.0.6
            org.slf4j:slf4j-api:2.0.6
            org.eclipse.jetty:jetty-server:11.0.14
            org.eclipse.jetty.toolchain:jetty-jakarta-servlet-api:5.0.2
            org.eclipse.jetty:jetty-http:11.0.14
            org.eclipse.jetty:jetty-io:11.0.14
            org.eclipse.jetty:jetty-util:11.0.14""", StringUtils.join(set_union2, "\n"));
    }

    @Test
    void testGenerateDependencyTreeJettySlf4j() {
        var dependencies = new DependencySet()
            .include(new Dependency("org.eclipse.jetty", "jetty-server", new VersionNumber(11, 0, 14)))
            .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(2, 0, 6)));
        assertEquals("""
            ├─ org.eclipse.jetty:jetty-server:11.0.14
            │  ├─ org.eclipse.jetty.toolchain:jetty-jakarta-servlet-api:5.0.2
            │  ├─ org.eclipse.jetty:jetty-http:11.0.14
            │  │  └─ org.eclipse.jetty:jetty-util:11.0.14
            │  └─ org.eclipse.jetty:jetty-io:11.0.14
            └─ org.slf4j:slf4j-simple:2.0.6
               └─ org.slf4j:slf4j-api:2.0.6
            """, dependencies.generateTransitiveDependencyTree(ArtifactRetriever.instance(), List.of(MAVEN_CENTRAL), compile));
    }

    @Test
    void testGenerateDependencyTreeSpringBoot() {
        var dependencies = new DependencySet()
            .include(new Dependency("org.springframework.boot", "spring-boot-starter", new VersionNumber(3, 0, 4)));
        assertEquals("""
            └─ org.springframework.boot:spring-boot-starter:3.0.4
               ├─ org.springframework.boot:spring-boot:3.0.4
               │  └─ org.springframework:spring-context:6.0.6
               │     ├─ org.springframework:spring-aop:6.0.6
               │     ├─ org.springframework:spring-beans:6.0.6
               │     └─ org.springframework:spring-expression:6.0.6
               ├─ org.springframework.boot:spring-boot-autoconfigure:3.0.4
               ├─ org.springframework.boot:spring-boot-starter-logging:3.0.4
               │  ├─ ch.qos.logback:logback-classic:1.4.5
               │  │  ├─ ch.qos.logback:logback-core:1.4.5
               │  │  └─ org.slf4j:slf4j-api:2.0.4
               │  ├─ org.apache.logging.log4j:log4j-to-slf4j:2.19.0
               │  │  └─ org.apache.logging.log4j:log4j-api:2.19.0
               │  └─ org.slf4j:jul-to-slf4j:2.0.6
               ├─ jakarta.annotation:jakarta.annotation-api:2.1.1
               ├─ org.springframework:spring-core:6.0.6
               │  └─ org.springframework:spring-jcl:6.0.6
               └─ org.yaml:snakeyaml:1.33
            """, dependencies.generateTransitiveDependencyTree(ArtifactRetriever.instance(), List.of(MAVEN_CENTRAL), compile));
    }

    @Test
    void testGenerateDependencyTreeMaven() {
        var dependencies = new DependencySet()
            .include(new Dependency("org.apache.maven", "maven-core", new VersionNumber(3, 9, 0)));
        assertEquals("""
            └─ org.apache.maven:maven-core:3.9.0
               ├─ org.apache.maven:maven-model:3.9.0
               ├─ org.apache.maven:maven-settings:3.9.0
               ├─ org.apache.maven:maven-settings-builder:3.9.0
               │  └─ org.codehaus.plexus:plexus-sec-dispatcher:2.0
               │     └─ org.codehaus.plexus:plexus-cipher:2.0
               ├─ org.apache.maven:maven-builder-support:3.9.0
               ├─ org.apache.maven:maven-repository-metadata:3.9.0
               ├─ org.apache.maven:maven-artifact:3.9.0
               ├─ org.apache.maven:maven-plugin-api:3.9.0
               ├─ org.apache.maven:maven-model-builder:3.9.0
               ├─ org.apache.maven:maven-resolver-provider:3.9.0
               ├─ org.apache.maven.resolver:maven-resolver-impl:1.9.4
               │  └─ org.apache.maven.resolver:maven-resolver-named-locks:1.9.4
               ├─ org.apache.maven.resolver:maven-resolver-api:1.9.4
               ├─ org.apache.maven.resolver:maven-resolver-spi:1.9.4
               ├─ org.apache.maven.resolver:maven-resolver-util:1.9.4
               ├─ org.apache.maven.shared:maven-shared-utils:3.3.4
               ├─ org.eclipse.sisu:org.eclipse.sisu.plexus:0.3.5
               │  └─ javax.annotation:javax.annotation-api:1.2
               ├─ org.eclipse.sisu:org.eclipse.sisu.inject:0.3.5
               ├─ com.google.inject:guice:5.1.0
               │  └─ aopalliance:aopalliance:1.0
               ├─ com.google.guava:guava:30.1-jre
               ├─ com.google.guava:failureaccess:1.0.1
               ├─ javax.inject:javax.inject:1
               ├─ org.codehaus.plexus:plexus-utils:3.4.2
               ├─ org.codehaus.plexus:plexus-classworlds:2.6.0
               ├─ org.codehaus.plexus:plexus-interpolation:1.26
               ├─ org.codehaus.plexus:plexus-component-annotations:2.1.0
               ├─ org.apache.commons:commons-lang3:3.8.1
               └─ org.slf4j:slf4j-api:1.7.36
            """, dependencies.generateTransitiveDependencyTree(ArtifactRetriever.instance(), List.of(MAVEN_CENTRAL), compile));
    }

    @Test
    void testGenerateDependencyTreePlay() {
        var dependencies = new DependencySet()
            .include(new Dependency("com.typesafe.play", "play_2.13", new VersionNumber(2, 8, 19)));
        assertEquals("""
            └─ com.typesafe.play:play_2.13:2.8.19
               ├─ org.scala-lang:scala-library:2.13.10
               ├─ com.typesafe.play:build-link:2.8.19
               │  └─ com.typesafe.play:play-exceptions:2.8.19
               ├─ com.typesafe.play:play-streams_2.13:2.8.19
               │  ├─ org.reactivestreams:reactive-streams:1.0.3
               │  └─ com.typesafe.akka:akka-stream_2.13:2.6.20
               │     └─ com.typesafe.akka:akka-protobuf-v3_2.13:2.6.20
               ├─ com.typesafe.play:twirl-api_2.13:1.5.1
               │  └─ org.scala-lang.modules:scala-xml_2.13:1.2.0
               ├─ org.slf4j:slf4j-api:1.7.36
               ├─ org.slf4j:jul-to-slf4j:1.7.36
               ├─ org.slf4j:jcl-over-slf4j:1.7.36
               ├─ com.typesafe.akka:akka-actor_2.13:2.6.20
               │  └─ com.typesafe:config:1.4.2
               ├─ com.typesafe.akka:akka-actor-typed_2.13:2.6.20
               ├─ com.typesafe.akka:akka-slf4j_2.13:2.6.20
               ├─ com.typesafe.akka:akka-serialization-jackson_2.13:2.6.20
               │  ├─ com.fasterxml.jackson.module:jackson-module-parameter-names:2.11.4
               │  ├─ com.fasterxml.jackson.dataformat:jackson-dataformat-cbor:2.11.4
               │  ├─ com.fasterxml.jackson.module:jackson-module-scala_2.13:2.11.4
               │  │  └─ com.fasterxml.jackson.module:jackson-module-paranamer:2.11.4
               │  │     └─ com.thoughtworks.paranamer:paranamer:2.8
               │  └─ org.lz4:lz4-java:1.8.0
               ├─ com.fasterxml.jackson.core:jackson-core:2.11.4
               ├─ com.fasterxml.jackson.core:jackson-annotations:2.11.4
               ├─ com.fasterxml.jackson.datatype:jackson-datatype-jdk8:2.11.4
               ├─ com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.11.4
               ├─ com.fasterxml.jackson.core:jackson-databind:2.11.4
               ├─ com.typesafe.play:play-json_2.13:2.8.2
               │  ├─ com.typesafe.play:play-functional_2.13:2.8.2
               │  ├─ org.scala-lang:scala-reflect:2.13.1
               │  └─ joda-time:joda-time:2.10.5
               ├─ com.google.guava:guava:30.1.1-jre
               │  ├─ com.google.guava:failureaccess:1.0.1
               │  ├─ com.google.guava:listenablefuture:9999.0-empty-to-avoid-conflict-with-guava
               │  ├─ com.google.code.findbugs:jsr305:3.0.2
               │  ├─ org.checkerframework:checker-qual:3.8.0
               │  ├─ com.google.errorprone:error_prone_annotations:2.5.1
               │  └─ com.google.j2objc:j2objc-annotations:1.3
               ├─ io.jsonwebtoken:jjwt:0.9.1
               ├─ jakarta.xml.bind:jakarta.xml.bind-api:2.3.3
               │  └─ jakarta.activation:jakarta.activation-api:1.2.2
               ├─ jakarta.transaction:jakarta.transaction-api:1.3.3
               ├─ javax.inject:javax.inject:1
               ├─ org.scala-lang.modules:scala-java8-compat_2.13:1.0.2
               ├─ com.typesafe:ssl-config-core_2.13:0.4.3
               └─ org.scala-lang.modules:scala-parser-combinators_2.13:1.1.2
            """, dependencies.generateTransitiveDependencyTree(ArtifactRetriever.instance(), List.of(MAVEN_CENTRAL), compile));
    }

    @Test
    void testGenerateDependencyTreeVaadin() {
        var dependencies = new DependencySet()
            .include(new Dependency("com.vaadin", "vaadin", new VersionNumber(23, 3, 7)));
        assertEquals("""
            └─ com.vaadin:vaadin:23.3.7
               ├─ com.vaadin:vaadin-core:23.3.7
               │  ├─ com.vaadin:flow-server:23.3.4
               │  │  ├─ com.vaadin.servletdetector:throw-if-servlet5:1.0.2
               │  │  ├─ org.slf4j:slf4j-api:1.7.36
               │  │  ├─ javax.annotation:javax.annotation-api:1.3.2
               │  │  ├─ com.vaadin.external.gwt:gwt-elemental:2.8.2.vaadin2
               │  │  ├─ commons-fileupload:commons-fileupload:1.4
               │  │  ├─ commons-io:commons-io:2.11.0
               │  │  ├─ com.fasterxml.jackson.core:jackson-core:2.14.1
               │  │  ├─ org.jsoup:jsoup:1.15.3
               │  │  ├─ com.helger:ph-css:6.5.0
               │  │  │  └─ com.helger.commons:ph-commons:10.1.6
               │  │  │     └─ com.google.code.findbugs:jsr305:3.0.2
               │  │  ├─ net.bytebuddy:byte-buddy:1.12.20
               │  │  ├─ com.vaadin.external:gentyref:1.2.0.vaadin1
               │  │  ├─ org.apache.commons:commons-compress:1.22
               │  │  ├─ org.apache.httpcomponents:httpclient:4.5.13
               │  │  │  ├─ org.apache.httpcomponents:httpcore:4.4.13
               │  │  │  └─ commons-logging:commons-logging:1.2
               │  │  └─ commons-codec:commons-codec:1.15
               │  ├─ com.vaadin:vaadin-dev-server:23.3.4
               │  │  └─ com.vaadin:open:8.5.0
               │  ├─ com.vaadin:flow-lit-template:23.3.4
               │  ├─ com.vaadin:flow-polymer-template:23.3.4
               │  ├─ com.vaadin:flow-push:23.3.4
               │  │  └─ com.vaadin.external.atmosphere:atmosphere-runtime:2.7.3.slf4jvaadin4
               │  ├─ com.vaadin:flow-client:23.3.4
               │  ├─ com.vaadin:flow-html-components:23.3.4
               │  ├─ com.vaadin:flow-data:23.3.4
               │  │  └─ javax.validation:validation-api:2.0.1.Final
               │  ├─ com.vaadin:flow-dnd:23.3.4
               │  │  ├─ org.webjars.npm:vaadin__vaadin-mobile-drag-drop:1.0.1
               │  │  └─ org.webjars.npm:mobile-drag-drop:2.3.0-rc.2
               │  ├─ com.vaadin:vaadin-lumo-theme:23.3.7
               │  ├─ com.vaadin:vaadin-material-theme:23.3.7
               │  ├─ com.vaadin:vaadin-accordion-flow:23.3.7
               │  ├─ com.vaadin:vaadin-avatar-flow:23.3.7
               │  │  └─ com.vaadin:vaadin-flow-components-base:23.3.7
               │  ├─ com.vaadin:vaadin-button-flow:23.3.7
               │  ├─ com.vaadin:vaadin-checkbox-flow:23.3.7
               │  ├─ com.vaadin:vaadin-combo-box-flow:23.3.7
               │  ├─ com.vaadin:vaadin-confirm-dialog-flow:23.3.7
               │  ├─ com.vaadin:vaadin-custom-field-flow:23.3.7
               │  ├─ com.vaadin:vaadin-date-picker-flow:23.3.7
               │  ├─ com.vaadin:vaadin-date-time-picker-flow:23.3.7
               │  ├─ com.vaadin:vaadin-details-flow:23.3.7
               │  ├─ com.vaadin:vaadin-time-picker-flow:23.3.7
               │  ├─ com.vaadin:vaadin-select-flow:23.3.7
               │  ├─ com.vaadin:vaadin-dialog-flow:23.3.7
               │  ├─ com.vaadin:vaadin-form-layout-flow:23.3.7
               │  ├─ com.vaadin:vaadin-field-highlighter-flow:23.3.7
               │  ├─ com.vaadin:vaadin-grid-flow:23.3.7
               │  │  └─ org.apache.commons:commons-lang3:3.12.0
               │  ├─ com.vaadin:vaadin-icons-flow:23.3.7
               │  ├─ com.vaadin:vaadin-iron-list-flow:23.3.7
               │  ├─ com.vaadin:vaadin-virtual-list-flow:23.3.7
               │  ├─ com.vaadin:vaadin-list-box-flow:23.3.7
               │  ├─ com.vaadin:vaadin-login-flow:23.3.7
               │  ├─ com.vaadin:vaadin-messages-flow:23.3.7
               │  ├─ com.vaadin:vaadin-ordered-layout-flow:23.3.7
               │  ├─ com.vaadin:vaadin-progress-bar-flow:23.3.7
               │  ├─ com.vaadin:vaadin-radio-button-flow:23.3.7
               │  ├─ com.vaadin:vaadin-renderer-flow:23.3.7
               │  ├─ com.vaadin:vaadin-split-layout-flow:23.3.7
               │  ├─ com.vaadin:vaadin-tabs-flow:23.3.7
               │  ├─ com.vaadin:vaadin-text-field-flow:23.3.7
               │  ├─ com.vaadin:vaadin-upload-flow:23.3.7
               │  ├─ com.vaadin:vaadin-notification-flow:23.3.7
               │  ├─ com.vaadin:vaadin-app-layout-flow:23.3.7
               │  ├─ com.vaadin:vaadin-context-menu-flow:23.3.7
               │  └─ com.vaadin:vaadin-menu-bar-flow:23.3.7
               ├─ com.vaadin:vaadin-board-flow:23.3.7
               ├─ com.vaadin:vaadin-charts-flow:23.3.7
               ├─ com.vaadin:vaadin-cookie-consent-flow:23.3.7
               ├─ com.vaadin:vaadin-crud-flow:23.3.7
               ├─ com.vaadin:vaadin-grid-pro-flow:23.3.7
               ├─ com.vaadin:vaadin-map-flow:23.3.7
               ├─ com.vaadin:vaadin-rich-text-editor-flow:23.3.7
               └─ com.vaadin:collaboration-engine:5.3.0
                  ├─ com.fasterxml.jackson.core:jackson-databind:2.14.1
                  │  └─ com.fasterxml.jackson.core:jackson-annotations:2.14.1
                  ├─ com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.1
                  └─ com.vaadin:license-checker:1.5.1
                     ├─ com.github.oshi:oshi-core:6.1.6
                     │  ├─ net.java.dev.jna:jna:5.11.0
                     │  └─ net.java.dev.jna:jna-platform:5.11.0
                     └─ com.auth0:java-jwt:3.19.2
            """, dependencies.generateTransitiveDependencyTree(ArtifactRetriever.instance(), List.of(MAVEN_CENTRAL), compile));
    }

    @Test
    void testGenerateDependencyTreeVarious() {
        var dependencies = new DependencySet()
            .include(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1,5,20)))
            .include(new Dependency("com.stripe", "stripe-java", new VersionNumber(20,136,0)))
            .include(new Dependency("org.json", "json", new VersionNumber(20230227)))
            .include(new Dependency("com.itextpdf", "itext7-core", new VersionNumber(7,2,5)))
            .include(new Dependency("org.slf4j", "slf4j-simple", new VersionNumber(2,0,7)))
            .include(new Dependency("org.apache.thrift", "libthrift", new VersionNumber(0,17,0)))
            .include(new Dependency("commons-codec", "commons-codec", new VersionNumber(1,15)))
            .include(new Dependency("org.apache.httpcomponents", "httpcore", new VersionNumber(4,4,16)))
            .include(new Dependency("com.google.zxing", "javase", new VersionNumber(3,5,1)));
        assertEquals("""
            ├─ com.uwyn.rife2:rife2:1.5.20
            ├─ com.stripe:stripe-java:20.136.0
            ├─ org.json:json:20230227
            ├─ com.itextpdf:itext7-core:7.2.5
            │  ├─ com.itextpdf:barcodes:7.2.5
            │  ├─ com.itextpdf:font-asian:7.2.5
            │  ├─ com.itextpdf:forms:7.2.5
            │  ├─ com.itextpdf:hyph:7.2.5
            │  ├─ com.itextpdf:io:7.2.5
            │  │  └─ com.itextpdf:commons:7.2.5
            │  ├─ com.itextpdf:kernel:7.2.5
            │  │  ├─ org.bouncycastle:bcpkix-jdk15on:1.70
            │  │  │  └─ org.bouncycastle:bcutil-jdk15on:1.70
            │  │  └─ org.bouncycastle:bcprov-jdk15on:1.70
            │  ├─ com.itextpdf:layout:7.2.5
            │  ├─ com.itextpdf:pdfa:7.2.5
            │  ├─ com.itextpdf:sign:7.2.5
            │  ├─ com.itextpdf:styled-xml-parser:7.2.5
            │  └─ com.itextpdf:svg:7.2.5
            ├─ org.slf4j:slf4j-simple:2.0.7
            │  └─ org.slf4j:slf4j-api:2.0.7
            ├─ org.apache.thrift:libthrift:0.17.0
            ├─ commons-codec:commons-codec:1.15
            ├─ org.apache.httpcomponents:httpcore:4.4.16
            └─ com.google.zxing:javase:3.5.1
               ├─ com.google.zxing:core:3.5.1
               └─ com.beust:jcommander:1.82
            """, dependencies.generateTransitiveDependencyTree(ArtifactRetriever.instance(), List.of(MAVEN_CENTRAL), compile));
    }

    @Test
    void testGenerateDependencyTreeCompileRuntime() {
        var dependencies = new DependencySet()
            .include(new Dependency("net.thauvin.erik", "bitly-shorten", new VersionNumber(0, 9, 4, "SNAPSHOT")));
        assertEquals("""
            └─ net.thauvin.erik:bitly-shorten:0.9.4-SNAPSHOT
               ├─ org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.8.20
               │  ├─ org.jetbrains.kotlin:kotlin-stdlib:1.8.20
               │  │  ├─ org.jetbrains.kotlin:kotlin-stdlib-common:1.8.20
               │  │  └─ org.jetbrains:annotations:13.0
               │  └─ org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.8.20
               ├─ com.squareup.okhttp3:okhttp:4.10.0
               │  └─ com.squareup.okio:okio-jvm:3.0.0
               ├─ com.squareup.okhttp3:logging-interceptor:4.10.0
               └─ org.json:json:20230227
            """, dependencies.generateTransitiveDependencyTree(ArtifactRetriever.instance(), List.of(MAVEN_CENTRAL, SONATYPE_SNAPSHOTS_LEGACY), compile, runtime));
    }
}
