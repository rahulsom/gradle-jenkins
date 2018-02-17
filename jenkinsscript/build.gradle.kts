buildscript {
    repositories {
        mavenCentral()
    }
}

plugins {
    id("maven-publish")
    id("groovy")
    id("com.bmuschko.nexus") version "2.3.1"
}

apply {
    from("../gradle/publishing.gradle")
}

repositories {
    maven { setUrl("http://repo.jenkins-ci.org/releases") }
    maven { setUrl("http://maven.twttr.com/") }
}

dependencies {
    compileOnly("org.codehaus.groovy:groovy-all:2.4.13")
    compileOnly("org.jenkins-ci.main:jenkins-core:2.104") {
        exclude("org.connectbot.jbcrypt", "jbcrypt")
    }
    compileOnly("javax.servlet:servlet-api:2.5")

    testCompile("org.jetbrains.spek:spek-api:1.1.5")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = project.name
            from(components.getByName("java"))
        }
    }
}