def call(def defaultRecipient, def skipNotification = false) {
    def currentResult = currentBuild.result ?: 'SUCCESS'
	def previousResult = currentBuild.previousBuild?.result ?: 'SUCCESS'

	if (!skipNotification) {
		if (currentResult == 'FAILURE') {
			notifyFailure(defaultRecipient)
		} else if (currentResult == 'SUCCESS' && previouslyFailed()) {
			notifyFix(defaultRecipient)
		}
	}
}

def notifyFix(defaultRecipient) {
	notify(defaultRecipient, 'FIXED', 'fixed previous build errors')
}

def notifyFailure(defaultRecipient) {
	notify(defaultRecipient, 'FAILED', 'failed')
}

def notify(defaultRecipient, token, verb) {
	node {
		wrap([$class: 'MaskPasswordsBuildWrapper', varMaskRegexes: [
			[regex: '@[^\\s,]+']
		]]) {
			emailext([
				attachLog: true,
				body: "The build of ${JOB_NAME} #${BUILD_NUMBER} ${verb}.\nPlease visit ${BUILD_URL} for details.",
				subject: "${token}: build of ${JOB_NAME} #${BUILD_NUMBER}",
				to: defaultRecipient,
				recipientProviders: [[$class: 'RequesterRecipientProvider'], [$class:'CulpritsRecipientProvider']]
			])
		}
	}

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