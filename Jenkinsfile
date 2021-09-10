pipeline {
    agent any

    environment {
        MVN_CREDS = credentials('maven-creds')
    }

    tools {
        maven 'Maven'
        jdk 'JDK'
    }

    stages {
        stage ('Initialize') {
            steps {
                sh '''
                    echo "PATH = ${PATH}"
                    echo "M2_HOME = ${M2_HOME}"
                '''
            }
            }
        stage('Build') {
             steps {
                 sh 'mvn -Dmaven.test.failure.ignore=true -s $MVN_CREDS clean package'
             }
             post {
                success {
                    archiveArtifacts artifacts: 'target/AuroraMC-Mission-Control-**.jar', followSymlinks: false
                }
             }
        }
    }
}