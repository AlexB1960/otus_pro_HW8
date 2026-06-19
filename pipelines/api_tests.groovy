
timeout(60) {  //время сборки в секундах
   node("ansible") { //значение поля Labels из настроек в Docker Agent templates
     currentBuild.description = "Running api-tests on Jenkins" //заголовок у каждой джобы

     stage("Running api-tests on Jenkins") { //выполняем запуск апи-тестов
        sh "docker run tests_api:1.0"
     }
   }
}
