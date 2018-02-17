buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("nebula.release").version("6.2.0")
}

subprojects {
    group = "com.github.rahulsom"

    repositories {
        mavenCentral()
    }
}

tasks["release"].dependsOn(
    ":jenkinsscript:uploadArchives",
    ":jenkins-dependencies-gradle-plugin:publishPlugins"
)