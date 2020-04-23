lock("gradle-cache-${CFG.slaveName}") {
    sh "docker exec ${CFG.containerId} cp -r /.gradle /tmp"
}

if(CFG.gradlePropertiesFile) {
    sh "docker cp ${CFG.gradlePropertiesFile} ${CFG.containerId}:/tmp/.gradle/gradle.properties"
}

sh "docker exec ${CFG.containerId} cp -r /ws /tmp"
sh "docker exec ${CFG.containerId} gradle -g /tmp/.gradle -p /tmp/ws"

if(CFG.gradlePropertiesFile) {
    sh "docker exec ${CFG.containerId} rm /tmp/.gradle/gradle.properties"
}

sh "docker cp ${CFG.containerId}:/tmp/ws/. ${CFG.workspacePath}"

if (!CFG.isPullRequest) {
    lock("gradle-cache-${CFG.slaveName}") {
        sh "docker cp ${CFG.containerId}:/tmp/.gradle/. ${CFG.slaveHome}/.gradle"
    }
}