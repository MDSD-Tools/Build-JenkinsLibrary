extendConfiguration([dockerWithRunParameters: """\
        ${CFG.dockerWithRunParameters ?: ""} \
        -u ${CFG.slaveUid} \
        -v ${CFG.workspacePath}:/ws:ro \
        -it \
        --entrypoint=/bin/cat \
        """])
        
        