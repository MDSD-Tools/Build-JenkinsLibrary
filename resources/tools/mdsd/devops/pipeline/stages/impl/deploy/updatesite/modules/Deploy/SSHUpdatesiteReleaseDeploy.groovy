def scriptFileId = "57dc902b-f5a7-49a9-aec3-98deabe48580"
if (CFG.createCompositeUpdatesiteScriptFileId) {
	scriptFileId = CFG.createCompositeUpdatesiteScriptFileId
}
configFileProvider(
    [configFile(fileId: scriptFileId, variable: 'COMPOSITE_SCRIPT')]) {
    def SCRIPTNAME = 'compositeCreate.sh'
    sh "rm -f ./$SCRIPTNAME"
    sh "cp $COMPOSITE_SCRIPT ./$SCRIPTNAME"
    try {
        sshPublisher(
            alwaysPublishFromMaster: true,
            failOnError: true,
            publishers: [
                sshPublisherDesc(
                    configName: CFG.deployUpdatesiteSshName,
                    transfers: [
                        sshTransfer(
                            sourceFiles: "$SCRIPTNAME",
                            remoteDirectory: "${CFG.deployUpdatesiteProjectDir}"
                        )
                    ]
                ),
                sshPublisherDesc(
                    configName: CFG.deployUpdatesiteSshName,
                    transfers: [
                        sshTransfer(
                            execCommand:
                                "cd ${CFG.deployUpdatesiteRootDir}/${CFG.deployUpdatesiteProjectDir} && " +
                                "mkdir -p releases/${CFG.deployReleaseVersion} && " +
                                "cp -a ${CFG.deployUpdatesiteSubDir}/* releases/${CFG.deployReleaseVersion}/ && " +
                                "rm -rf releases/latest && " +
                                "ln -s releases/${CFG.deployReleaseVersion} releases/latest && " +
                                "chmod +x $SCRIPTNAME && " +
                                "./$SCRIPTNAME releases && " +
                                "rm $SCRIPTNAME"
                        )
                    ]
                )
            ]
        )
    } finally {
        sh "rm ./$SCRIPTNAME"
    }
}
