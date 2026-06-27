
timeout(500) {
   node("ansible") {
     currentBuild.description = "Running appium-tests on Jenkins"

     stage("Running appium-tests on Jenkins") {
        sh "docker run --shm-size=4gb --rm --network host tests_appium:1.0"
     }
   }
}
