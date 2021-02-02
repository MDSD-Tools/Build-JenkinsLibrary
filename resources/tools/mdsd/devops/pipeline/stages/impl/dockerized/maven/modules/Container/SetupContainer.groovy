def mavenVersion = CFG.mavenVersion ?: '3'
def mavenJdkVersion = CFG.mavenJdkVersion ?: '11'

// try to use a forked version of the maven image
def mavenImageName = "maven:${mavenVersion}-jdk-${mavenJdkVersion}"
def mavenImageFqn = "docker.mdsd.tools/${mavenImageName}"
try{
    def dockerImage = docker.image(mavenImageFqn)
    dockerImage.pull()
} catch (err){
    mavenImageFqn = mavenImageName
}

extendConfiguration([
    dockerBuildImage: mavenImageFqn,
    dockerWithRunParameters: """\
    ${CFG.dockerWithRunParameters ?: ""} \
    -v ${CFG.slaveHome}/.m2:/.m2:ro \
    -v ${CFG.emptySlaveDir}:/root/.m2:ro \
    -v ${CFG.mavenSettingsFile}:/settings.xml:ro \
    -e MAVEN_CONFIG=/tmp/.m2 \
    -e MAVEN_OPTS=-Duser.home=/tmp \
    -e USE_PROXY=true \
    """])

MPLModule('Setup Container')
