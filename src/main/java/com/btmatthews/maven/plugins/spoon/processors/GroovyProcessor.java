package com.btmatthews.maven.plugins.spoon.processors;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import spoon.processing.AbstractProcessor;
import spoon.processing.Severity;
import spoon.reflect.declaration.CtElement;

import java.io.File;
import java.io.IOException;

public class GroovyProcessor<T extends CtElement> extends AbstractProcessor<T> {

    private final File script;

    public GroovyProcessor(final File script) {
        this.script = script;
    }

    @Override
    public void process(final T element) {
        final Binding binding = new Binding();
        binding.setVariable("element", element);
        try {
            new GroovyShell(binding).evaluate(script);
        } catch (final IOException e) {
            getEnvironment().report(this, Severity.ERROR, element, e.getMessage());
        }
    }
}
