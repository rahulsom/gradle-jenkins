package com.github.rahulsom.jenkins.dependencies

import com.bmuschko.gradle.docker.DockerRemoteApiPlugin
import com.bmuschko.gradle.docker.tasks.image.DockerBuildImage
import com.bmuschko.gradle.docker.tasks.image.DockerPushImage
import com.bmuschko.gradle.docker.tasks.image.Dockerfile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import java.io.File
import java.io.FileWriter

open class JenkinsDependenciesPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.plugins.apply("groovy")
        project.plugins.apply(DockerRemoteApiPlugin::class.java)

        if (project.configurations.findByName(Constants.JpiConfigurationName) == null) {
            project.configurations.create(Constants.JpiConfigurationName)
        }
        if (project.configurations.findByName(Constants.ScriptConfigurationName) == null) {
            project.configurations.create(Constants.ScriptConfigurationName)
        }
        val jenkinsExtension =
            project.extensions.create(Constants.ExtensionName, JenkinsExtension::class.java)

        val scriptConfiguration =
            project.configurations.getByName(Constants.ScriptConfigurationName)

        project.afterEvaluate {
            val hpiUtils = JpiUtils(project)

            val jenkinsConfiguration =
                project.configurations.getByName(Constants.JpiConfigurationName)
            addPluginsToCompileClasspath(project, hpiUtils, jenkinsConfiguration, jenkinsExtension)

            copyScriptDependencies(project)

            addScriptDependencies(project, scriptConfiguration)

            val dockerDir = ensureDockerDir(project)
            buildPluginTxt(dockerDir, jenkinsConfiguration)

            val createDockerfile = createDockerfileTask(project, jenkinsExtension)
            createDockerfile.dependsOn("compileGroovy")

            val buildImage = createBuildImageTask(project, dockerDir, jenkinsExtension)
            buildImage.dependsOn(createDockerfile)

            val pushImage = project.tasks.create("pushImage", DockerPushImage::class.java)
            pushImage.imageName = "${buildImage.imageId}:${buildImage.tag}"
            pushImage.dependsOn(buildImage)

            project.tasks.getByPath("test").dependsOn(buildImage)
        }
    }

    private fun copyScriptDependencies(project: Project) =
        project.file("build/dependencies/script").apply {
            project.copy {
                it.from(project.configurations.getByName(Constants.ScriptConfigurationName))
                it.into(this)
            }

            val jarFiles = project.configurations.getByName(Constants.ScriptConfigurationName)
                .resolvedConfiguration.firstLevelModuleDependencies
                .flatMap { fileNames(it) }

            listFiles()
                .forEach { file ->
                    jarFiles
                        .find { it.toJar() == file.name }
                        ?.let { dep -> file.renameTo(File(file.parentFile, "${dep.m}.jar")) }
                }
        }

    private fun addScriptDependencies(project: Project, scriptConfiguration: Configuration) =
        project.configurations
            .getByName("compileOnly")
            .dependencies
            .addAll(scriptConfiguration.dependencies)


    private fun addPluginsToCompileClasspath(
        project: Project,
        jpiUtils: JpiUtils,
        jenkinsConfiguration: Configuration,
        extension: JenkinsExtension
    ) {
        val compileOnly = project.configurations.getByName("compileOnly")

        jpiUtils.extractClasses()

        val classes = project.dependencies.create(
            project.files("${jpiUtils.getContentsDir()}/WEB-INF/classes")
        )

        val libs = project.dependencies.create(
            project.fileTree("${jpiUtils.getContentsDir()}/WEB-INF/lib")
        )

        val defaultScripts = File("build/scripts").apply { mkdirs() }
        listOf("A00_CopyJars.groovy").forEach {
            File(defaultScripts, it)
                .writeText(
                    this.javaClass.classLoader.getResourceAsStream(it)
                        .bufferedReader().readText()
                )
        }

        val addDep = compileOnly.dependencies::add

        addDep(project.dependencies.create("org.jenkins-ci.main:jenkins-core:${extension.version}"))
        addDep(project.dependencies.create("javax.servlet:servlet-api:2.5"))
        addDep(classes)
        addDep(libs)
        compileOnly.dependencies.addAll(jenkinsConfiguration.dependencies)
    }

    private fun buildPluginTxt(dockerDir: File, jenkinsPlugins: Configuration) =
        FileWriter(File(dockerDir, "plugins.txt"))
            .apply {
                write(
                    jenkinsPlugins
                        .resolvedConfiguration
                        .firstLevelModuleDependencies
                        .map { "${it.moduleName}:${it.moduleVersion}" }
                        .joinToString("\n")
                )
                close()
            }

    private fun ensureDockerDir(project: Project) =
        project.file("build/docker").also {
            if (!it.exists()) {
                it.mkdirs()
            }
        }

    private fun createBuildImageTask(project: Project, dockerDir: File, jenkins: JenkinsExtension) =
        project.tasks
            .create("buildImage", DockerBuildImage::class.java) {
                it.inputDir = project.projectDir
                it.dockerFile = File(dockerDir, "Dockerfile")
                it.tag = "${jenkins.imageName}:${project.version.toString().toLowerCase()}"
            }

    private fun createDockerfileTask(project: Project, extension: JenkinsExtension) =
        project.tasks.create("createDockerfile", Dockerfile::class.java) {
            it.destFile = project.file("build/docker/Dockerfile")

            it.from("jenkins/jenkins:${extension.version}-alpine")

            it.copyFile("build/docker/plugins.txt", "/usr/share/jenkins/ref/plugins.txt")
            it.runCommand("/usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt")

            it.copyFile("src/main/groovy", "/usr/share/jenkins/ref/init.groovy.d")
            File("build/scripts").list().forEach { name ->
                it.copyFile("build/scripts/$name", "/usr/share/jenkins/ref/init.groovy.d/$name")
            }

            it.runCommand("mkdir -p /usr/share/jenkins/ref/jars")

            val scriptDepsDir = project.file("build/dependencies/script")
            it.addFile("build/dependencies/script", "/usr/share/jenkins/ref/jars")
            val logFormat =
                "%1\$tY-%1\$tm-%1\$td %1\$tH:%1\$tM:%1\$tS %4\$-7s %3\$-40.40s - %5\$s%6\$s%n"
                    .replace("\$", "\\\$")

            val classpath = scriptDepsDir.listFiles()
                .joinToString(":") { "/usr/share/jenkins/ref/jars/${it.name}" }

            it.environmentVariable(
                mapOf(
                    "JAVA_OPTS" to "-Djava.util.logging.SimpleFormatter.format='$logFormat'"
                )
            )

            val home = "/var/jenkins_home"
            it.volume("$home/users", "$home/jobs", "$home/logs", "$home/nodes")
        }

    data class Dep(val m: String, val v: String) {
        fun toJar() = "$m-$v.jar"
    }

    private fun fileNames(r: ResolvedDependency): List<Dep> =
        r.children.flatMap { fileNames(it) } + Dep(r.moduleName, r.moduleVersion)

}