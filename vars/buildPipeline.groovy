final MAIL_DEFAULT_RECIPIENT = new String('c3RlcGhhbi5zZWlmZXJtYW5uQGtpdC5lZHU='.decodeBase64())

def call(body) {

	// mandatory framework stuff
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()
	
	// build constants
	final BUILD_IMAGE = 'maven:3-jdk-11'
	final BUILD_LIMIT_TIME = 30
	final BUILD_LIMIT_RAM = '4G'
	final BUILD_LIMIT_HDD = '20G'

	// evaluation of git build information
	boolean isMasterBranch = "$BRANCH_NAME" == 'master'
	echo "Is master branch: $isMasterBranch"
	boolean isPullRequest = !(env.CHANGE_TARGET == null)
	echo "Is pull request: $isPullRequest"

	// evaluation of build parameters
	String relativeArtifactsDir = "${config.updateSiteLocation}"
	final MANDATORY_PARAMS = ['webserverDir', 'updateSiteLocation']
	for (mandatoryParameter in MANDATORY_PARAMS) {
		if (!config.containsKey(mandatoryParameter) || config.get(mandatoryParameter).toString().trim().isEmpty()) {
			error "Missing mandatory parameter $mandatoryParameter"
		}
	}
	boolean skipCodeQuality = config.containsKey('skipCodeQuality') && config.get('skipCodeQuality').toString().trim().toBoolean()
	boolean skipNotification = config.containsKey('skipNotification') && config.get('skipNotification').toString().trim().toBoolean()
	boolean doReleaseBuild = params.DO_RELEASE_BUILD.toString().toBoolean()
	String releaseVersion = params.RELEASE_VERSION
	if (doReleaseBuild && (releaseVersion == null || releaseVersion.trim().isEmpty())) {
		error 'A release build requires a proper release version.'
	}

	// archive release build
	if (doReleaseBuild) {
		currentBuild.rawBuild.keepLog(true)
	}

	try {
	
		pipeline {
		
			properties([
				// define parameters for parameterized builds
				parameters([
					booleanParam(defaultValue: false, name: 'Release', description: 'Set true for release build'),
					string(defaultValue: 'nightly', name: 'ReleaseVersion', description: 'Set version to be used for the release')
				]),
				// define log rotation
				buildDiscarder(logRotator(artifactDaysToKeepStr: '1', artifactNumToKeepStr: '', daysToKeepStr: '7', numToKeepStr: '')),
			])

			node('docker') {
				def workspace
				
				def slaveHome = "${env.SLAVE_HOME}"
				def slaveUid = "${env.SLAVE_USER_ID}"

				stage ('Prepare') {
					deleteDir()
					workspace = pwd()
				}
				
				stage ('Checkout') {
					// scm is injected and configured by multi branch pipeline
					checkout scm
				}
				
				stage ('Build') {
					timeout(time: BUILD_LIMIT_TIME, unit: 'MINUTES') {

						// inject maven config file
						configFileProvider(
							[configFile(fileId: 'fba2768e-c997-4043-b10b-b5ca461aff54', variable: 'MAVEN_SETTINGS')]) {
							
							// run maven build in docker container
							docker.image(BUILD_IMAGE).withRun("""\
								-u ${slaveUid} \
								-v ${workspace}:/ws:ro \
								-v ${slaveHome}/.m2:/.m2:ro \
								-v /tmp/emptyDir:/root/.m2:ro \
								-v $MAVEN_SETTINGS:/settings.xml:ro \
								-e MAVEN_CONFIG=/tmp/.m2 \
								-e MAVEN_OPTS=-Duser.home=/tmp \
								-m $BUILD_LIMIT_RAM \
								--storage-opt size=$BUILD_LIMIT_HDD \
								--network proxy \
								-it \
								--entrypoint=/bin/cat \
							""") { c ->
								sh "docker exec ${c.id} cp -r /.m2 /tmp"
								sh "docker exec ${c.id} cp -r /ws /tmp"
								sh "docker exec ${c.id} mvn -s /settings.xml -f /tmp/ws/pom.xml clean verify"
								sh "docker cp ${c.id}:/tmp/ws/. ${workspace}"
								if (isMasterBranch && !isPullRequest) {
									sh "docker cp ${c.id}:/tmp/.m2/. ${slaveHome}/.m2"
								}
							}

						}

					}
				}

				stage ('Archive') {
					archiveArtifacts "${relativeArtifactsDir}/**/*"
				}

				stage ('Quality Metrics') {

					if (!skipCodeQuality) {

						if (!isPullRequest) {
							publishHTML([
								allowMissing: false,
								alwaysLinkToLastBuild: false,
								keepAll: false,
								reportDir: "${relativeArtifactsDir}/javadoc",
								reportFiles: 'overview-summary.html',
								reportName: 'JavaDoc',
								reportTitles: ''
							])
						}

						recordIssues([
							tool: checkStyle([
								pattern: '**/target/checkstyle-result.xml'
							])
						])

						junit([
							testResults: '**/surefire-reports/*.xml',
							allowEmptyResults: true
						])

						jacoco([
							execPattern: '**/target/*.exec',
							classPattern: '**/target/classes',
							sourcePattern: '**/src,**/src-gen,**/xtend-gen',
							inclusionPattern: '**/*.class',
							exclusionPattern: '**/*Test*.class'
						])
					} else {
						echo 'Skipping collection of code quality metrics'
					}
				}
				
				stage ('Deploy') {
					if (!isPullRequest && isMasterBranch) {
					
						echo 'TODO: add deployment'
				
					}
				}

				

			}
		}
	}
	catch (err) {
		currentBuild.result = 'FAILURE'
		if (err instanceof hudson.AbortException && err.getMessage().contains('script returned exit code 143')) {
			currentBuild.result = 'ABORTED'
		}
		if (err instanceof org.jenkinsci.plugins.workflow.steps.FlowInterruptedException && err.causes.size() == 0) {
			currentBuild.result = 'ABORTED'
		}
		throw err
	} finally {
		def currentResult = currentBuild.result ?: 'SUCCESS'
		def previousResult = currentBuild.previousBuild?.result ?: 'SUCCESS'

		if (!skipNotification) {
			if (currentResult == 'FAILURE') {
				notifyFailure()
			} else if (currentResult == 'SUCCESS' && previouslyFailed()) {
				notifyFix()
			}
		}
	}

}

def notifyFix() {
	notify('FIXED', 'fixed previous build errors')
}

def notifyFailure() {
	notify('FAILED', 'failed')
}

def notify(token, verb) {
	mail([
		subject: "${token}: build of ${JOB_NAME} #${BUILD_NUMBER}",
		body: "The build of ${JOB_NAME} #${BUILD_NUMBER} ${verb}.\nPlease visit ${BUILD_URL} for details.",
		to: MAIL_DEFAULT_RECIPIENT
	])
}

def previouslyFailed() {
	for (buildObject = currentBuild.previousBuild; buildObject != null; buildObject = buildObject.previousBuild) {
		if (buildObject.result == null) {
			continue
		}
		if (buildObject.result == 'FAILURE') {
			return true
		}
		if (buildObject.resultIsBetterOrEqualTo('UNSTABLE')) {
			return false
		}
	}
}