def gradleVersion = CFG.gradleVersion ?: '5.5.1'
def gradleJdkVersion = CFG.gradleJdkVersion ?: '11'

// If the gradle version is not specified explicitly we try to extract it from the gradle wrapper properties file.
// We do not use the gradle wrapper directly, to avoid executing binary code from the repository and benefit from
// caching of the build container images.
if (!CFG.gradleVersion) {
    if (fileExists('gradle/wrapper/gradle-wrapper.properties')) {
        def wrapperProperties = readProperties file: 'gradle/wrapper/gradle-wrapper.properties'
        if (wrapperProperties.containsKey("distributionUrl")) {
            def url = wrapperProperties["distributionUrl"]
            // Use regex to extract Gradle version from distribution url
            // The url is usually formatted as follows: https\://services.gradle.org/distributions/gradle-5.6.4-all.zip
            def match = url =~ /\d+\.\d+\.\d+/
            if (match.find()) {
                echo "Version extracted from Gradle wrapper: ${match[0]}"
                gradleVersion = match[0]
            } else {
                echo "Could not extract version from Gradle distribution URL $url. Fallback to default version."
            }
        } else {
            echo "gradle-wrapper.properties does not specify a distributionUrl. Fallback to default version."
        }
    } else {
        echo "No Gradle version specified and no Gradle wrapper present. Fallback to default version."
    }
}

extendConfiguration([
    dockerBuildImage: "gradle:${gradleVersion}-jdk${gradleJdkVersion}",
    dockerWithRunParameters: """\
    ${CFG.dockerWithRunParameters ?: ""} \
    -v ${CFG.slaveHome}/.gradle:/.gradle:ro \
    """])

MPLModule('Setup Container')