def call(body) {

	// mandatory framework stuff
	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	try {
	
		pipeline {
		
			// define parameters for parameterized builds
			properties([
				parameters([
					booleanParam(defaultValue: false, name: 'Release', description: 'Set true for release build'),
					string(defaultValue: 'nightly', name: 'ReleaseVersion', description: 'Set version to be used for the release')
				])
			])

			node('docker') {
				def workspace
				
				def slaveHome = "${env.SLAVE_HOME}"
				def slaveUid = "${env.SLAVE_USER_ID}"
				
				// build constants
				final BUILD_IMAGE = 'maven:3-jdk-11'

				// evaluation of git build information
				boolean isPullRequest = "${env.CHANGE_TARGET}".toBoolean()
				boolean isMasterBranch = "${env.GIT_BRANCH}" == 'master'

				// evaluation of build parameters
				String relativeArtifactsDir = "${config.updateSiteLocation}"
				final MANDATORY_PARAMS = ['gitUrl', 'webserverDir', 'updateSiteLocation']
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

				
				stage ('Prepare') {
					deleteDir()
					workspace = pwd()
				}
				
				stage ('Checkout') {
					checkout scm
				}
				
				stage ('Build') {
					timeout(time: 30, unit: 'MINUTES') {

						// treat maven cache read only by default
						def cacheVolumeMount = "-v ${slaveHome}/.m2:/.m2:ro"
						def cacheCopyCommand = 'cp -r /.m2 /tmp'

						// treat maven cache writable for master branch
						if (isMasterBranch && !isPullRequest) {
							cacheVolumeMount = "-v ${slaveHome}/.m2:/tmp/.m2"
							cacheCopyCommand = 'echo "Writable m2 cache enabled"'
						}

						// inject maven config file
						configFileProvider(
							[configFile(fileId: 'fba2768e-c997-4043-b10b-b5ca461aff54', variable: 'MAVEN_SETTINGS')]) {
							
							// run maven build in docker container
							sh """docker run --rm \
								-u ${slaveUid} \
								-w /tmp/ws \
								-v ${workspace}:/tmp/ws \
								${cacheVolumeMount} \
								-v /tmp/emptyDir:/root/.m2:ro \
								-v $MAVEN_SETTINGS:/settings.xml:ro \
								-e MAVEN_CONFIG=/tmp/.m2 \
								-e MAVEN_OPTS=-Duser.home=/tmp \
								-m 4G \
								--storage-opt size=20G \
								--network proxy \
								BUILD_IMAGE /bin/sh -c '${cacheCopyCommand} && mvn -s /settings.xml clean verify'"""
						}

					}
				}
				
				if (isMasterBranch && !isPullRequest) {

					stage ('Archive') {
						archiveArtifacts "${relativeArtifactsDir}/**/*"
					}

					stage ('Quality Metrics') {

						publishHTML([
							allowMissing: false,
							alwaysLinkToLastBuild: false,
							keepAll: false,
							reportDir: "${relativeArtifactsDir}/javadoc",
							reportFiles: 'overview-summary.html',
							reportName: 'JavaDoc',
							reportTitles: ''
						])

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
					}

				}

			}
		}
	}
	catch (err) {
		currentBuild.result = "FAILURE"
		echo err
		throw err
	}

}