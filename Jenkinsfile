pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS=credentials('dockerhub-id')
    }
    stages {
        stage('Info') {
            steps {
                sh 'printenv'
            }
        }
        stage('Build') {
            environment { 
                branchName= sh (returnStdout: true, script: 'echo $GIT_BRANCH').trim()
                commitId= sh (returnStdout: true, script: 'echo $GIT_COMMIT').trim()
            }
            steps {        
                sh 'docker build -t xethhung/webclip2-server:latest .'
                echo 'build complete'
            }
        }
        stage('Deploy') {
            steps {
                sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
                sh 'docker push xethhung/webclip2-server:latest'
                sh 'docker tag xethhung/webclip2-server:latest xethhung/webclip2-server:1.0'
                sh 'docker push xethhung/webclip2-server:1.0'
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
