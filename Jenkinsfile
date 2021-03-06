pipeline {
    agent any
    options {
        timestamps()
        timeout(time: 1, unit: 'HOURS')
    }
    tools {
        jdk 'openJDK7u131'
    }
    environment {
        MAXINE_HOME="$WORKSPACE/maxine"
        GRAAL_HOME="$WORKSPACE/graal"
        MX="$GRAAL_HOME/mxtool/mx"
        PATH="/localhome/regression/gcc-linaro-7.1.1-2017.08-x86_64_aarch64-linux-gnu/bin:/localhome/regression/gcc-arm-none-eabi-7-2017-q4-major/bin:/localhome/regression/qemu-2.10.1/build/aarch64-softmmu:/localhome/regression/qemu-2.10.1/build/arm-softmmu:/localhome/regression/riscv/bin:$PATH"
    }

    stages {
        stage('clone') {
            steps {
                // Clean up workspace
                step([$class: 'WsCleanup'])
                dir(env.MAXINE_HOME) {
                    checkout scm
                }
                dir(env.GRAAL_HOME) {
                    // Use ugly/advanced syntax to perform shallow clone
                    checkout([$class: 'GitSCM', branches: [[name: 'master']], extensions: [[$class: 'CloneOption', noTags: true, shallow: true]], userRemoteConfigs: [[credentialsId: 'orion_github', url: 'https://github.com/beehive-lab/Maxine-Graal.git']]])
                }
            }
        }
        stage('checkstyle-n-build') {
            steps {
                parallel 'checkstyle': {
                    dir(env.MAXINE_HOME) {
                        sh '$MX --suite maxine checkstyle'
                    }
                }, 'build': {
                    dir(env.MAXINE_HOME) {
                        sh '$MX build'
                    }
                }
            }
        }
        stage('image-n-test-init') {
            steps {
                parallel 'image': {
                    dir(env.MAXINE_HOME) {
                        sh '$MX image @c1xgraal'
                        sh '$MX image -platform linux-aarch64 -isa Aarch64 --build=DEBUG'
                        sh '$MX image -platform linux-arm -isa ARMV7 --build=DEBUG'
                        sh '$MX image'
                    }
                }, 'test-init': {
                    dir(env.MAXINE_HOME) {
                        sh '$MX jttgen'
                        sh '$MX canonicalizeprojects'
                    }
                }
            }
        }
        stage('test-n-crossisa') {
            steps {
                parallel 'test': {
                    dir(env.MAXINE_HOME) {
                        sh '$MX test -image-configs=java -tests=c1x,graal,junit:test.com,output'
                        sh '$MX test -maxvm-configs=jsr292 -image-configs=java -tests=jsr292'
                        sh '$MX test -image-configs=ss -tests=output:Hello+Catch+GC+WeakRef+Final'
                    }
                }, 'crossisa': {
                    dir(env.MAXINE_HOME) {
                        sh '$MX --J @"-Dmax.platform=linux-aarch64 -Dtest.crossisa.qemu=1 -ea" test -s=t -junit-test-timeout=1800 -tests=junit:aarch64.asm+Aarch64T1XTest+Aarch64T1XpTest+Aarch64JTT'
                        sh '$MX --J @"-Dmax.platform=linux-arm -Dtest.crossisa.qemu=1 -ea" test -s=t -junit-test-timeout=1800 -tests=junit:armv7.asm+ARMV7T1XTest+ARMV7JTT'
                        sh '$MX --J @"-Dmax.platform=linux-riscv64 -Dtest.crossisa.qemu=1 -ea" test -s=t -tests=junit:riscv64.asm'
                    }
                }
            }
        }
        stage('javatester') {
            steps {
                dir(env.MAXINE_HOME) {
                    sh '$MX test -image-configs=java -tests=javatester'
                }
            }
        }
    }

    post {
        success {
            slackSend color: '#00CC00', message: "SUCCESSFUL: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})"
        }
        failure {
            slackSend color: '#CC0000', message: "FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]' (${env.BUILD_URL})"
        }
    }
}
