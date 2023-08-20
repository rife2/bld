[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/1.7.1-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://img.shields.io/github/release/rife2/bld.svg)](https://github.com/rife2/bld/releases/latest)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.uwyn.rife2/bld/badge.svg?color=blue)](https://maven-badges.herokuapp.com/maven-central/com.uwyn.rife2/bld)
[![Nexus Snapshot](https://img.shields.io/nexus/s/com.uwyn.rife2/bld?server=https%3A%2F%2Fs01.oss.sonatype.org%2F)](https://s01.oss.sonatype.org/content/repositories/snapshots/com/uwyn/rife2/bld/)
[![gradle-ci](https://github.com/rife2/bld/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld/actions/workflows/bld.yml)
[![Tests](https://rife2.com/tests-badge/badge/com.uwyn.rife2/bld)](https://github.com/rife2/rife2/actions/workflows/bld.yml)

<br>

<p align="center"><img src="https://github.com/rife2/bld/raw/main/images/bld_logo.png" width="120"></p>

# What is bld?

`bld` is a new build system that allows you to write your build logic in pure
Java.

`bld` was created because we're not really interested in build tools. We use
them because we have to, but we'd rather just get on with coding the real stuff.

`bld` is designed with the following principles in mind:

* tasks don't happen without you telling them to happen
* no auto-magical behavior, task behavior is explicit and API-defined
* managing libs yourself is fine, having that automated also, or mix and match
* build logic is written in Java, with all the advantages of Java
* standard collection of Java-centric tasks for common operations
* bld is distributed in a single jar, if you have the jar, you have the build system

# Designed for modern Java

bld relies on Java 17 and leverages many of the features that this version of
Java provides. Thanks to the modern language constructs, your Java build logic
ends up looking very concise, is easily readable and understood by any IDE.
You automatically get support for auto-completion and javadoc documentation,
and you can split your build logic into multiple files and classes when you outgrow a single file.

Here is a complete bld file for a Java application using JUnit 5 for its tests.
Nothing else is needed to be able to run it, test it and distribute it:

```java
package com.example;

import rife.bld.Project;
import java.util.List;
import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;

public class MyappBuild extends Project {
    public MyappBuild() {
        pkg = "com.example";
        name = "Myapp";
        mainClass = "com.example.MyappMain";
        version = version(0,1,0);

        downloadSources = true;
        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);
        scope(test)
            .include(dependency("org.junit.jupiter",
                                "junit-jupiter",
                                version(5,9,2)))
            .include(dependency("org.junit.platform",
                                "junit-platform-console-standalone",
                                version(1,9,2)));
    }

    public static void main(String[] args) {
        new MyappBuild().start(args);
    }
}
```


> **NOTE:** `bld` supports different ways to describe dependencies,
> `dependency("com.uwyn.rife2", "rife2", version(1,7,0))` can for instance also
> be written as `dependency("com.uwyn.rife2:rife2:1.7.0")`. Which format you use,
> is a matter of personal taste.

# Where does `bld` fit?

From a very high level, build tools can be organized in a matrix:
* either your tool is declarative or in code
* either your tool first describes a plan or immediately executes a plan


|        | Declarative | Code | Describes | Immediate |
|--------|-------------|------|-----------|-----------|
| Maven  | X           |      | X         |           |
| Gradle |             | X    | X         |           |
| `bld`  |             | X    |           | X         |

Writing your build logic in the same language as your application (Java),
significantly reduces the cognitive load, and taking actions immediately
without having to mentally construct a described plan, makes it easier to
reason about your build.

# Find out more

`bld` lets your build logic get out of the way so that you can focus on writing
applications.

If you have any questions, suggestions, ideas or just want to chat, feel free
to post on the [forums](https://forum.uwyn.com) or to join
us on [Discord](https://discord.gg/zDG6anEXQX).

**Read more in the [full documentation](https://github.com/rife2/bld/wiki)
and [bld Javadocs](https://rife2.github.io/bld/).**
