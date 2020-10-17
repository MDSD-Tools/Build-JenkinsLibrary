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
    echo "${env.BUILD_URL}"
    echo "${env.JOB_URL}"
}