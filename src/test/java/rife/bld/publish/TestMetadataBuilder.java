/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.publish;

import org.junit.jupiter.api.Test;
import rife.bld.dependencies.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestMetadataBuilder {
    @Test
    void testInstantiation() {
        var builder = new MetadataBuilder();
        assertNull(builder.info());
        assertNull(builder.updated());
    }

    @Test
    void testEmptyBuild() {
        var builder = new MetadataBuilder();
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId></groupId>
              <artifactId></artifactId>
              <versioning>
              </versioning>
            </metadata>
            """, builder.build());
    }

    @Test
    void testMainInfoBuild() {
        var builder = new MetadataBuilder()
            .info(new PublishInfo()
                .groupId("com.example")
                .artifactId("myapp")
                .version(VersionNumber.parse("1.2.3-SNAPSHOT")));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId>com.example</groupId>
              <artifactId>myapp</artifactId>
              <versioning>
                <latest>1.2.3-SNAPSHOT</latest>
                <release>1.2.3-SNAPSHOT</release>
                <versions>
                  <version>1.2.3-SNAPSHOT</version>
                </versions>
              </versioning>
            </metadata>
            """, builder.build());
    }

    @Test
    void testUpdatedBuild() {
        var builder = new MetadataBuilder()
            .updated(ZonedDateTime.of(2023, 3, 27, 8, 56, 17, 123, ZoneId.of("America/New_York")));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId></groupId>
              <artifactId></artifactId>
              <versioning>
                <lastUpdated>20230327125617</lastUpdated>
              </versioning>
            </metadata>
            """, builder.build());
    }

    @Test
    void testOtherVersionsBuild() {
        var builder = new MetadataBuilder()
            .otherVersions(List.of(new VersionNumber(6,0,1), new VersionNumber(3,0,2), new VersionNumber(1,0,3), new VersionNumber(3,0,2)));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId></groupId>
              <artifactId></artifactId>
              <versioning>
                <versions>
                  <version>1.0.3</version>
                  <version>3.0.2</version>
                  <version>6.0.1</version>
                </versions>
              </versioning>
            </metadata>
            """, builder.build());
    }

    @Test
    void testCompleteBuild() {
        var builder = new MetadataBuilder()
            .info(new PublishInfo()
                .groupId("com.example")
                .artifactId("myapp")
                .version(VersionNumber.parse("1.2.3-SNAPSHOT")))
            .otherVersions(List.of(new VersionNumber(6,0,1), new VersionNumber(3,0,2), new VersionNumber(1,0,3), new VersionNumber(3,0,2)))
            .updated(ZonedDateTime.of(2023, 3, 27, 8, 56, 17, 123, ZoneId.of("America/New_York")));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId>com.example</groupId>
              <artifactId>myapp</artifactId>
              <versioning>
                <latest>1.2.3-SNAPSHOT</latest>
                <release>1.2.3-SNAPSHOT</release>
                <versions>
                  <version>1.0.3</version>
                  <version>1.2.3-SNAPSHOT</version>
                  <version>3.0.2</version>
                  <version>6.0.1</version>
                </versions>
                <lastUpdated>20230327125617</lastUpdated>
              </versioning>
            </metadata>
            """, builder.build());
    }

    @Test
    void testSnapshot() {
        var builder = new MetadataBuilder()
            .info(new PublishInfo()
                .groupId("com.example")
                .artifactId("myapp")
                .version(VersionNumber.parse("1.2.3-SNAPSHOT")))
            .snapshot(ZonedDateTime.of(2023, 3, 27, 8, 56, 17, 123, ZoneId.of("America/New_York")), 5);
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId>com.example</groupId>
              <artifactId>myapp</artifactId>
              <version>1.2.3-SNAPSHOT</version>
              <versioning>
                <snapshot>
                  <timestamp>20230327.125617</timestamp>
                  <buildNumber>5</buildNumber>
                </snapshot>
              </versioning>
            </metadata>
            """, builder.build());
    }

    @Test
    void testSnapshotVersions() {
        var moment = ZonedDateTime.of(2023, 3, 27, 8, 56, 17, 123, ZoneId.of("America/New_York"));
        var moment2 = ZonedDateTime.of(2023, 3, 27, 8, 56, 17, 123, ZoneId.of("America/New_York"));
        var moment3 = ZonedDateTime.of(2023, 5, 27, 8, 56, 17, 123, ZoneId.of("America/New_York"));
        var builder = new MetadataBuilder()
            .info(new PublishInfo()
                .groupId("com.example")
                .artifactId("myapp")
                .version(VersionNumber.parse("1.2.3-SNAPSHOT")))
            .snapshot(moment, 5)
            .snapshotVersions(List.of(
                new SnapshotVersion("classifier1", "ext1", "123", moment2),
                new SnapshotVersion("classifier2", "ext2", "456", moment3),
                new SnapshotVersion("classifier3", "ext3", "789", moment2)));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId>com.example</groupId>
              <artifactId>myapp</artifactId>
              <version>1.2.3-SNAPSHOT</version>
              <versioning>
                <snapshot>
                  <timestamp>20230327.125617</timestamp>
                  <buildNumber>5</buildNumber>
                </snapshot>
                <snapshotVersions>
                  <snapshotVersion>
                    <classifier>classifier1</classifier>
                    <extension>ext1</extension>
                    <value>123</value>
                    <updated>20230327125617</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>classifier2</classifier>
                    <extension>ext2</extension>
                    <value>456</value>
                    <updated>20230527125617</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>classifier3</classifier>
                    <extension>ext3</extension>
                    <value>789</value>
                    <updated>20230327125617</updated>
                  </snapshotVersion>
                </snapshotVersions>
              </versioning>
            </metadata>
            """, builder.build());
    }

    @Test
    void testLocalSnapshotVersions() {
        var moment = ZonedDateTime.of(2023, 3, 27, 8, 56, 17, 123, ZoneId.of("America/New_York"));
        var moment2 = ZonedDateTime.of(2023, 3, 27, 8, 56, 17, 123, ZoneId.of("America/New_York"));
        var moment3 = ZonedDateTime.of(2023, 5, 27, 8, 56, 17, 123, ZoneId.of("America/New_York"));
        var builder = new MetadataBuilder()
            .info(new PublishInfo()
                .groupId("com.example")
                .artifactId("myapp")
                .version(VersionNumber.parse("1.2.3-SNAPSHOT")))
            .snapshotLocal()
            .snapshotVersions(List.of(
                new SnapshotVersion("classifier1", "ext1", "123", moment2),
                new SnapshotVersion("classifier2", "ext2", "456", moment3),
                new SnapshotVersion("classifier3", "ext3", "789", moment2)));
        assertEquals("""
            <?xml version="1.0" encoding="UTF-8"?>
            <metadata modelVersion="1.1.0">
              <groupId>com.example</groupId>
              <artifactId>myapp</artifactId>
              <version>1.2.3-SNAPSHOT</version>
              <versioning>
                <snapshot>
                  <localCopy>true</localCopy>
                </snapshot>
                <snapshotVersions>
                  <snapshotVersion>
                    <classifier>classifier1</classifier>
                    <extension>ext1</extension>
                    <value>123</value>
                    <updated>20230327125617</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>classifier2</classifier>
                    <extension>ext2</extension>
                    <value>456</value>
                    <updated>20230527125617</updated>
                  </snapshotVersion>
                  <snapshotVersion>
                    <classifier>classifier3</classifier>
                    <extension>ext3</extension>
                    <value>789</value>
                    <updated>20230327125617</updated>
                  </snapshotVersion>
                </snapshotVersions>
              </versioning>
            </metadata>
            """, builder.build());
    }
}
