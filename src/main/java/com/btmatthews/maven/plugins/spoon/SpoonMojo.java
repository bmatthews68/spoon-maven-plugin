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

import com.btmatthews.maven.plugins.spoon.processors.GroovyProcessor;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import spoon.processing.Builder;
import spoon.processing.ProcessingManager;
import spoon.reflect.Factory;
import spoon.reflect.declaration.CtElement;
import spoon.support.DefaultCoreFactory;
import spoon.support.JavaOutputProcessor;
import spoon.support.QueueProcessingManager;
import spoon.support.StandardEnvironment;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:brian@btmatthews.com">Brian Matthews</a>
 * @since 1.0.0
 */
@Mojo(
        name = "spoon",
        defaultPhase = LifecyclePhase.PROCESS_SOURCES,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME,
        configurator = "include-project-dependencies")
public class SpoonMojo extends AbstractMojo {

    @Parameter(property = "maven.compiler.source", defaultValue = "1.5")
    private String source;
    @Parameter(required = true)
    private File[] inputSources;
    @Parameter(required = true)
    private String[] processors;
    @Parameter(defaultValue = "${project.build.directory}/spooned", required = true)
    private File outputDirectory;
    @Parameter(property = "project", required=true, readonly = true)
    private MavenProject project;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final int complianceLevel = getComplianceLevel();
        if (complianceLevel >= 5 && complianceLevel <= 8) {
            try {
                outputDirectory.mkdirs();
                doExecute(complianceLevel);
                project.addCompileSourceRoot(outputDirectory.getAbsolutePath());
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

    private int getComplianceLevel() {
            final Matcher matcher = Pattern.compile("^1\\.([5-8])$").matcher(source);
            if (matcher.matches()) {
                return Integer.valueOf(matcher.group(1));
            } else {
                return 0;
            }
    }

    private void doExecute(final int complianceLevel) throws Exception {
        final StandardEnvironment env = new StandardEnvironment();
        env.setVerbose(false);
        env.setDebug(false);
        env.setComplianceLevel(complianceLevel);

        getLog().info("Write processed sources to: " + outputDirectory.getAbsolutePath());

        final Factory factory = new Factory(new DefaultCoreFactory(), env);

        final Builder builder = factory.getBuilder();
        for (final File inputSource : inputSources) {
            getLog().info("Adding input source: " + inputSource.getPath());
            builder.addInputSource(inputSource);
        }
        builder.build();

        final ProcessingManager processing = new QueueProcessingManager(factory);
        for (final String processor : processors) {
            getLog().info("Adding processor: " + processor);
            if (processor.endsWith(".groovy")) {
                final File script = new File(processor);
                if (script.exists()) {
                    processing.addProcessor(new GroovyProcessor<CtElement>(script));
                }
            } else {
                processing.addProcessor(processor);
            }
        }
        processing.addProcessor(new JavaOutputProcessor(outputDirectory));

        getLog().info("Started processing input sources");
        processing.process();
        getLog().info("Finished processing input sources");
    }
}
