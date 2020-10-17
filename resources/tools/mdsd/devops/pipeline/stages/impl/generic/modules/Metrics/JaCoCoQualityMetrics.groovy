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

// perform code coverage status check for pull requests
if (CFG.isPullRequest) {

    // constants
    final COVERAGE_LEVEL = 'instructionCoverage'
    final EPSILON = 0.001
    final STATUS_CONTEXT = 'continuous-integration/jenkins/codecoverage'
    final STATUS_URL = "${env.BUILD_URL}jacoco"
    final CREDENTIALS_ID = '8adf889c-2157-45d1-acc7-1c6211538dac'

    // pending notification
    githubNotify credentialsId: CREDENTIALS_ID,
                 context: STATUS_CONTEXT,
                 targetUrl: STATUS_URL,
                 status: 'PENDING',
                 description: "Status check in progress..."

    // search for master/main code coverage
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

        // search for job coverage
        echo "Determining job coverage"
        def coverageUrl = "${env.BUILD_URL}jacoco/api/json"
        response = httpRequest url: coverageUrl, validResponseCodes: '100:599'
        if (200.equals(response.status)) {
            jsonObject = readJSON text: response.content
            def jobCoverage = jsonObject[COVERAGE_LEVEL]['percentageFloat']
            echo "Found job coverage ${COVERAGE_LEVEL} of ${jobCoverage}."

            // comparison of job with master coverage
            if (jobCoverage - masterCoverage < -1.0 * EPSILON) {

                // coverage has worsened
                echo "Coverage decreased from ${masterCoverage} to ${jobCoverage}"
                githubNotify credentialsId: CREDENTIALS_ID,
                             context: STATUS_CONTEXT,
                             targetUrl: STATUS_URL,
                             status: 'ERROR',
                             description: "The code coverage decreased from ${masterCoverage}% to ${jobCoverage}% with respect to the ${masterBranchName} branch."
            } else {
                // coverage is better or equal
                echo "Coverage did not decrease."
                githubNotify credentialsId: CREDENTIALS_ID,
                             context: STATUS_CONTEXT,
                             targetUrl: STATUS_URL,
                             status: 'SUCCESS',
                             description: "The code coverage did not decrease with respect to the ${masterBranchName} branch."
            }
        } else {
            // no job coverage found
            echo "Could not find job coverage."
            githubNotify credentialsId: CREDENTIALS_ID,
                         context: STATUS_CONTEXT,
                         targetUrl: STATUS_URL,
                         status: 'FAILURE',
                         description: "Code coverage of job could not be determined but there is a coverage for the ${masterBranchName} branch."
        }
    } else {
        // no master coverage found
        echo "Could not find master branch coverage. Skipping coverage comparison."
        githubNotify credentialsId: CREDENTIALS_ID,
                     context: STATUS_CONTEXT,
                     targetUrl: STATUS_URL,
                     status: 'SUCCESS',
                     description: "There is no code coverage for the ${masterBranchName} branch, skipping coverage check."
    }
}