pipeline {

    agent any

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['development', 'staging'],
            description: 'Select the deployment environment'
        )
    }

    stages {

        stage('Checkout') {
            steps {
                echo "Checking out source code..."
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo "Building the application..."
                bat 'mvnw.cmd clean compile -DskipTests'
            }
        }

        stage('Package') {
            steps {
                echo "Packaging the Spring Boot application..."
                bat 'mvnw.cmd package -DskipTests'
            }
        }

        stage('Deploy') {
            steps {
                echo "Deploying application to ${params.DEPLOY_ENV} environment..."

                bat '''
                    if not exist C:\\deploy mkdir C:\\deploy

                    copy /Y target\\accessportal-0.0.1-SNAPSHOT.jar C:\\deploy\\accessportal.jar

                    powershell -NoProfile -Command "$c=Get-NetTCPConnection -LocalPort 8081 -State Listen -ErrorAction SilentlyContinue; if ($c) { Stop-Process -Id $c.OwningProcess -Force }"

                    start "AccessPortal" /B java -jar C:\\deploy\\accessportal.jar > C:\\deploy\\accessportal.log 2>&1
                '''

                echo "Application deployed successfully."
                echo "Environment: ${params.DEPLOY_ENV}"
                echo "Application URL: http://localhost/login"
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully.'
        }

        failure {
            echo 'Pipeline failed. Check the Jenkins console output.'
        }
    }
}