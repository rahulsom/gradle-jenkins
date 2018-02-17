package com.github.rahulsom.jenkinsscript

import groovy.transform.CompileStatic
import groovy.transform.Memoized
import groovy.transform.TypeChecked
import hudson.model.Describable
import jenkins.model.Jenkins

import java.util.logging.Logger

/**
 *
 */
@CompileStatic
@TypeChecked
abstract class JenkinsScript extends Script {

    @SuppressWarnings("GrMethodMayBeStatic")
    Jenkins getJenkins() {
        Jenkins.get()
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    <T extends Describable> T descriptor(Class<T> type) {
        jenkins.getDescriptor(type) as T
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    <T> T extension(Class<T> type) {
        jenkins.getExtensionList(type).
                with { it.empty ? null : it.first() }
    }

    @SuppressWarnings("GroovyUnusedDeclaration")
    @Memoized
    Logger getLogger() {
        Logger.getLogger(this.class.name)
    }
}
