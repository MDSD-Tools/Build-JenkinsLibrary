extendConfiguration([
    emptySlaveDir: "/tmp/${env.BUILD_TAG}"])

CFG = getCurrentConfiguration()

MPLModulePostStep {
    sh "rm -rf \"${CFG.emptySlaveDir}\""
}

sh "mkdir -p \"${CFG.emptySlaveDir}\""