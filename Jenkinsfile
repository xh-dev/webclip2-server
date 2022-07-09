def project_version
pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS=credentials('dockerhub-id')
    }
	
    stages {
        stage('Info') {
            environment { 
                C_VERSION = sh(returnStdout: true, script: 'cat build.sbt | grep -E "^[ ]*version[ ]*:=[ ]*\\"([^\\"]+)\\"$" | sed -e "s/version[ ]*:=[ ]*\\"\\(.*\\)\\"/\\1/g"').trim()
                branchName= sh (returnStdout: true, script: 'echo $GIT_BRANCH').trim()
                commitId= sh (returnStdout: true, script: 'echo $GIT_COMMIT').trim()
            }
            steps {
                sh 'printenv'
                script {
                    sh 'echo $C_VERSION'
                    project_version = '$C_VERSION'
                }
            }
        }
        stage('Build') {
            environment { 
                branchName= sh (returnStdout: true, script: 'echo $GIT_BRANCH').trim()
                commitId= sh (returnStdout: true, script: 'echo $GIT_COMMIT').trim()
            }
            steps {
                sh 'cat build.sbt'
                sh 'docker build -t xethhung/webclip2-server:latest .'
                echo 'build complete'
            }
        }
        stage('Deploy') {
            steps {
                sh 'printenv'
                sh 'echo $project_version'
                sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
                sh 'docker push $DOCKERHUB_CREDENTIALS_USR/webclip2-server:latest'
                sh 'docker tag $DOCKERHUB_CREDENTIALS_USR/webclip2-server:latest xethhung/webclip2-server:${project_version}'
                sh 'docker push $DOCKERHUB_CREDENTIALS_USR/webclip2-server:${project_version}'
                echo 'build complete'
            }
        }
    }
    post {
        always {
            sh 'docker logout'
        }
    }    
}
