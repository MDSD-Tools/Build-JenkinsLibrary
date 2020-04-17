def call(body) {
    AbstractMDSDToolsDSLPipeline {
        agent_label = 'docker'

        buildWithMaven {
            version = '3.6.0'
            jdkVersion = 11
            settingsId = 'fba2768e-c997-4043-b10b-b5ca461aff54'
            goal = 'clean verify'
        }

        constraintBuild {
            timeLimitMinutes = 30
            ramLimit = '4G'
            hddLimit = '20G'
        }
        
        skipDeploy ("${this.BRANCH_NAME}" != 'master')
        skipNotification ("${this.BRANCH_NAME}" != 'master')
        
        deployUpdatesiteSshName 'web'
        deployUpdatesiteRootDir '/home/sftp/data'
        deployUpdatesiteSubDir ("${this.BRANCH_NAME}" == 'master' ? 'nightly': "branches/${this.BRANCH_NAME}")
        deployUpdatesiteProjectDir this.scm.userRemoteConfigs[0].url.replaceFirst(/^.*\/([^\/]+?).git$/, '$1').toLowerCase()

        createCompositeUpdatesiteScriptFileId '57dc902b-f5a7-49a9-aec3-98deabe48580'
             
        notifyDefault 'bWRzZC10b29scy1idWlsZEBpcmEudWthLmRl'

        body.delegate = delegate
        body.resolveStrategy = Closure.DELEGATE_FIRST
        body()
    }
}
