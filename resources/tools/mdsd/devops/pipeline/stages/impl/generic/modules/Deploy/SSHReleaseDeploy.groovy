configFileProvider(
    [configFile(fileId: '57dc902b-f5a7-49a9-aec3-98deabe48580', variable: 'COMPOSITE_SCRIPT')]) {
    def SCRIPTNAME = 'compositeCreate.sh'
    sh "rm -f ./$SCRIPTNAME"
    sh "cp $COMPOSITE_SCRIPT ./$SCRIPTNAME"
    try {
        sshPublisher(
            alwaysPublishFromMaster: true,
            failOnError: true,
            publishers: [
                sshPublisherDesc(
                    configName: CFG.deploySSHName,
                    transfers: [
                        sshTransfer(
                            sourceFiles: "$SCRIPTNAME",
                            remoteDirectory: "${CFG.deployProjectDir}"
                        )
                    ]
                ),
                sshPublisherDesc(
                    configName: CFG.deploySSHName,
                    transfers: [
                        sshTransfer(
                            execCommand:
                                "cd ${CFG.deployRootDir}/${CFG.deployProjectDir} && " +
                                "mkdir -p releases/${CFG.deployReleaseVersion} && " +
                                "cp -a ${CFG.deploySubDir}/* releases/${CFG.deployReleaseVersion}/ && " +
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