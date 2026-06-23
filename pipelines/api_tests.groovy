
timeout(120) {
   node("ansible") {
     currentBuild.description = "Running api-tests on Jenkins"

     def params = readYaml text: env.YAML_CONFIG ?: [:] //считываем YAML-параметры

     stage("Checkout") {
         checkout scm  //стягиваем проект
     }
     stage("Build Docker image") {
         docker.withRegistry("http://localhost:5005") { //в Registry, который прилагается к дженкинсу
             //docker.build("otus-allure:${env.REFSPEC}").push() //собираем докер-образ с именем(имя проекта:имя ветки) и пушим его в Registry для дальнейшего запуска
             docker.build("api_tests:1.1").push()
         }
     }
     stage("Running api-tests") {
        ansiblePlaybook playbook: "playbook.yml", //плейбука, которая запускает тесты (и разворачивает инфраструктуру)
                extraVars: [
                        branch : "${env.REFSPEC}", //передаем BRANCH в плейбуку ветку, из которой запускаем
                        profile: "${params.PROFILE}" //передаем в плейбуку, какие именно тесты запускаем (ui/api/appium)
                ]
        //sh "docker run --rm tests_api:1.0"
     }
     stage("Allure report") {
         sh "tar -czf ajjure-results.tar.gz -C allure-results ." //архивация json-файлов текущещей джобы в tar-архив
         archiveArtifacts artifacts: "*.tar.gz", //пушим архив как артифакт текущей джобы
                 allowEmptyArchive: true, //пустой архив разрешается к пушу
                 fingerprint: true,
                 onlyIfSuccessful: true //только если джоба прошла успешно (не упала), но тестам разрешено падать
         allure(
                 results: [[path: "allure-results"]], //результаты искать в папке allure-results
                 disable: false,
                 reportBuildPolicy: "ALWAYS" //всегда включаем и всегла собираем
         )
     }
   }
}
