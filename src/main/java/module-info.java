/**
 * bld is a new build system that allows you to write your build logic in pure Java.
 * <p>
 * More information can be found on the <a href="https://rife2.com/bld">`bld` website</a>.
 *
 * @since 2.1.0
 */
module rife.bld {
    requires java.compiler;
    requires java.desktop;
    requires java.logging;
    requires java.net.http;
    requires java.prefs;
    requires static java.sql;
    requires java.xml;

    exports rife.bld;
    exports rife.bld.blueprints;
    exports rife.bld.dependencies;
    exports rife.bld.dependencies.exceptions;
    exports rife.bld.help;
    exports rife.bld.operations;
    exports rife.bld.operations.exceptions;
    exports rife.bld.publish;

    exports rife;
    exports rife.cmf;
    exports rife.cmf.transform;
    exports rife.config;
    exports rife.config.exceptions;
    exports rife.database;
    exports rife.database.exceptions;
    exports rife.database.queries;
    exports rife.database.querymanagers.generic;
    exports rife.database.querymanagers.generic.exceptions;
    exports rife.database.types;
    exports rife.datastructures;
    exports rife.engine;
    exports rife.forms;
    exports rife.ioc;
    exports rife.ioc.exceptions;
    exports rife.resources;
    exports rife.resources.exceptions;
    exports rife.selector;
    exports rife.template;
    exports rife.template.exceptions;
    exports rife.tools;
    exports rife.tools.exceptions;
    exports rife.validation;
    exports rife.validation.annotations;
    exports rife.validation.exceptions;
    exports rife.xml;
    exports rife.xml.exceptions;
}