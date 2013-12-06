package com.btmatthews.maven.plugins.spoon.processors;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import spoon.processing.AbstractProcessor;
import spoon.processing.Severity;
import spoon.reflect.declaration.CtElement;

import java.io.File;
import java.io.IOException;

public class GroovyProcessor<T extends CtElement> extends AbstractProcessor<T> {

    private final Script script;

    public GroovyProcessor(final File script) throws IOException {
        this.script = new GroovyShell().parse(script);
    }

    @Override
    public void process(final T element) {
        final Binding binding = new Binding();
        binding.setVariable("element", element);
        binding.setVariable("factory", getFactory());
        script.setBinding(binding);
        script.run();
    }
}
