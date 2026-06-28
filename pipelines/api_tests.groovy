
timeout(120) {
   node("ansible") {
     currentBuild.description = "Running api-tests on Jenkins"

     def params = readYaml text: env.YAML_CONFIG ?: [:] //считываем YAML-параметры

     try {
         stage("Checkout") {
             checkout scm  //стягиваем проект
         }
         stage("Build Docker image") {
             docker.withRegistry("http://localhost:5005") { //в Registry, который прилагается к дженкинсу
                 //docker.build("otus-allure:${env.REFSPEC}").push() //собираем докер-образ с именем(имя проекта:имя ветки) и пушим его в Registry для дальнейшего запуска
                 docker.build("api_tests:1.1").push()
             }
         }
     }  finally {
         cleanWs(deleteDirs: true)
     }

     dir("${params.PROFILE}") {
         try {
             stage("Checkout") {
                 checkout scm  //стягиваем проект
             }
             stage("Running api-tests") {
                 //sh "pwd"
                 //sh "ls -la"
                 ansiblePlaybook playbook: "playbook.yml", //плейбука, которая запускает тесты (и разворачивает инфраструктуру?)
                 //        installation: "Ansible",
                         extraVars: [
                                 branch : "${env.REFSPEC}", //передаем BRANCH в плейбуку ветку, из которой запускаем
                                 profile: "${params.PROFILE}" //передаем в плейбуку, какие именно тесты запускаем (ui/api/appium)
                         ]
                 //sh "docker run --rm tests_api:1.0"
                 //sh "docker run --rm api_tests:1.1"
             }
             stage("Allure report") {
                 sh "tar -czf allure-results.tar.gz -C allure-results ." //архивация json-файлов текущещей джобы в tar-архив
                 archiveArtifacts artifacts: "*.tar.gz", //пушим архив как артифакт текущей джобы
                         allowEmptyArchive: true, //пустой архив разрешается к пушу
                         fingerprint: true,
                         onlyIfSuccessful: true //только если джоба прошла успешно (не упала), но тестам разрешено падать
                 allure(
                         results: [[path: "allure-results"]], //результаты искать в папке allure-results
                         disabled: false,
                         reportBuildPolicy: "ALWAYS" //всегда включаем и всегда собираем
                 )
             }
             stage("Send notification") { //отправка сообщения на Mattermost
                 def summary = junit testResults: "**/surefire-reports/*.xml" //забрали общую статистику, из которой далее составили части сообщения
                 String message = """Test Summary
                                        |JOB: ${env.JOB_NAME}
                                        |${currentBuild.description}
                                        |
                                        |Total: ${summary.totalCount}
                                        |Passed: ${summary.passCount}
                                        |Failed: ${summary.failCount}
                                        |Skipped: ${summary.skipCount}
                                        |
                                        |See [full report](${env.BUILD_URL}allure) for details."""
                         .stripMargin()
                 withCredentials([ //из Jenkins Credentials
                                   string(credentialsId: "mattermost-webhook", variable: "WEBHOOK"), //какую переменную записать в строку, т.е. WEBHOOK=адрес из МАТТЕРМОСТа
                                   string(credentialsId: "mattermost-users-to-notify", variable: "USERS") //для имен пользователей
                 ]) {
                     env.USERS.tokenize(",").each { username ->  //разбиваем список пользователей через запятую и отправляем каждому наше сообщение
                         httpRequest consoleLogResponseBody: true, //в консоль запишем ответ, успешно или неуспешно отправилось сообщение
                                 contentType: "APPLICATION_JSON",
                                 httpMode: "POST",
                                 requestBody:"{\"text\":\"$message\",\"channel\":\"$username\",\"username\":\"Jenkins\"}",
                                 url: "${env.WEBHOOK}" //куда отправляем сообщение
                     }

                 }
             }
         } finally {
             deleteDir()
         }
     }
   }
}