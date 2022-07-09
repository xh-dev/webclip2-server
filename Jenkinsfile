pipeline {
    agent any

    stages {
        stage('Info') {
            environment { 
                BRANCHNAME= sh (returnStdout: true, script: 'echo \${env.GIT_BRANCH}').trim()
            }
            steps {
                echo "Running ${env.BUILD_ID} on ${env.JENKINS_URL}"
                echo "BRANCHNAME = ${env.BRANCHNAME}"
                sh 'printenv'
            }
        }
        stage('Build') {
            steps {        
                sh 'printenv'
                echo 'echo $BRANCHNAME'
                sh 'build complete'
            }
        }

    }
}
