def mavenVersion = CFG.mavenVersion ?: '3'
def mavenJdkVersion = CFG.mavenJdkVersion ?: '11'

extendConfiguration([
    dockerBuildImage: "maven:${mavenVersion}-jdk-${mavenJdkVersion}",
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
