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
    def lastCoverageUrl = "${env.JOB_URL}/../master/lastSuccessfulBuild/jacoco/api/json"
    def get = new URL(lastCoverageUrl).openConnection();
    def getRC = get.getResponseCode();
    if(getRC.equals(200)) {
        def jsonText = get.getInputStream().getText()
        def jsonObject = readJSON text: jsonText
        def coverage = jsonObject['instructionCoverage']['percentageFloat']
        echo "${coverage}"
    }
    echo "${getRC}"
}