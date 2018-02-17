import com.gradle.publish.PluginBundleExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.plugins.ExtensionAware
import org.junit.platform.console.options.Details
import org.junit.platform.console.options.Details.TREE
import org.junit.platform.gradle.plugin.FiltersExtension
import org.junit.platform.gradle.plugin.EnginesExtension
import org.junit.platform.gradle.plugin.JUnitPlatformExtension

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.junit.platform:junit-platform-gradle-plugin:1.0.0")
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm").version("1.2.21")
    id("com.gradle.plugin-publish").version("0.9.9")
}

apply {
    plugin("org.junit.platform.gradle.plugin")
}

val kotlinVersion: String by extra

dependencies {
    compileOnly(kotlin("stdlib-jdk8", kotlinVersion))
    compileOnly(gradleApi())
    compile("com.bmuschko:gradle-docker-plugin:3.2.3")

    testCompile(gradleTestKit())

    testCompile("org.jetbrains.spek:spek-api:1.1.5")
    testRuntime("org.jetbrains.spek:spek-junit-platform-engine:1.1.5")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

pluginBundle {
    website = "https://github.com/rahulsom/gradle-jenkins-dependencies"
    vcsUrl = "https://github.com/rahulsom/gradle-jenkins-dependencies"
    description = "Allows adding jenkins plugins as compileOnly dependencies to gradle projects."
    tags = listOf("jenkins")

    plugins.create("jenkinsDependenciesPlugin") {
        id = "com.github.rahulsom.jenkins-dependencies"
        displayName = "Gradle Jenkins Dependencies Plugin"
    }

}

fun JUnitPlatformExtension.filters(setup: FiltersExtension.() -> Unit) {
    when (this) {
        is ExtensionAware -> extensions.getByType(FiltersExtension::class.java).setup()
        else -> throw Exception("${this::class} must be an instance of ExtensionAware")
    }
}

fun FiltersExtension.engines(setup: EnginesExtension.() -> Unit) {
    when (this) {
        is ExtensionAware -> extensions.getByType(EnginesExtension::class.java).setup()
        else -> throw Exception("${this::class} must be an instance of ExtensionAware")
    }
}

configure<JUnitPlatformExtension> {
    details = TREE

    filters {
        engines {
            include("spek")
        }
    }
}

project.afterEvaluate {
    tasks["processResources"].doLast {
        project.file("build/resources/main/gradle-jenkins.properties").writeText("""
            gradle.jenkins.version=${project.version}
        """.trimIndent())
    }
}
