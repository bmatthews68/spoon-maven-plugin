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

package com.btmatthews.maven.plugins.spoon.processors;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtElement;

import java.io.File;
import java.io.IOException;

/**
 * A processor that delegates the processing to a Groovy script.
 *
 * @author <a href="mailto:brian@btmatthews.com">Brian Matthews</a>
 * @since 1.0.0
 */
public class GroovyProcessor<T extends CtElement> extends AbstractProcessor<T> {

    /**
     * The binding used to pass the {@link CtElement}.
     */
    private static final String ELEMENT_BINDING = "element";
    /**
     * The binding used to pass the {@link spoon.reflect.Factory}.
     */
    private static final String FACTORY_BINDING = "factory";
    /**
     * The parsed Groovy script.
     */
    private final Script script;

    /**
     * Initialise the processor by loading and parsing the Groovy script file.
     *
     * @param script The Groovy script file.
     * @throws IOException If there was a problem parsing the Groovy script.
     */
    public GroovyProcessor(final File script) throws IOException {
        this.script = new GroovyShell().parse(script);
    }

    /**
     * Invoke the Groovy script to process the {@link CtElement}.
     *
     * @param element The {@link CtElement} to process.
     */
    @Override
    public void process(final T element) {
        final Binding binding = new Binding();
        binding.setVariable(ELEMENT_BINDING, element);
        binding.setVariable(FACTORY_BINDING, getFactory());
        script.setBinding(binding);
        script.run();
    }
}
