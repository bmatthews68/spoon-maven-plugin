package com.btmatthews.maven.plugins.spoon;

import org.codehaus.classworlds.ClassRealm;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.configurator.AbstractComponentConfigurator;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ComponentConfigurator;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.special.ClassRealmConverter;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component(role = ComponentConfigurator.class, hint = "include-project-dependencies")
public class IncludeProjectDependenciesComponentConfigurator extends AbstractComponentConfigurator {
    @Override
    public void configureComponent(final Object component,
                                   final PlexusConfiguration configuration,
                                   final ExpressionEvaluator expressionEvaluator,
                                   final ClassRealm containerRealm,
                                   final ConfigurationListener listener)
            throws ComponentConfigurationException {

        addProjectDependenciesToClassRealm(expressionEvaluator, containerRealm);

        converterLookup.registerConverter(new ClassRealmConverter(containerRealm));

        final ObjectWithFieldsConverter converter = new ObjectWithFieldsConverter();

        converter.processConfiguration(converterLookup, component, containerRealm.getClassLoader(),
                configuration, expressionEvaluator, listener);
    }

    private void addProjectDependenciesToClassRealm(final ExpressionEvaluator expressionEvaluator,
                                                    final ClassRealm containerRealm)
            throws ComponentConfigurationException {
        try {
            final List<String> classpathElements = (List<String>) expressionEvaluator
                    .evaluate("${project.compileClasspathElements}");

            if (classpathElements != null) {
                final URL[] testUrls = buildURLs(classpathElements);
                for (final URL url : testUrls) {
                    containerRealm.addConstituent(url);

                }
            }
        } catch (final ExpressionEvaluationException e) {
            throw new ComponentConfigurationException(
                    "There was a problem evaluating: ${project.compileClasspathElements}", e);
        }
    }

    private URL[] buildURLs(final List<String> runtimeClasspathElements)
            throws ComponentConfigurationException {
        final List<URL> urls = new ArrayList<URL>(runtimeClasspathElements.size());
        for (final String element : runtimeClasspathElements) {
            try {
                final URL url = new File(element).toURI().toURL();
                urls.add(url);
            } catch (final MalformedURLException e) {
                throw new ComponentConfigurationException("Unable to access project dependency: " + element, e);
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }
}
