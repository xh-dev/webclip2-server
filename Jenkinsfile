pipeline {
    agent any

    stages {
        stage('Info') {
            steps {
                sh 'printenv'
                echo '$BRANCH_NAME'
                sh 'echo \${env.GIT_BRANCH}'
            }
        }
        stage('Build') {
            steps {            
                sh 'build complete'
            }
        }

    }
}
