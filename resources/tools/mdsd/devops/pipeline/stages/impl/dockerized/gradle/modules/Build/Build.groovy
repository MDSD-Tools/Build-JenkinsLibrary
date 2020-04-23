if (CFG.gradlePropertiesId) {
    configFileProvider([configFile(fileId: CFG.gradlePropertiesId, variable: 'GRADLE_PROPERTIES')]) {
        extendConfiguration([gradlePropertiesFile: GRADLE_PROPERTIES])
        MPLModule("Build")
    }
} else {
    MPLModule("Build")
}