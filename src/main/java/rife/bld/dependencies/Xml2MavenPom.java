/*
 * Copyright 2001-2023 Geert Bevin (gbevin[remove] at uwyn dot com)
 * Licensed under the Apache License, Version 2.0 (the "License")
 */
package rife.bld.dependencies;

import org.xml.sax.Attributes;
import rife.xml.Xml2Data;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Parses an XML document to retrieve POM information, this is an internal class.
 *
 * @author Geert Bevin (gbevin[remove] at uwyn dot com)
 * @since 1.5.18
 */
class Xml2MavenPom extends Xml2Data {
    private final Dependency parent_;
    private final ArtifactRetriever retriever_;
    private final List<Repository> repositories_;
    private Map<Scope, Set<PomDependency>> resolvedDependencies_ = null;

    private final Map<PomDependency, PomDependency> dependencyManagement_ = new LinkedHashMap<>();
    private final Set<PomDependency> dependencies_ = new LinkedHashSet<>();
    private final Map<String, String> properties_ = new HashMap<>();
    private final Stack<String> elementStack_ = new Stack<>();
    private ExclusionSet exclusions_ = null;

    private boolean collectProperties_ = false;
    private boolean collectDependencyManagement_ = false;
    private boolean collectDependencies_ = false;
    private boolean collectExclusions_ = false;

    private StringBuilder characterData_ = null;

    private String lastGroupId_ = null;
    private String lastArtifactId_ = null;
    private String lastVersion_ = null;
    private String lastType_ = null;
    private String lastClassifier_ = null;
    private String lastScope_ = null;
    private String lastOptional_ = null;
    private String lastExclusionGroupId_ = null;
    private String lastExclusionArtifactId_ = null;

    Xml2MavenPom(Dependency parent, ArtifactRetriever retriever, List<Repository> repositories) {
        parent_ = parent;
        retriever_ = retriever;
        repositories_ = repositories;
    }

    Set<PomDependency> getDependencies(Scope... scopes) {
        if (scopes == null || scopes.length == 0) {
            return Collections.emptySet();
        }

        var scopes_list = Arrays.asList(scopes);

        if (resolvedDependencies_ == null) {
            var resolved_dependencies = new HashMap<Scope, Set<PomDependency>>();

            if (!dependencies_.isEmpty()) {
                for (var dependency : dependencies_) {
                    var managed_dependency = dependencyManagement_.get(dependency);
                    var version = dependency.version();
                    var dep_scope = dependency.scope();
                    var optional = dependency.optional();
                    var exclusions = dependency.exclusions();
                    if (managed_dependency != null) {
                        if (version == null) {
                            version = managed_dependency.version();
                        }
                        if (dep_scope == null) {
                            dep_scope = managed_dependency.scope();
                        }
                        if (optional == null) {
                            optional = managed_dependency.optional();
                        }
                        if (exclusions == null) {
                            exclusions = managed_dependency.exclusions();
                        }
                    }
                    if (dep_scope == null) {
                        dep_scope = "compile";
                    }
                    optional = resolveProperties(optional);
                    if ("true".equals(optional)) {
                        continue;
                    }

                    var resolved_dependency = new PomDependency(
                        resolveProperties(dependency.groupId()),
                        resolveProperties(dependency.artifactId()),
                        resolveProperties(version),
                        resolveProperties(dependency.classifier()),
                        resolveProperties(dependency.type()),
                        dep_scope,
                        "false",
                        exclusions,
                        dependency.parent());
                    if (resolved_dependency.type() == null || resolved_dependency.type().equals("jar")) {
                        var scope = Scope.valueOf(resolved_dependency.scope());
                        if (scopes_list.contains(scope)) {
                            var resolved_dependency_set = resolved_dependencies.computeIfAbsent(scope, k -> new LinkedHashSet<>());
                            resolved_dependency_set.add(resolved_dependency);
                        }
                    }
                }
            }

            resolvedDependencies_ = resolved_dependencies;
        }

        var result = new LinkedHashSet<PomDependency>();
        for (var scope : scopes) {
            var deps = resolvedDependencies_.get(scope);
            if (deps != null) {
                result.addAll(deps);
            }
        }

        return result;
    }

    PomDependency resolveDependency(PomDependency dependency) {
        return new PomDependency(
            resolveProperties(dependency.groupId()),
            resolveProperties(dependency.artifactId()),
            resolveProperties(dependency.version()),
            resolveProperties(dependency.classifier()),
            resolveProperties(dependency.type()),
            dependency.scope(),
            resolveProperties(dependency.optional()),
            dependency.exclusions(),
            dependency.parent());
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) {
        characterData_ = new StringBuilder();

        switch (qName) {
            case "parent" -> resetState();
            case "properties" -> {
                if (isChildOfProject()) {
                    collectProperties_ = true;
                }
            }
            case "dependencyManagement" -> {
                if (isChildOfProject()) {
                    collectDependencyManagement_ = true;
                }
            }
            case "dependencies" -> {
                if (isChildOfProject()) {
                    resetState();
                    collectDependencies_ = true;
                }
            }
            case "exclusions" -> {
                if (collectDependencyManagement_ || collectDependencies_) {
                    collectExclusions_ = true;
                    exclusions_ = new ExclusionSet();
                }
            }
            case "dependency" -> {
                if (collectDependencies_) resetState();
            }
        }

        elementStack_.push(qName);
    }

    public void endElement(String uri, String localName, String qName) {
        elementStack_.pop();

        switch (qName) {
            case "parent" -> {
                if (isChildOfProject()) {
                    var parent_dependency = new Dependency(resolveProperties(lastGroupId_), resolveProperties(lastArtifactId_), VersionNumber.parse(resolveProperties(lastVersion_)));
                    var parent = new DependencyResolver(retriever_, repositories_, parent_dependency).getMavenPom(parent_);

                    parent.properties_.keySet().removeAll(properties_.keySet());
                    properties_.putAll(parent.properties_);

                    parent.dependencyManagement_.keySet().removeAll(dependencyManagement_.keySet());
                    dependencyManagement_.putAll(parent.dependencyManagement_);

                    parent.dependencies_.removeAll(dependencies_);
                    dependencies_.addAll(parent.dependencies_);

                    resetState();
                }
            }
            case "properties" -> collectProperties_ = false;
            case "dependencyManagement" -> collectDependencyManagement_ = false;
            case "dependencies" -> collectDependencies_ = false;
            case "exclusions" -> collectExclusions_ = false;
            case "exclusion" -> {
                if (collectExclusions_) {
                    exclusions_.add(new DependencyExclusion(lastExclusionGroupId_, lastExclusionArtifactId_));
                }
            }
            case "dependency" -> {
                var dependency = new PomDependency(lastGroupId_, lastArtifactId_, lastVersion_, lastClassifier_, lastType_, lastScope_, lastOptional_, exclusions_, parent_);
                if (collectDependencyManagement_) {
                    if (dependency.isPomImport()) {
                        var import_dependency = new Dependency(resolveProperties(lastGroupId_), resolveProperties(lastArtifactId_), VersionNumber.parse(resolveProperties(lastVersion_)));
                        var imported_pom = new DependencyResolver(retriever_, repositories_, import_dependency).getMavenPom(parent_);
                        imported_pom.dependencyManagement_.keySet().removeAll(dependencyManagement_.keySet());
                        var resolved_dependencies = new LinkedHashSet<PomDependency>();
                        for (var managed_dependency : imported_pom.dependencyManagement_.keySet()) {
                            resolved_dependencies.add(imported_pom.resolveDependency(managed_dependency));
                        }

                        resolved_dependencies.removeAll(dependencyManagement_.keySet());
                        for (var resolved_dependency : resolved_dependencies) {
                            dependencyManagement_.put(resolved_dependency, resolved_dependency);
                        }
                    } else {
                        dependencyManagement_.put(dependency, dependency);
                    }
                } else if (collectDependencies_) {
                    dependencies_.add(dependency);
                }
                resetState();
            }
            case "groupId" -> {
                if (isChildOfProject()) {
                    addProjectProperty(qName);
                } else if (isChildOfParent() || isChildOfDependency()) {
                    lastGroupId_ = getCharacterData();
                } else if (collectExclusions_ && isChildOfExclusion()) {
                    lastExclusionGroupId_ = getCharacterData();
                }
            }
            case "artifactId" -> {
                if (isChildOfProject()) {
                    addProjectProperty(qName);
                } else if (isChildOfParent() || isChildOfDependency()) {
                    lastArtifactId_ = getCharacterData();
                } else if (collectExclusions_ && isChildOfExclusion()) {
                    lastExclusionArtifactId_ = getCharacterData();
                }
            }
            case "version" -> {
                if (isChildOfProject()) {
                    addProjectProperty(qName);
                } else if (isChildOfParent() || isChildOfDependency()) {
                    lastVersion_ = getCharacterData();
                }
            }
            case "type" -> {
                if (isChildOfDependency()) {
                    lastType_ = getCharacterData();
                }
            }
            case "classifier" -> {
                if (isChildOfDependency()) {
                    lastClassifier_ = getCharacterData();
                }
            }
            case "scope" -> {
                if (isChildOfDependency()) {
                    lastScope_ = getCharacterData();
                }
            }
            case "optional" -> {
                if (isChildOfDependency()) {
                    lastOptional_ = getCharacterData();
                }
            }
            case "packaging", "name", "description", "url", "inceptionYear" -> {
                if (isChildOfProject()) {
                    addProjectProperty(qName);
                }
            }
            default -> {
                if (collectProperties_) {
                    properties_.put(qName, getCharacterData());
                }
            }
        }

        characterData_ = null;
    }

    private boolean isChildOfProject() {
        return elementStack_.peek().equals("project");
    }

    private boolean isChildOfParent() {
        return elementStack_.peek().equals("parent");
    }

    private boolean isChildOfDependency() {
        return elementStack_.peek().equals("dependency");
    }

    private boolean isChildOfExclusion() {
        return elementStack_.peek().equals("exclusion");
    }

    private void addProjectProperty(String name) {
        properties_.put("project." + name, getCharacterData());
    }

    private String getCharacterData() {
        if (characterData_ == null) {
            return null;
        }

        var result = characterData_.toString().trim();
        if (result.isEmpty()) {
            return null;
        }
        return result;
    }

    private static final Pattern MAVEN_PROPERTY = Pattern.compile("\\$\\{([^<>{}]+)}");

    private String resolveProperties(String data) {
        if (data == null) {
            return null;
        }

        boolean replaced;
        do {
            replaced = false;

            var processed_data = new StringBuilder();
            var matcher = MAVEN_PROPERTY.matcher(data);
            var last_end = 0;
            while (matcher.find()) {
                if (matcher.groupCount() == 1) {
                    var property = matcher.group(1);
                    if (properties_.containsKey(property)) {
                        processed_data.append(data, last_end, matcher.start());
                        processed_data.append(properties_.get(property));
                        last_end = matcher.end();

                        replaced = true;
                    }
                }
            }
            if (last_end < data.length()) {
                processed_data.append(data.substring(last_end));
            }

            data = processed_data.toString();
        } while (replaced);

        return data;
    }

    private void resetState() {
        lastGroupId_ = null;
        lastArtifactId_ = null;
        lastVersion_ = null;
        lastType_ = null;
        lastClassifier_ = null;
        lastScope_ = null;
        lastOptional_ = null;
        lastExclusionArtifactId_ = null;
        lastExclusionGroupId_ = null;
        exclusions_ = null;
    }

    public void characters(char[] ch, int start, int length) {
        if (characterData_ != null) {
            characterData_.append(String.copyValueOf(ch, start, length));
        }
    }
}
