pipeline {
    agent any

    stages {
        stage('Info') {
            environment { 
                branchName= sh (returnStdout: true, script: 'echo $GIT_BRANCH').trim()
                commitId= sh (returnStdout: true, script: 'echo $GIT_COMMIT').trim()
            }
            steps {
                sh 'printenv'
            }
        }
        stage('Build') {
            steps {        
                sh 'printenv'
                sh 'build complete'
            }
        }

    }
}
