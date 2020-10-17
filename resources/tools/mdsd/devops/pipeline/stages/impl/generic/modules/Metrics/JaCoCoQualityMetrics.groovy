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
    echo "Test Coverage Debug"
    def lastCoverageUrl = "${env.JOB_URL}../main/lastSuccessfulBuild/jacoco/api/json"
    def response = httpRequest lastCoverageUrl
    if (!200.equals(response.status)) {
        lastCoverageUrl = "${env.JOB_URL}../master/lastSuccessfulBuild/jacoco/api/json"
        response = httpRequest lastCoverageUrl
    }
    if (200.equals(response.status)) {
        def jsonObject = readJSON text: response.content
        def coverage = jsonObject['instructionCoverage']['percentageFloat']
        echo "${coverage}"
    }
    echo "${response.status}"
}