def call(usingDocker = true, buildTool = "Maven", deployment = ['updatesite'], constraintBuild = true) {
    MPLEnforce([])

    MPLModulesPath('tools/mdsd/devops/pipeline/stages')
    MPLModulesPath('tools/mdsd/devops/pipeline/stages/impl/generic')

    if (usingDocker) {
        MPLModulesPath('tools/mdsd/devops/pipeline/stages/impl/dockerized')
        MPLModulesPath("tools/mdsd/devops/pipeline/stages/impl/dockerized/${buildTool.toLowerCase()}")
    } else {
        MPLModulesPath("tools/mdsd/devops/pipeline/stages/impl/${buildTool.toLowerCase()}")
    }

    if (constraintBuild) {
        MPLModulesPath('tools/mdsd/devops/pipeline/stages/impl/constraintBuild')
        if (usingDocker) {
            MPLModulesPath('tools/mdsd/devops/pipeline/stages/impl/dockerized/constraintBuild')
        }
    }

    deployment.each {
        MPLModulesPath("tools/mdsd/devops/pipeline/stages/impl/deploy/${it.toLowerCase()}")
    }

    if (usingDocker) {
        MPLModulesPath('tools/mdsd/devops/pipeline/stages/impl/dockerized/initialize')
    }
}