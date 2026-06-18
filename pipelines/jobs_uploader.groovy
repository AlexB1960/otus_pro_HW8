
timeout(60) {  //время сборки в секундах
   node("ansible") { //значение поля Labels из настроек в Docker Agent templates
     currentBuild.description = "Upload jobs on Jenkins" //заголовок у каждой джобы

     stage("Create config file") { //создание конфиг-файла
       //забираем креды, созданные нами в Jenkins-Credentials. они хранятся только в пределах этих(внизу) фигурных скобок
       withCredentials([usernamePassword(credentialsId: 'jenkins', usernameVariable: 'JENKINS_USER', passwordVariable: 'JENKINS_PASSWORD')]) {
         writeFile file: './config.ini', test: """[jenkins]
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

     stage("Upload jobs on Jenkins") { //выполняем накатку джоб в дженкинс
        sh "jenkins-jobs --conf ./config.ini --flush-cache update ./jobs" // ./jobs - это путь, где у нас лежат джобы
     }
   }
}
