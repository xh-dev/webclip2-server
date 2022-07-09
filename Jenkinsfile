pipeline {
    agent any

    stages {
        stage('Info') {
            steps {
                sh 'printenv'
                echo '$BRANCH_NAME'
                echo '$evn.GIT_BRANCH'
                echo '$GIT_BRANCH'
            }
        }
        stage('Build') {
            steps {
                sh 'docker build -t webclip2-server:latest .'
            }
        }

    }
}
