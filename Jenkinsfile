pipeline {

    agent any

    options {
        skipStagesAfterUnstable()
        disableConcurrentBuilds()
        timestamps()
    }

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

                bat '''
                    echo ===== JAVA VERSION =====
                    java -version

                    echo ===== BUILD =====
                    mvnw.cmd clean compile -DskipTests
                '''
            }
        }

        stage('Package') {
            steps {
                echo "Packaging the Spring Boot application..."

                bat '''
                    mvnw.cmd package -DskipTests
                '''
            }
        }

        stage('Start Test Server') {
            steps {

                echo "Starting application for Selenium testing..."

                powershell '''
                    $java = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"
                    $jar = "$env:WORKSPACE\\target\\accessportal-0.0.1-SNAPSHOT.jar"

                    $outputLog = "$env:WORKSPACE\\target\\selenium-output.log"
                    $errorLog = "$env:WORKSPACE\\target\\selenium-error.log"

                    Write-Host "Checking port 8081..."

                    $connection = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connection) {

                        Write-Host "Stopping existing application on port 8081..."

                        Stop-Process `
                            -Id $connection.OwningProcess `
                            -Force

                        Start-Sleep -Seconds 2
                    }

                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    Write-Host "Starting test application..."

                    Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "$env:WORKSPACE" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden

                    Write-Host "Waiting for application startup..."

                    $started = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 1

                        $running = Get-NetTCPConnection `
                            -LocalPort 8081 `
                            -State Listen `
                            -ErrorAction SilentlyContinue

                        if ($running) {

                            $started = $true

                            Write-Host "Application detected on port 8081 after $i seconds."

                            break
                        }
                    }

                    if (-not $started) {

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

                    Write-Host "Test application started successfully."
                    Write-Host "Proceeding to Selenium Tests..."
                '''
            }
        }

        stage('Selenium Tests') {

            options {
                timeout(
                    time: 3,
                    unit: 'MINUTES'
                )
            }

            steps {

                echo "Running Selenium UI tests..."

                bat '''
                    echo ===== SELENIUM ENVIRONMENT =====

                    echo Chrome version:
                    "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe" --version

                    echo ===== STARTING SELENIUM TESTS =====

                    mvnw.cmd -Dtest=PortalSeleniumTests test

                    echo ===== SELENIUM TESTS FINISHED =====
                '''
            }
        }

        stage('Deploy') {

            steps {

                echo "All Selenium tests passed."
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

                        Start-Sleep -Seconds 2
                    }

                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    Write-Host "Starting Spring Boot application..."

                    Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "C:\\deploy" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden

                    Write-Host "Waiting for application startup..."

                    $started = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 1

                        $running = Get-NetTCPConnection `
                            -LocalPort 8081 `
                            -State Listen `
                            -ErrorAction SilentlyContinue

                        if ($running) {

                            $started = $true

                            Write-Host "Application detected on port 8081 after $i seconds."

                            break
                        }
                    }

                    if (-not $started) {

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

                    Write-Host "Application deployed successfully on port 8081."
                '''

                echo "Environment: ${params.DEPLOY_ENV}"
                echo "Application URL: http://localhost:8081/login"
            }
        }
    }

    post {

        always {

            echo "Publishing Selenium test reports..."

            junit(
                testResults: 'target/surefire-reports/*.xml',
                allowEmptyResults: true
            )

            archiveArtifacts(
                artifacts: 'target/screenshots/*.png',
                allowEmptyArchive: true
            )

            archiveArtifacts(
                artifacts: 'target/selenium-*.log',
                allowEmptyArchive: true
            )

            echo "Cleaning up test application..."

            powershell '''

                $connection = Get-NetTCPConnection `
                    -LocalPort 8081 `
                    -State Listen `
                    -ErrorAction SilentlyContinue

                if ($connection) {

                    Write-Host "Stopping application running on port 8081..."

                    Stop-Process `
                        -Id $connection.OwningProcess `
                        -Force

                    Start-Sleep -Seconds 2

                    Write-Host "Test application stopped."
                }
                else {

                    Write-Host "No application process found on port 8081."
                }
            '''
        }

        success {

            echo "========================================"
            echo "WEEK 10 PIPELINE SUCCESS"
            echo "========================================"

            echo "Selenium tests passed."
            echo "Test reports published."
            echo "Application deployed successfully."
        }

        failure {

            echo "========================================"
            echo "WEEK 10 PIPELINE FAILED"
            echo "========================================"

            echo "Selenium tests or another pipeline stage failed."
            echo "Deployment was stopped."
            echo "Check the Jenkins test report and console output."
        }

        aborted {

            echo "========================================"
            echo "WEEK 10 PIPELINE ABORTED"
            echo "========================================"

            echo "Pipeline execution was manually aborted."
        }
    }
}