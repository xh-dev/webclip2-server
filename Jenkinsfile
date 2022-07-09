pipeline {
    agent any

    stages {
        stage('Info') {
            steps {
                sh 'printenv'
                echo '$BRANCH_NAME'
                echo '$GIT_BRANCH'
                echo 'Hello World'
            }
        }
        stage('Build') {
            steps {
                sh 'docker build -t webclip2-server:latest .'
            }
        }

    }
}
