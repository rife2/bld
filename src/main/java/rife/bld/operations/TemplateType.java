/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import java.io.Serial;
import java.io.Serializable;

/**
 * Allows template types to be specified for pre-compilation.
 */
public class TemplateType implements Serializable {
    @Serial private static final long serialVersionUID = -2736320275307140837L;

    /**
     * The {@code html} template type.
     */
    public static TemplateType HTML = new TemplateType("html");
    /**
     * The {@code json} template type.
     */
    public static TemplateType JSON = new TemplateType("json");
    /**
     * The {@code svg} template type.
     */
    public static TemplateType SVG = new TemplateType("svg");
    /**
     * The {@code xml} template type.
     */
    public static TemplateType XML = new TemplateType("xml");
    /**
     * The {@code txt} template type.
     */
    public static TemplateType TXT = new TemplateType("txt");
    /**
     * The {@code sql} template type.
     */
    public static TemplateType SQL = new TemplateType("sql");

    private final String identifier_;

    /**
     * Creates a new template type instance.
     *
     * @param identifier the identifier of this template type
     */
    public TemplateType(String identifier) {
        identifier_ = identifier;
    }

    /**
     * Retrieves the identifier for this template type
     * @return the template type identifier as a string
     */
    public String identifier() {
        return identifier_;
    }
}