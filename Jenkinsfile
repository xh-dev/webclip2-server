def project_version
def setStatus(status){
    def m = '{"state": "'+status+'","context": "continuous-integration/jenkins", "description": "Jenkins", "target_url": "https://jks.xh-network.xyz/job/webclip2-server/'+env.BUILD_NUMBER+'/console"}'
    m = m.replaceAll("\"", "\\\\\"")
    msg = "curl -i -s \"https://api.GitHub.com/repos/$GITHUB_CREDENTIALS_USR/webclip2-server/statuses/$GIT_COMMIT\" -H \"Authorization: token $GITHUB_CREDENTIALS_PSW\" -H \"Content-Type: application/json\" -X POST -d \"$m\""
    //echo "$msg"
    sh msg
}

pipeline {
    agent any

    environment {
        DOCKERHUB_CREDENTIALS=credentials('dockerhub-id')
        GITHUB_CREDENTIALS=credentials('github-id')
        DEPLOY_CREDENTIALS=credentials('ssh-deployment')
    }
	
    stages {
        stage('Init') {
            environment {
                C_VERSION = sh(returnStdout: true, script: 'cat build.sbt | grep -E "^[ ]*version[ ]*:=[ ]*\\"([^\\"]+)\\"$" | sed -e "s/version[ ]*:=[ ]*\\"\\(.*\\)\\"/\\1/g"').trim()
                branchName= sh (returnStdout: true, script: 'echo $GIT_BRANCH').trim()
                commitId= sh (returnStdout: true, script: 'echo $GIT_COMMIT').trim()
            }
            steps {
                script {
                    setStatus("pending")
                    project_version = sh(returnStdout: true, script: 'echo $C_VERSION')
                }
            }
        }
        stage('Build') {
            environment { 
                branchName= sh (returnStdout: true, script: 'echo $GIT_BRANCH').trim()
                commitId= sh (returnStdout: true, script: 'echo $GIT_COMMIT').trim()
            }
            steps {
                if(env.branchName == "origin/master"){
                    sh 'printenv'
                    sh 'docker build --build-arg branchName=$GIT_BRANCH --build-arg commitId=$GIT_COMMIT -t xethhung/webclip2-server:latest .'
                    echo 'build complete'
                } else {
                    echo "Branch[$branchName] no need to build image"
                }
            }
        }
        stage('Publish') {
            environment {
                branchName= sh (returnStdout: true, script: 'echo $GIT_BRANCH').trim()
                commitId= sh (returnStdout: true, script: 'echo $GIT_COMMIT').trim()
            }
            steps {
                if(env.branchName == "origin/master"){
                    sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
                    sh 'docker push $DOCKERHUB_CREDENTIALS_USR/webclip2-server:latest'
                    sh 'docker tag $DOCKERHUB_CREDENTIALS_USR/webclip2-server:latest xethhung/webclip2-server:'+project_version
                    sh 'docker push $DOCKERHUB_CREDENTIALS_USR/webclip2-server:'+project_version
                } else {
                    echo "Branch[$branchName] no need to publish"
                }
            }
        }
        stage('Deploy') {
            environment {
                branchName= sh (returnStdout: true, script: 'echo $GIT_BRANCH').trim()
                commitId= sh (returnStdout: true, script: 'echo $GIT_COMMIT').trim()
            }
            steps {
                if(env.branchName == "origin/master"){
                    withCredentials([string(credentialsId: 'deployment-host', variable: 'host')]) {
                        sshagent(credentials: ['ssh-deployment']){
                            sh "ssh -i /ssh/id_rsa $DEPLOY_CREDENTIALS_USR@$host /data/webclip2-server/refresh.sh"
                        }
                    }
                } else {
                    echo "Branch[$branchName] no need to redeploy"
                }
            }
        }
    }
    post {
        always {
            sh 'docker logout'
        }
        success {
            setStatus("success")
        }
        failure {
            setStatus("failure")
        }
    }    
}
