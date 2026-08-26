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
                '''

                powershell '''
                    $java = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"
                    $jar = "C:\\deploy\\accessportal.jar"
                    $outputLog = "C:\\deploy\\accessportal-output.log"
                    $errorLog = "C:\\deploy\\accessportal-error.log"

                    Write-Host "Checking whether port 8081 is already in use..."

                    $connection = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connection) {
                        Write-Host "Stopping existing application process..."

                        Stop-Process `
                            -Id $connection.OwningProcess `
                            -Force

                        Start-Sleep -Seconds 3
                    }

                    if (Test-Path $outputLog) {
                        Remove-Item $outputLog -Force
                    }

                    if (Test-Path $errorLog) {
                        Remove-Item $errorLog -Force
                    }

                    Write-Host "Starting Spring Boot application..."

                    Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "C:\\deploy" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden

                    Write-Host "Waiting for application startup..."

                    Start-Sleep -Seconds 15

                    $running = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if (-not $running) {

                        Write-Host "Application failed to start."

                        Write-Host "===== APPLICATION OUTPUT ====="

                        if (Test-Path $outputLog) {
                            Get-Content $outputLog -Tail 50
                        }

                        Write-Host "===== APPLICATION ERROR ====="

                        if (Test-Path $errorLog) {
                            Get-Content $errorLog -Tail 50
                        }

                        exit 1
                    }

                    Write-Host "Application is running successfully on port 8081."
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