lock("m2-cache-${CFG.slaveName}") {
    sh "docker exec ${CFG.containerId} cp -r /.m2 /tmp"
}
sh "docker exec ${CFG.containerId} cp -r /ws /tmp"
sh "docker exec ${CFG.containerId} mvn -Dbnd.home.dir=/tmp -s /settings.xml -f /tmp/ws/pom.xml clean verify"
sh "docker cp ${CFG.containerId}:/tmp/ws/. ${CFG.workspacePath}"
if (!CFG.isPullRequest) {
    lock("m2-cache-${CFG.slaveName}") {
        sh "docker cp ${CFG.containerId}:/tmp/.m2/. ${CFG.slaveHome}/.m2"
    }
}