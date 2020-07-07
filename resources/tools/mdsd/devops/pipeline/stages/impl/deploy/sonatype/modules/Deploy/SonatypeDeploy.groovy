if (!CFG.deploySonatypeSettingsId) {
    error 'A maven settings file id is mandatory for sonatype deployments.'
}
if (!CFG.deploySonatypeGpgId) {
    error 'A GPG key file id is mandatory for sonatype deployments.'
}

def mavenDeployGoal = "clean deploy -Plocal-build-deployable"
if (CFG.deploySonatypeSingleArtifactDeploy) {
    mavenDeployGoal = "gpg:sign-and-deploy-file -Psonatype-single-artifact-deploy"
}

extendConfiguration([
    sonatypeDeploymentActive: true,
    mavenSettingsId: CFG.deploySonatypeSettingsId,
    mavenGoal: "${mavenDeployGoal}",
    skipCacheWriteBack: true,
    dockerWithRunParameters: "",
    dockerBuildImage: "",
    dockerWithRunParameters: "",
    containerId: ""
])

CFG = getCurrentConfiguration()

configFileProvider([configFile(fileId: CFG.deploySonatypeGpgId, variable: 'GPG_KEY')]) {
    extendConfiguration([gpgKeyFile: GPG_KEY])
    MPLModule("Build")
}