if (CFG.mavenSettingsId) {
    configFileProvider([configFile(fileId: CFG.gradlePropertiesId, variable: 'GRADLE_PROPERTIES')]) {
        extendConfiguration([gradlePropertiesFile: GRADLE_PROPERTIES])
        MPLModule("Build")
    }
} else {
    MPLModule("Build")
}