pipeline {
	agent any
	environment { 
	def ITEMNAME = "webapp"
	def DESTPATH = "/data/wwwroot"
	def SRCPATH = "~/workspace/test"
	def BUILD_USER = "mark"
	}
	
	stages {	
		stage('代码拉取'){
			steps {
			echo "checkout from ${ITEMNAME}"
			git url: 'git@git.ds.com:mark/maxtest.git', branch: 'master'
			//git credentialsId:CRED_ID, url:params.repoUrl, branch:params.repoBranch
					}
					}	
		stage('服务检查') {
			steps {
				echo "检查nginx进程是否存在"
				script{
					def resultUpdateshell = sh script: 'ansible webapp -m shell -a "ps aux|grep nginx|grep -v grep"'
					currentBuild.result = 'FAILED'
					if (resultUpdateshell == 0) {
						skip = '0'
						return
					}	
					}
					}
					}		
		}
   post {
        always {
          step([$class: 'Mailer',
            notifyEveryUnstableBuild: true,
            recipients: "myname@gmail.com",
            sendToIndividuals: true])
        }
    }
}
