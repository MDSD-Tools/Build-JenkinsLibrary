MPLModule('Setup Container')

if (CFG.sonatypeDeploymentActive) {
	CFG = getCurrentConfiguration()
	extendConfiguration([dockerWithRunParameters: """\
		-e GNUPGHOME=/tmp/.gnupg \
		-v ${CFG.gpgKeyFile}:/gpg.sec:ro \
		${CFG.dockerWithRunParameters} \
		"""])
}