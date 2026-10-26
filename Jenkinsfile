pipeline {
    agent any

    tools {
        maven 'Maven-3.9.6'
        jdk 'JDK-21'
    }

    environment {
        DOCKER_IMAGE = 'servicedesk:1.0.0'
        SONAR_HOST_URL = 'http://localhost:9000'
    }

    stages {
        stage('Checkout Source') {
            steps {
                echo 'Checking out source code from Git repository...'
                checkout scm
            }
        }

        stage('Build & Unit Test') {
            steps {
                echo 'Compiling codebase and executing JUnit 5 test suite...'
                sh 'mvn clean test jacoco:report'
            }
            post {
                always {
                    junit 'target/surefire-reports/*.xml'
                }
            }
        }

        stage('SonarQube Static Analysis') {
            steps {
                echo 'Running static code analysis and transmitting report to SonarQube...'
                withSonarQubeEnv('Local-SonarQube') {
                    sh 'mvn sonar:sonar -Dsonar.host.url=${SONAR_HOST_URL} -Dsonar.projectKey=servicedesk-app'
                }
            }
        }

        stage('Quality Gate Verification') {
            steps {
                echo 'Checking SonarQube Quality Gate threshold status...'
                timeout(time: 2, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Docker Image Build') {
            steps {
                echo 'Building production multi-stage Docker container image...'
                sh 'docker build -t ${DOCKER_IMAGE} .'
            }
        }

        stage('Deploy Container') {
            steps {
                echo 'Deploying ServiceDesk container on port 8080...'
                sh '''
                    docker rm -f servicedesk-app 2>/dev/null || true
                    docker run -d --name servicedesk-app -p 8080:8080 ${DOCKER_IMAGE}
                '''
            }
        }

        stage('Release Tagging') {
            steps {
                echo 'Creating release version tag v1.0.0...'
                sh '''
                    git tag -a v1.0.0 -m "release: verified production build v1.0.0" || true
                '''
            }
        }
    }

    post {
        success {
            echo '=================================================='
            echo ' ServiceDesk CI/CD Pipeline Completed Successfully! '
            echo '=================================================='
        }
        failure {
            echo 'Pipeline failed. Check stage logs for troubleshooting.'
        }
    }
}
