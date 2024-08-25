/*
 * Copyright 2001-2024 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.instrument;

import rife.asm.*;

/**
 * This utility class will modify a Java module {@code module-info.class} to add
 * a module main class to its attributes.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 2.1
 */
public class ModuleMainClassAdapter extends ClassVisitor implements Opcodes {
    private final String mainClass_;

    /**
     * Performs the actual modification of the module info class's bytecode.
     *
     * @param origBytes the bytes of the module class that should be modified
     * @param mainClass the main class of the module
     * @return the modified bytes
     * @since 2.1
     */
    public static byte[] addModuleMainClassToBytes(byte[] origBytes, String mainClass) {
        var cw = new ClassWriter(0);
        new ClassReader(origBytes).accept(new ModuleMainClassAdapter(mainClass, cw), 0);
        return cw.toByteArray();
    }

    private ModuleMainClassAdapter(String mainClass, ClassVisitor writer) {
        super(ASM9, writer);
        mainClass_ = mainClass.replace('.', '/');
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        var module_visitor = super.visitModule(name, access, version);
        module_visitor.visitMainClass(mainClass_);
        return module_visitor;
    }
}