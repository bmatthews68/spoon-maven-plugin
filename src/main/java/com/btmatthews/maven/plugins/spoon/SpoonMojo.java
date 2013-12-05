/*
 * Copyright 2013 Brian Thomas Matthews
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.btmatthews.maven.plugins.spoon;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import spoon.processing.Builder;
import spoon.processing.ProcessingManager;
import spoon.reflect.Factory;
import spoon.support.DefaultCoreFactory;
import spoon.support.JavaOutputProcessor;
import spoon.support.QueueProcessingManager;
import spoon.support.StandardEnvironment;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:brian@btmatthews.com">Brian Matthews</a>
 * @since 1.0.0
 */
@Mojo(name = "spoon", defaultPhase = LifecyclePhase.PROCESS_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class SpoonMojo extends AbstractMojo {

    @Parameter(property = "project.compileClasspathElements", required = true, readonly = true)
    private List<String> classpathEntries;
    @Parameter(defaultValue = "${maven.compiler.source}", required = true)
    private String source;
    @Parameter(required = true)
    private File[] inputSources;
    @Parameter(required = true)
    private String[] processors;
    @Parameter(defaultValue = "${project.build.directory}/spooned", required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Matcher matcher = Pattern.compile("^1.([5-8])$").matcher(source);
        if (matcher.matches()) {
            try {
                final Set<URL> urls = new HashSet<URL>();
                for (final String classpathEntry : classpathEntries) {
                    System.out.println(classpathEntry);
                    urls.add(new File(classpathEntry).toURI().toURL());
                }
                final ClassLoader contextClassLoader = URLClassLoader.newInstance(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());
                System.out.print(contextClassLoader);
                final Thread worker = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println(Thread.currentThread().getContextClassLoader());
                        doWork(Integer.valueOf(matcher.group(1)));
                    }
                });
                worker.setContextClassLoader(contextClassLoader);
                worker.start();
                worker.join();
            } catch (final Exception e) {
                getLog().error(e.getMessage(), e);
                throw new MojoExecutionException(e.getMessage(), e);
            }
        } else {
            final String message = "Invalid or unsupported source level. Must be 1.5, 1.6, 1.7 or 1.8";
            getLog().error(message);
            throw new MojoExecutionException(message);
        }
    }

    private void doWork(final int complianceLevel) {
        try {
            final StandardEnvironment env = new StandardEnvironment();
            env.setVerbose(false);
            env.setDebug(false);
            env.setComplianceLevel(complianceLevel);

            final JavaOutputProcessor printer = new JavaOutputProcessor(outputDirectory);
            env.setDefaultFileGenerator(printer);

            final Factory factory = new Factory(new DefaultCoreFactory(), env);

            final Builder builder = factory.getBuilder();
            for (final File inputSource : inputSources) {
                builder.addInputSource(inputSource);
            }
            builder.build();

            final ProcessingManager processing = new QueueProcessingManager(factory);
            for (String processor : processors) {
                processing.addProcessor(processor);
            }
            processing.process();

            final ProcessingManager printing = new QueueProcessingManager(factory);
            printing.addProcessor(env.getDefaultFileGenerator());
            printing.process();
        } catch (final Exception e) {
            getLog().error(e.getMessage(), e);
        }
    }
}
