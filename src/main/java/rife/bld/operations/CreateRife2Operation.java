/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.operations;

import rife.bld.blueprints.Rife2ProjectBlueprint;
import rife.bld.dependencies.*;
import rife.template.TemplateFactory;
import rife.tools.FileUtils;
import rife.tools.exceptions.FileUtilsErrorException;

import java.io.File;
import java.io.IOException;

/**
 * Creates a new RIFE2 project structure.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5
 */
public class CreateRife2Operation extends AbstractCreateOperation<CreateRife2Operation, Rife2ProjectBlueprint> {
    File srcMainWebappCssDirectory_;
    File srcMainWebappWebInfDirectory_;

    public CreateRife2Operation() {
        super("bld.rife2_hello.");
    }

    protected Rife2ProjectBlueprint createProjectBlueprint() {
        return new Rife2ProjectBlueprint(new File(workDirectory(), projectName()), packageName(), projectName());
    }

    @Override
    protected void executeConfigure() {
        super.executeConfigure();

        projectMainName_ = projectClassName_ + "Site";
        projectMainUberName_ = projectMainName_ + "Uber";
        srcMainWebappCssDirectory_ = new File(project_.srcMainWebappDirectory(), "css");
        srcMainWebappWebInfDirectory_ = new File(project_.srcMainWebappDirectory(), "WEB-INF");
    }

    @Override
    protected void executeCreateProjectStructure() {
        super.executeCreateProjectStructure();

        srcMainWebappCssDirectory_.mkdirs();
        srcMainWebappWebInfDirectory_.mkdirs();
    }

    @Override
    protected void executePopulateProjectStructure()
    throws FileUtilsErrorException, IOException {
        super.executePopulateProjectStructure();

        // project site uber
        var main_uber_template = TemplateFactory.TXT.get(templateBase_ + "project_main_uber");
        main_uber_template.setValue("package", project_.pkg());
        main_uber_template.setValue("projectMain", projectMainName_);
        main_uber_template.setValue("projectMainUber", projectMainUberName_);
        var project_main_uber_file = new File(mainPackageDirectory_, projectMainUberName_ + ".java");
        FileUtils.writeString(main_uber_template.getContent(), project_main_uber_file);

        // project template
        var template_template = TemplateFactory.HTML.get(templateBase_ + "project_template");
        template_template.setValue("project", projectClassName_);
        var project_template_file = new File(project_.srcMainResourcesTemplatesDirectory(), "hello.html");
        FileUtils.writeString(template_template.getContent(), project_template_file);

        // project css
        FileUtils.writeString(
            TemplateFactory.TXT.get(templateBase_ + "project_style").getContent(),
            new File(srcMainWebappCssDirectory_, "style.css"));

        // project web.xml
        var web_xml_template = TemplateFactory.XML.get(templateBase_ + "project_web");
        web_xml_template.setValue("package", project_.pkg());
        web_xml_template.setValue("projectMain", projectMainName_);
        var project_web_xml_file = new File(srcMainWebappWebInfDirectory_, "web.xml");
        FileUtils.writeString(web_xml_template.getContent(), project_web_xml_file);
    }

    @Override
    protected void executePopulateIdeaProject()
    throws FileUtilsErrorException {
        super.executePopulateIdeaProject();
        FileUtils.writeString(
            TemplateFactory.XML.get(templateBase_ + "idea.libraries.standalone").getContent(),
            new File(ideaLibrariesDirectory_, "standalone.xml"));
    }
}
