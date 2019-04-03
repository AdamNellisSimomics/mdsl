pipeline {
    agent any
    environment {
        PROJECT_NAME = "artoo_homepage"
        PORT = "3500"
    }
    stages {
      stage('Build') {
          steps {
              sh 'gradle -b leish_simulator/build.gradle build'

          }
      }
        stage('Test'){
            steps {
			  sh 'gradle -b leish_simulator/build.gradle test'
              sh 'gradle -b leish_simulator/build.gradle javadoc'
              sh 'gradle -b leish_simulator/build.gradle jarDiagramGenerator'
            }
        }
        stage('Deploy') {
            steps {
                sh 'echo publish'
            }
        }
    }
}
