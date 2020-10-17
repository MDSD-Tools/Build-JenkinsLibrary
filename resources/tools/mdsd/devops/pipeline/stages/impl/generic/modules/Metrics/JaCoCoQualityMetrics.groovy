// This module makes assumptions about folder names which are typical for Maven
// TODO: Generalize this by delegating to build tool specific implementation
// if necessary.
jacoco([
    execPattern: '**/target/*.exec',
    classPattern: '**/target/classes',
    sourcePattern: '**/src,**/src-gen,**/xtend-gen',
    inclusionPattern: '**/*.class',
    exclusionPattern: '**/*Test*.class'
])

if (CFG.isPullRequest) {
    final COVERAGE_LEVEL = 'instructionCoverage'
    final EPSILON = 0.1
    final STATUS_CONTEXT = 'continuous-integration/jenkins/codecoverage'
    final STATUS_URL = "${env.BUILD_URL}jacoco"


    echo "Collecting coverage of master branch"
    def masterBranchName = 'main'
    def masterCoverageUrl = "${env.JOB_URL}../${masterBranchName}/lastSuccessfulBuild/jacoco/api/json"
    def response = httpRequest url: masterCoverageUrl, validResponseCodes: '100:599'
    if (!200.equals(response.status)) {
        masterBranchName = 'master'
        masterCoverageUrl = "${env.JOB_URL}../${masterBranchName}/lastSuccessfulBuild/jacoco/api/json"
        response = httpRequest url: masterCoverageUrl, validResponseCodes: '100:599'
    }
    if (200.equals(response.status)) {
        def jsonObject = readJSON text: response.content
        def masterCoverage = jsonObject[COVERAGE_LEVEL]['percentageFloat']
        echo "Found master branch coverage ${COVERAGE_LEVEL} of ${masterCoverage}."

        echo "Determining job coverage"
        def coverageUrl = "${env.BUILD_URL}jacoco/api/json"
        response = httpRequest url: coverageUrl, validResponseCodes: '100:599'
        if (200.equals(response.status)) {
            jsonObject = readJSON text: response.content
            def jobCoverage = jsonObject[COVERAGE_LEVEL]['percentageFloat']
            echo "Found job coverage ${COVERAGE_LEVEL} of ${jobCoverage}."
            if (jobCoverage - masterCoverage < -1.0 * EPSILON) {
                echo "Coverage decreased from ${masterCoverage} to ${jobCoverage}"
                // TODO report worsened coverage
            } else {
                echo "Coverage did not decrease."
                githubNotify context: STATUS_CONTEXT,
                             targetUrl: STATUS_URL,
                             status: 'SUCCESS',
                             description: "The code coverage did not decrease with respect to the ${masterBranchName} branch."
            }
        } else {
            echo "Could not find job coverage."
            // TODO report worsened coverage
        }
    } else {
        echo "Could not find master branch coverage. Skipping coverage comparison."
    }
}