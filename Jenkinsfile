pipeline {
    agent any

    stages {
        stage('Info') {
            environment {
                
            }
            steps {
                echo "BUILD_NUMBER = ${env.BUILD_NUMBER}"
                sh 'echo BUILD_NUMBER = $BUILD_NUMBER'
                sh 'printenv'
                echo '$BUILD_NUMBER'
                sh 'echo ${env.GIT_BRANCH}'
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
