/*
 * Copyright 2001-2026 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

public class TestInstrumentOperation {
    @Test
    void testInstantiation() {
        var operation = new InstrumentOperation();
        assertEquals("java", operation.javaTool());
        assertTrue(operation.javaOptions().isEmpty());
        assertTrue(operation.classpath().isEmpty());
        assertTrue(operation.sourceDirectories().isEmpty());
        assertNull(operation.destinationDirectory());
    }

    @Test
    void testPopulation() {
        var source1 = new File("source1");
        var source2 = new File("source2");
        var destination = new File("destination");

        var operation = new InstrumentOperation()
            .classpath("classpath1", "classpath2")
            .sourceDirectories(source1, source2)
            .destinationDirectory(destination);
        assertEquals(2, operation.classpath().size());
        assertEquals(2, operation.sourceDirectories().size());
        assertEquals(destination, operation.destinationDirectory());
    }

    @Test
    void testProcessCommandList() {
        var operation = new InstrumentOperation()
            .classpath("cp1", "cp2")
            .sourceDirectories(new File("classes"), new File("more-classes"))
            .destinationDirectory(new File("instrumented"));

        var command = operation.executeConstructProcessCommandList();
        assertEquals("java", command.get(0));
        assertTrue(command.contains("-cp"));
        assertTrue(command.contains("cp1" + File.pathSeparator + "cp2"));
        assertTrue(command.contains(InstrumentOperation.DEPLOYER_CLASS));
        assertTrue(command.contains("-verbose"));
        var d_index = command.indexOf("-d");
        assertTrue(d_index > 0);
        assertEquals(new File("instrumented").getAbsolutePath(), command.get(d_index + 1));
        assertEquals(new File("classes").getAbsolutePath(), command.get(command.size() - 2));
        assertEquals(new File("more-classes").getAbsolutePath(), command.get(command.size() - 1));
    }
}
