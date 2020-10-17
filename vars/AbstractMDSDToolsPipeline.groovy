/**
 * Abstract MDSDTools Pipeline
 * Defines pipeline stages of MDSD.tools Build process
 * Relies on concrete Pipeline to provide agent configuration
 */
def call(defaults  = [:], overrides = [:]) {
  extendConfiguration(defaults, [    
    deployRelease: params.Release,
    deployReleaseVersion: params.ReleaseVersion
  ] + overrides) 

  def config = getCurrentConfiguration()
  
  def modules = ['Prepare', 'Checkout', 'Build', 'Archive', 'Quality Metrics', 'Deploy', 'Cleanup']
  def moduleConfig = [:]

  modules.each {
    if (!config["skip${it.replaceAll("\\s","")}"]) {
      moduleConfig.put(it, [:])
    } else {
      echo "Stage $it will be skipped."
    }
  }
  
  extendConfiguration([
    modules: moduleConfig,
    notifyDefault: decodeEmailAddress(config.notifyDefault)
  ])

  config = getCurrentConfiguration()

  pipeline {
    agent {
      label config.agent_label
    }

    parameters {
      booleanParam(defaultValue: false, name: 'Release', description: 'Set true for release build')
      string(defaultValue: 'nightly', name: 'ReleaseVersion', description: 'Set version to be used for the release')
    }

    options {
      skipDefaultCheckout true
      buildDiscarder(logRotator(artifactDaysToKeepStr: '1', artifactNumToKeepStr: '', daysToKeepStr: '', numToKeepStr: '100'))
    }

    stages { 
      stage( 'Prepare' ) {
        steps {
          extendConfiguration([
            workspacePath: pwd(),
            isMasterBranch: ['master', 'main'].find{it == "$BRANCH_NAME"},
            isPullRequest: !(env.CHANGE_TARGET == null)
          ])
          MPLModule()
        }
      }
      stage( 'Checkout' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
      stage( 'Build' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
      stage( 'Archive' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
      stage( 'Quality Metrics' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
      stage( 'Deploy' ) {
        when { expression { MPLModuleEnabled() } }
        steps {
          MPLModule()
        }
      }
    }
    post {
      always {
        emailNotification(config.notifyDefault, config.skipNotification)
      }
      cleanup {
        script {
          if (MPLModuleEnabled('Cleanup')) {
            MPLModule ('Cleanup')
          }
        }
      }
    }
  }
}  

def decodeEmailAddress(configParameter) {
	return configParameter.contains('@') ? configParameter : new String(configParameter.decodeBase64())
}
