/*
 * Copyright 2013 Brian Matthews
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.btmatthews.maven.plugins.spoon;

import org.codehaus.plexus.classworlds.realm.ClassRealm;
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

/**
 * @author <a href="mailto:brian@btmatthews.com">Brian Matthews</a>
 * @since 1.0.0
 */
@Component(role = ComponentConfigurator.class, hint = "include-project-dependencies")
public class IncludeProjectDependenciesComponentConfigurator extends AbstractComponentConfigurator {

    /**
     * @param component           The Mojo being configured.
     * @param configuration
     * @param expressionEvaluator Used to obtain compile scope class path elements.
     * @param containerRealm      The class loader.
     * @param listener
     * @throws ComponentConfigurationException
     *          If there was a problem obtaining the class path or loading individual classpath element.
     */
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

        converter.processConfiguration(converterLookup, component, containerRealm, configuration,
                expressionEvaluator, listener);
    }

    /**
     * Add the compile scope class path elements to the class loader.
     *
     * @param expressionEvaluator Used to obtain compile scope class path elements.
     * @param containerRealm      The class loader.
     * @throws ComponentConfigurationException
     *          If there was a problem obtaining the class path or loading individual classpath element.
     */
    @SuppressWarnings("unchecked")
    private void addProjectDependenciesToClassRealm(final ExpressionEvaluator expressionEvaluator,
                                                    final ClassRealm containerRealm)
            throws ComponentConfigurationException {
        try {
            final List<String> classpathElements = (List<String>) expressionEvaluator
                    .evaluate("${project.compileClasspathElements}");

            if (classpathElements != null) {
                for (final URL url : buildURLs(classpathElements)) {
                    containerRealm.addURL(url);
                }
            }
        } catch (final ExpressionEvaluationException e) {
            throw new ComponentConfigurationException(
                    "There was a problem evaluating: ${project.compileClasspathElements}", e);
        }
    }

    /**
     * Convert the list of class path elements to URLs.
     *
     * @param classpathElements A list of class path elements.
     * @return A list of URLs.
     * @throws ComponentConfigurationException
     *          If a class path element is not invalid.
     */
    private List<URL> buildURLs(final List<String> classpathElements)
            throws ComponentConfigurationException {
        final List<URL> urls = new ArrayList<URL>(classpathElements.size());
        for (final String classpathElement : classpathElements) {
            try {
                final URL url = new File(classpathElement).toURI().toURL();
                urls.add(url);
            } catch (final MalformedURLException e) {
                throw new ComponentConfigurationException("Unable to access project dependency: " + classpathElement, e);
            }
        }
        return urls;
    }
}
