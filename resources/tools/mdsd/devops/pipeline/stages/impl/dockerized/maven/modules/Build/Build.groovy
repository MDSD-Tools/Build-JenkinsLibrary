extendConfiguration([
    emptySlaveDir: "/tmp/${env.BUILD_TAG}"])

CFG = getCurrentConfiguration()

MPLModulePostStep {
    sh "rm -rf \"${CFG.emptySlaveDir}\""
}

sh "mkdir -p \"${CFG.emptySlaveDir}\""

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