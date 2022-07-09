pipeline {
    agent any

    stages {
        stage('Info') {
            steps {
                echo "BRANCH_NAME = ${env.BRANCH_NAME}"
                sh 'echo BUILD_NUMBER = $BRANCH_NAME'
                sh 'printenv'
                echo '$BRANCH_NAME'
            }
        }
        stage('Build') {
            steps {        
                sh 'printenv'
                echo '$BRANCH_NAME'
                sh 'build complete'
            }
        }

    }
}
