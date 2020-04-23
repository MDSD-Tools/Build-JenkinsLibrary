def gradleVersion = CFG.gradleVersion ?: '5.5.1'
def gradleJdkVersion = CFG.gradleJdkVersion ?: '11'

// If the gradle version is not specified explicitly we try to extract it from the gradle wrapper properties file.
// We do not use the gradle wrapper directly, to avoid executing binary code from the repository and benefit from
// caching of the build container images.
if (!CFG.gradleVersion) {
    def propFile = new File("${CFG.workspacePath}/gradle/wrapper/gradle-wrapper.properties")
    if (propFile.exists() && propFile.canRead()) {
        def wrapperProperties = new Properties()
        propFile.withInputStream {
            stream -> wrapperProperties.load(stream)
        }
        if (wrapperProperties.containsKey("distributionUrl")) {
            def url = wrapperProperties["distributionUrl"]
            // Use regex to extract Gradle version from distribution url
            // The url is usually formatted as follows: https\://services.gradle.org/distributions/gradle-5.6.4-all.zip
            def match = url =~ /\d+\.\d+\.\d+/
            if (match.find()) {
                gradleVersion = match[0]
            }
        }
    }
}

extendConfiguration([
    dockerBuildImage: "gradle:${gradleVersion}-jdk${gradleJdkVersion}",
    dockerWithRunParameters: """\
    ${CFG.dockerWithRunParameters ?: ""} \
    -v ${CFG.slaveHome}/.gradle:/.gradle:ro \
    """])

MPLModule('Setup Container')