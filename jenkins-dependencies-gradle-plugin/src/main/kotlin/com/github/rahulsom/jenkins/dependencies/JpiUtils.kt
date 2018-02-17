package com.github.rahulsom.jenkins.dependencies

import org.gradle.api.Project
import java.io.File

class JpiUtils(private val project: Project) {

    fun extractClasses() {
        val jenkinsPlugins = project.configurations.findByName(Constants.JpiConfigurationName)

        val jpiDir = getHpiDir()
        val jpiContentDir = getContentsDir()

        project.copy {
            it.from(jenkinsPlugins)
            it.into(jpiDir)
            it.include("**/*.hpi")
        }

        project.fileTree(jpiDir).forEach { jpiFile ->
            project.copy {
                it.from(project.zipTree(jpiFile))
                it.into(jpiContentDir)
            }
        }

    }

    fun getContentsDir(): File =
        project.file("${getDependenciesDir()}/jpi-content")

    private fun getHpiDir(): File =
        project.file("${getDependenciesDir()}/jpis")

    private fun getDependenciesDir(): File =
        project.file("${project.buildDir}/dependencies")

}