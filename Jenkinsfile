pipeline {
    agent any

    stages {
        stage('Info') {
            steps {
                echo "Running ${env.BUILD_ID} on ${env.JENKINS_URL}"
                echo "BRANCHNAME = ${env.BRANCH_NAME}"
                sh 'echo BUILD_NUMBER = $BRANCHNAME'
                sh 'printenv'
                echo 'echo $BRANCHNAME'
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
