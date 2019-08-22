if (CFG.sonatypeDeploymentActive) {
	sh "docker exec ${CFG.containerId} mkdir /tmp/.gnupg"
	sh "docker exec ${CFG.containerId} chmod 700 /tmp/.gnupg"
	sh "docker exec ${CFG.containerId} gpg --batch --import /gpg.sec"
}

MPLModule('Build In Container')