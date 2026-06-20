
timeout(60) {
   node("ansible") {
     currentBuild.description = "Upload jobs on jenkins"

     stage("Checkout") {
       checkout scm
     }

     stage("Create config file") {
       withCredentials([usernamePassword(credentialsId: 'jenkins', usernameVariable: 'JENKINS_USER', passwordVariable: 'JENKINS_PASSWORD')]) {
         writeFile file: './config.ini', text: """[jenkins]
           url=${env.JENKINS_URL}
           user=${JENKINS_USER}
           password=${JENKINS_PASSWORD}

        [job_builder]
        recursive=True
        keep_description=True
        ignore_cache=True
        update=all
"""
       }
     }

     stage("Upload jobs on jenkins") {
        sh "jenkins-jobs --conf ./config.ini --flush-cache update ./jobs"
     }
   }
}