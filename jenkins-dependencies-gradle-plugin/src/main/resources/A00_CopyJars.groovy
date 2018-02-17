import jenkins.model.Jenkins

import java.util.logging.Level
import java.util.logging.Logger

def logger = Logger.getLogger("A00_CopyJars")

logger.info("Copying jars")

def groovyClassLoader = Jenkins.class.classLoader
def rootClassLoader = groovyClassLoader

def jarPath = new File("/usr/share/jenkins/ref/jars")

jarPath.eachFile { file ->
    logger.info("Adding ${file}")
    try {
        rootClassLoader.addURL(file.toURL())
        logger.info("Successfully added ${file}")
    } catch (Exception e) {
        logger.log(Level.WARNING, "Could not add ${file}.", e)
    }
}
