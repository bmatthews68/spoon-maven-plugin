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
import spoon.processing.Builder;
import spoon.processing.ProcessingManager;
import spoon.reflect.Factory;
import spoon.support.DefaultCoreFactory;
import spoon.support.QueueProcessingManager;
import spoon.support.StandardEnvironment;

import java.io.File;

@Mojo(name = "spoon", defaultPhase = LifecyclePhase.PROCESS_SOURCES)
public class SpoonMojo extends AbstractMojo {

    @Parameter(defaultValue = "1.5", required = true)
    private String source;
    @Parameter(required = true)
    private File[] sources;
    @Parameter(required = true)
    private String[] processors;
    @Parameter(defaultValue = "${project.build.directory}/spooned", required = true)
    private File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            final StandardEnvironment env = new StandardEnvironment();
            env.setComplianceLevel(5);

            final Factory factory = new Factory(new DefaultCoreFactory(), env);

            final Builder builder = factory.getBuilder();
            for (final File source : sources) {
                builder.addInputSource(source);
            }
            builder.build();

            final ProcessingManager processing = new QueueProcessingManager(factory);
            for (String processor : processors) {
                processing.addProcessor(processor);
            }
            processing.process();

            env.getDefaultFileGenerator().setOutputDirectory(outputDirectory);
            final ProcessingManager printing = new QueueProcessingManager(factory);
            printing.addProcessor(env.getDefaultFileGenerator());
            printing.process();

        } catch (final Exception e) {
            getLog().error(e.getMessage(), e);
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }
}
