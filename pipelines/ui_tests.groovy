
timeout(500) {
   node("ansible") {
     currentBuild.description = "Running ui-tests on Jenkins"

     stage("Running ui-tests on Jenkins") {
        sh "docker run tests_ui:1.0"
     }
   }
}
