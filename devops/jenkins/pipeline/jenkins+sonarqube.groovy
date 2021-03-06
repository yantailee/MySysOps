pipeline {
	agent any
	parameters { string(defaultValue: '', name: 'GIT_TAG', description: '请根据发布类型进行选择发布：\n1，输入-TESTING-发布-最新代码-到灰度\n2，输入-LATEST-发布-最新代码-到生产\n3，输入-版本号-发布-制定版本-到生产 ' ) }
	environment { 
	def ITEMNAME = "webapp"
	def ITEMNAME2 = "twebapp" 
	def DESTPATH = "/data/wwwroot"
	def SRCPATH = "~/workspace/linuxea-3-sonarqube"
	}
	stages {	
		stage('代码拉取'){
			steps {
			echo "checkout from ${ITEMNAME}"
			git url: 'git@git.ds.com:mark/maxtest.git', branch: 'master'
			//git credentialsId:CRED_ID, url:params.repoUrl, branch:params.repoBranch
					}
					}
        stage('SonarQube')	{
			steps {
				echo "starting codeAnalyze with SonarQube......"
               //sonar:sonar.QualityGate should pass
               withSonarQubeEnv('SonarQube') {
                 //固定使用项目根目录${basedir}下的pom.xml进行代码检查
                 sh "/usr/local/sonar-scanner/bin/sonar-scanner"
               }
               script {
               timeout(10) { 
                   //利用sonar webhook功能通知pipeline代码检测结果，未通过质量阈，pipeline将会fail
                   def qg = waitForQualityGate() 
                       if (qg.status != 'OK') {
                           error "未通过Sonarqube的代码质量阈检查，请及时修改！failure: ${qg.status}"
                       }
                   }
               }
           }
       }				
			
		stage('目录检查') {
			steps {
				echo "检查${DESTPATH}目录是否存在"
				script{
					def resultUpdateshell = sh script: 'ansible webapp -m shell -a "ls -d ${DESTPATH}"'
					//def resultUpdateshell = sh script: 'ansible twebapp -m shell -a "ls -d ${DESTPATH}"'
					if (resultUpdateshell == 0) {
						skip = '0'
						return
					}	
					}
					}
					}		
		stage('服务检查') {
			steps {
				echo "检查nginx进程是否存在"
				script{
					def resultUpdateshell = sh script: 'ansible webapp -m shell -a "ps aux|grep nginx|grep -v grep"'
					//def resultUpdateshell = sh script: 'ansible twebapp -m shell -a "ps aux|grep nginx|grep -v grep"'					
					if (resultUpdateshell == 0) {
						skip = '0'
						return
					}	
					}
					}
					}
        stage('发布确认') {
            steps {
                input "检查完成，是否发布?"
            }
        }					
		stage('代码推送') {
		    steps {
			echo "code sync"
				script {
					if (env.GIT_TAG == 'TESTING') {
						echo 'TESTING'
							sh "ansible ${ITEMNAME2} -m synchronize -a 'src=${SRCPATH}/ dest=${DESTPATH}/ rsync_opts=-avz,--exclude=.git,--delete'"
						} else {
						if (env.GIT_TAG == 'LATEST') {
							echo 'LATEST'
							sh "ansible ${ITEMNAME} -m synchronize -a 'src=${SRCPATH}/ dest=${DESTPATH}/ rsync_opts=-avz,--exclude=.git,--delete'"						
						} else { 
							sh """
							git checkout ${GIT_TAG}
							ansible ${ITEMNAME} -m synchronize -a 'src=${SRCPATH}/ dest=${DESTPATH}/ rsync_opts=-avz,--exclude=.git,--delete'
							"""
						}
						}
					}
					}
					}
	}
}
