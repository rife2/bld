/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.*;

import static org.junit.jupiter.api.Assertions.*;

public class TestPomBuilder {
    @Test
    void testInstantiation() {
        var builder = new PomBuilder();
        assertNull(builder.info());
        assertTrue(builder.dependencies().isEmpty());
    }

    @Test
    void testEmptyBuild() {
        var builder = new PomBuilder();
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId></groupId>
              <artifactId></artifactId>
              <version></version>
              <name></name>
              <description></description>
              <url></url>
            </project>
            """, builder.build());
    }

    @Test
    void testMainInfoBuild() {
        var builder = new PomBuilder()
            .info(new PublishInfo()
                .groupId("com.example")
                .artifactId("myapp")
                .version(VersionNumber.parse("1.2.3-SNAPSHOT"))
                .name("the thing")
                .description("the thing but longer")
                .url("https://the.thing"));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>myapp</artifactId>
              <version>1.2.3-SNAPSHOT</version>
              <name>the thing</name>
              <description>the thing but longer</description>
              <url>https://the.thing</url>
            </project>
            """, builder.build());
    }

    @Test
    void testLicensesInfoBuild() {
        var builder = new PomBuilder()
            .info(new PublishInfo()
                .license(new PublishLicense().name("license1").url("https://license1.com"))
                .license(new PublishLicense().name("license2").url("https://license2.com")));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId></groupId>
              <artifactId></artifactId>
              <version></version>
              <name></name>
              <description></description>
              <url></url>
              <licenses>
                <license>
                  <name>license1</name>
                  <url>https://license1.com</url>
                </license>
                <license>
                  <name>license2</name>
                  <url>https://license2.com</url>
                </license>
              </licenses>
            </project>
            """, builder.build());
    }

    @Test
    void testDevelopersInfoBuild() {
        var builder = new PomBuilder()
            .info(new PublishInfo()
                .developer(new PublishDeveloper().id("id1").name("name1").email("email1").url("url1"))
                .developer(new PublishDeveloper().id("id2").name("name2"))
                .developer(new PublishDeveloper().id("id3").name("name3").url("url3")));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId></groupId>
              <artifactId></artifactId>
              <version></version>
              <name></name>
              <description></description>
              <url></url>
              <developers>
                <developer>
                  <id>id1</id>
                  <name>name1</name>
                  <email>email1</email>
                  <url>url1</url>
                </developer>
                <developer>
                  <id>id2</id>
                  <name>name2</name>
                  <email></email>
                  <url></url>
                </developer>
                <developer>
                  <id>id3</id>
                  <name>name3</name>
                  <email></email>
                  <url>url3</url>
                </developer>
              </developers>
            </project>
            """, builder.build());
    }

    @Test
    void testScmInfoBuild() {
        var builder = new PomBuilder()
            .info(new PublishInfo()
                .scm(new PublishScm().connection("conn1").developerConnection("devconn1").url("url1")));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId></groupId>
              <artifactId></artifactId>
              <version></version>
              <name></name>
              <description></description>
              <url></url>
              <scm>
                <connection>conn1</connection>
                <developerConnection>devconn1</developerConnection>
                <url>url1</url>
              </scm>
            </project>
            """, builder.build());
    }

    @Test
    void testFullInfoBuild() {
        var builder = new PomBuilder()
            .info(new PublishInfo()
                .groupId("com.example")
                .artifactId("myapp")
                .version(VersionNumber.parse("1.2.3-SNAPSHOT"))
                .name("the thing")
                .description("the thing but longer")
                .url("https://the.thing")
                .license(new PublishLicense().name("license1").url("https://license1.com"))
                .license(new PublishLicense().name("license2").url("https://license2.com"))
                .developer(new PublishDeveloper().id("id1").name("name1").email("email1").url("url1"))
                .developer(new PublishDeveloper().id("id2").name("name2"))
                .developer(new PublishDeveloper().id("id3").name("name3").url("url3"))
                .scm(new PublishScm().connection("conn1").developerConnection("devconn1").url("url1")));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>myapp</artifactId>
              <version>1.2.3-SNAPSHOT</version>
              <name>the thing</name>
              <description>the thing but longer</description>
              <url>https://the.thing</url>
              <licenses>
                <license>
                  <name>license1</name>
                  <url>https://license1.com</url>
                </license>
                <license>
                  <name>license2</name>
                  <url>https://license2.com</url>
                </license>
              </licenses>
              <developers>
                <developer>
                  <id>id1</id>
                  <name>name1</name>
                  <email>email1</email>
                  <url>url1</url>
                </developer>
                <developer>
                  <id>id2</id>
                  <name>name2</name>
                  <email></email>
                  <url></url>
                </developer>
                <developer>
                  <id>id3</id>
                  <name>name3</name>
                  <email></email>
                  <url>url3</url>
                </developer>
              </developers>
              <scm>
                <connection>conn1</connection>
                <developerConnection>devconn1</developerConnection>
                <url>url1</url>
              </scm>
            </project>
            """, builder.build());
    }

    @Test
    void testDependenciesCompile() {
        var builder = new PomBuilder();
        builder.dependencies().scope(Scope.compile)
            .include(new Dependency("com.uwyn.rife2", "rife2"))
            .include(new Dependency("com.uwyn.rife2", "rife2", VersionNumber.UNKNOWN, "agent"))
            .include(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 5, 5), "bld", "zip"))
            .include(new Dependency("org.eclipse.jetty", "jetty-server", new VersionNumber(11, 0, 14))
                .exclude("*", "*").exclude("groupId", "artifactId"))
            .include(new Dependency("org.springframework.boot", "spring-boot-starter", new VersionNumber(3, 0, 4))
                .exclude("*", "artifactId"));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId></groupId>
              <artifactId></artifactId>
              <version></version>
              <name></name>
              <description></description>
              <url></url>
              <dependencies>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <scope>compile</scope>
                </dependency>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <classifier>agent</classifier>
                  <scope>compile</scope>
                </dependency>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <version>1.5.5</version>
                  <type>zip</type>
                  <classifier>bld</classifier>
                  <scope>compile</scope>
                </dependency>
                <dependency>
                  <groupId>org.eclipse.jetty</groupId>
                  <artifactId>jetty-server</artifactId>
                  <version>11.0.14</version>
                  <scope>compile</scope>
                  <exclusions>
                    <exclusion>
                      <groupId>*</groupId>
                      <artifactId>*</artifactId>
                    </exclusion>
                    <exclusion>
                      <groupId>groupId</groupId>
                      <artifactId>artifactId</artifactId>
                    </exclusion>
                  </exclusions>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter</artifactId>
                  <version>3.0.4</version>
                  <scope>compile</scope>
                  <exclusions>
                    <exclusion>
                      <groupId>*</groupId>
                      <artifactId>artifactId</artifactId>
                    </exclusion>
                  </exclusions>
                </dependency>
              </dependencies>
            </project>
            """, builder.build());
    }

    @Test
    void testDependenciesRuntime() {
        var builder = new PomBuilder();
        builder.dependencies().scope(Scope.runtime)
            .include(new Dependency("com.uwyn.rife2", "rife2"))
            .include(new Dependency("com.uwyn.rife2", "rife2", VersionNumber.UNKNOWN, "agent"))
            .include(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 5, 5), "bld", "zip"))
            .include(new Dependency("org.eclipse.jetty", "jetty-server", new VersionNumber(11, 0, 14)))
            .include(new Dependency("org.springframework.boot", "spring-boot-starter", new VersionNumber(3, 0, 4)));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId></groupId>
              <artifactId></artifactId>
              <version></version>
              <name></name>
              <description></description>
              <url></url>
              <dependencies>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <scope>runtime</scope>
                </dependency>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <classifier>agent</classifier>
                  <scope>runtime</scope>
                </dependency>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <version>1.5.5</version>
                  <type>zip</type>
                  <classifier>bld</classifier>
                  <scope>runtime</scope>
                </dependency>
                <dependency>
                  <groupId>org.eclipse.jetty</groupId>
                  <artifactId>jetty-server</artifactId>
                  <version>11.0.14</version>
                  <scope>runtime</scope>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter</artifactId>
                  <version>3.0.4</version>
                  <scope>runtime</scope>
                </dependency>
              </dependencies>
            </project>
            """, builder.build());
    }

    @Test
    void testDependencies() {
        var builder = new PomBuilder();
        builder.dependencies().scope(Scope.compile)
            .include(new Dependency("com.uwyn.rife2", "rife2"))
            .include(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 5, 5), "bld", "zip"))
            .include(new Dependency("org.springframework.boot", "spring-boot-starter", new VersionNumber(3, 0, 4))
                .exclude("*", "artifactId"));
        builder.dependencies().scope(Scope.runtime)
            .include(new Dependency("com.uwyn.rife2", "rife2", VersionNumber.UNKNOWN, "agent"))
            .include(new Dependency("org.eclipse.jetty", "jetty-server", new VersionNumber(11, 0, 14))
                .exclude("*", "*").exclude("groupId", "artifactId"));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId></groupId>
              <artifactId></artifactId>
              <version></version>
              <name></name>
              <description></description>
              <url></url>
              <dependencies>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <scope>compile</scope>
                </dependency>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <version>1.5.5</version>
                  <type>zip</type>
                  <classifier>bld</classifier>
                  <scope>compile</scope>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter</artifactId>
                  <version>3.0.4</version>
                  <scope>compile</scope>
                  <exclusions>
                    <exclusion>
                      <groupId>*</groupId>
                      <artifactId>artifactId</artifactId>
                    </exclusion>
                  </exclusions>
                </dependency>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <classifier>agent</classifier>
                  <scope>runtime</scope>
                </dependency>
                <dependency>
                  <groupId>org.eclipse.jetty</groupId>
                  <artifactId>jetty-server</artifactId>
                  <version>11.0.14</version>
                  <scope>runtime</scope>
                  <exclusions>
                    <exclusion>
                      <groupId>*</groupId>
                      <artifactId>*</artifactId>
                    </exclusion>
                    <exclusion>
                      <groupId>groupId</groupId>
                      <artifactId>artifactId</artifactId>
                    </exclusion>
                  </exclusions>
                </dependency>
              </dependencies>
            </project>
            """, builder.build());
    }

    @Test
    void testComplete() {
        var builder = new PomBuilder()
            .info(new PublishInfo()
                .groupId("com.example")
                .artifactId("myapp")
                .version(VersionNumber.parse("1.2.3-SNAPSHOT"))
                .name("the thing")
                .description("the thing but longer")
                .url("https://the.thing")
                .license(new PublishLicense().name("license1").url("https://license1.com"))
                .license(new PublishLicense().name("license2").url("https://license2.com"))
                .developer(new PublishDeveloper().id("id1").name("name1").email("email1").url("url1"))
                .developer(new PublishDeveloper().id("id2").name("name2"))
                .developer(new PublishDeveloper().id("id3").name("name3").url("url3"))
                .scm(new PublishScm().connection("conn1").developerConnection("devconn1").url("url1")));
        builder.dependencies().scope(Scope.compile)
            .include(new Dependency("com.uwyn.rife2", "rife2"))
            .include(new Dependency("com.uwyn.rife2", "rife2", new VersionNumber(1, 5, 5), "bld", "zip"))
            .include(new Dependency("org.springframework.boot", "spring-boot-starter", new VersionNumber(3, 0, 4))
                .exclude("*", "artifactId"));
        builder.dependencies().scope(Scope.runtime)
            .include(new Dependency("com.uwyn.rife2", "rife2", VersionNumber.UNKNOWN, "agent"))
            .include(new Dependency("org.eclipse.jetty", "jetty-server", new VersionNumber(11, 0, 14))
                .exclude("*", "*").exclude("groupId", "artifactId"));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd" xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <modelVersion>4.0.0</modelVersion>
              <groupId>com.example</groupId>
              <artifactId>myapp</artifactId>
              <version>1.2.3-SNAPSHOT</version>
              <name>the thing</name>
              <description>the thing but longer</description>
              <url>https://the.thing</url>
              <licenses>
                <license>
                  <name>license1</name>
                  <url>https://license1.com</url>
                </license>
                <license>
                  <name>license2</name>
                  <url>https://license2.com</url>
                </license>
              </licenses>
              <dependencies>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <scope>compile</scope>
                </dependency>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <version>1.5.5</version>
                  <type>zip</type>
                  <classifier>bld</classifier>
                  <scope>compile</scope>
                </dependency>
                <dependency>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter</artifactId>
                  <version>3.0.4</version>
                  <scope>compile</scope>
                  <exclusions>
                    <exclusion>
                      <groupId>*</groupId>
                      <artifactId>artifactId</artifactId>
                    </exclusion>
                  </exclusions>
                </dependency>
                <dependency>
                  <groupId>com.uwyn.rife2</groupId>
                  <artifactId>rife2</artifactId>
                  <classifier>agent</classifier>
                  <scope>runtime</scope>
                </dependency>
                <dependency>
                  <groupId>org.eclipse.jetty</groupId>
                  <artifactId>jetty-server</artifactId>
                  <version>11.0.14</version>
                  <scope>runtime</scope>
                  <exclusions>
                    <exclusion>
                      <groupId>*</groupId>
                      <artifactId>*</artifactId>
                    </exclusion>
                    <exclusion>
                      <groupId>groupId</groupId>
                      <artifactId>artifactId</artifactId>
                    </exclusion>
                  </exclusions>
                </dependency>
              </dependencies>
              <developers>
                <developer>
                  <id>id1</id>
                  <name>name1</name>
                  <email>email1</email>
                  <url>url1</url>
                </developer>
                <developer>
                  <id>id2</id>
                  <name>name2</name>
                  <email></email>
                  <url></url>
                </developer>
                <developer>
                  <id>id3</id>
                  <name>name3</name>
                  <email></email>
                  <url>url3</url>
                </developer>
              </developers>
              <scm>
                <connection>conn1</connection>
                <developerConnection>devconn1</developerConnection>
                <url>url1</url>
              </scm>
            </project>
            """, builder.build());
    }
}
