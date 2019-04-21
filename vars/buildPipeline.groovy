def call(body) {

	def config = [:]
	body.resolveStrategy = Closure.DELEGATE_FIRST
	body.delegate = config
	body()

	try {
	
		def relativeArtifactsDir = "${config.updateSiteLocation}"
		
		pipeline {
		
			parameters {
				booleanParam (name: 'Release', defaultValue: false, description: 'Set true for Release')
				string (defaultValue: '', description: 'set Version of Release', name: 'ReleaseVersion', trim: true)
			}
			
			options {
                timeout(time: 30, unit: 'MINUTES')
			}

			node('docker') {
				def workspace
				
				def slaveHome = "${env.SLAVE_HOME}"
				def slaveUid = "${env.SLAVE_USER_ID}"
				
				stage ('Prepare') {
					deleteDir()
					workspace = pwd()
				}
				
				
				stage ('Checkout') {
					checkout scm
					/*
					sh """docker run --rm \
					-u ${slaveUid} \
					-v ${workspace}:/git alpine/git clone \
					--depth 1 \
					https://github.com/PalladioSimulator/Palladio-ThirdParty-YakinduStateCharts.git \
					./"""
					*/
				}
				
				stage ('Build') {
					def cacheVolumeMount = "-v ${slaveHome}/.m2:/.m2:ro"
					def cacheCopyCommand = 'cp -r /.m2 /tmp'
					//def cacheVolumeMount = "-v ${slaveHome}/.m2:/tmp/.m2"
					//def cacheCopyCommand = 'echo "Writable m2 cache enabled"'
					try {
						configFileProvider(
							[configFile(fileId: 'fba2768e-c997-4043-b10b-b5ca461aff54', variable: 'MAVEN_SETTINGS')]) {
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
								maven:3-jdk-11 /bin/sh -c '${cacheCopyCommand} && mvn -s /settings.xml clean verify'"""
						}
					} finally {
						
					}
				}
				
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
			} // node
		}
	} // try end
	catch (err) {
		currentBuild.result = "FAILURE"
		throw err
	}

}