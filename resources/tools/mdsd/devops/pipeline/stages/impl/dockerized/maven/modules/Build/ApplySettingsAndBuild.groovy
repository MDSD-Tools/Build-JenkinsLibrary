def mavenSettingsFile = "${CFG.emptySlaveDir}/emptyFile"

if (CFG.mavenSettingsId) {
	configFileProvider(
		[configFile(fileId: CFG.mavenSettingsId, variable: 'MAVEN_SETTINGS')]) {
		extendConfiguration([mavenSettingsFile: MAVEN_SETTINGS])
		MPLModule("Build")
	}
} else {
	sh "touch ${mavenSettingsFile}"
	MPLModule("Build")
}