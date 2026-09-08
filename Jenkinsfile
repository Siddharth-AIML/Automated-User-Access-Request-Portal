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
                bat 'mvnw.cmd clean compile -DskipTests'
            }
        }

        stage('Package') {
            steps {
                echo "Packaging the Spring Boot application..."
                bat 'mvnw.cmd package -DskipTests'
            }
        }

        stage('Start Test Server') {

    steps {

        echo "Starting application for Selenium testing..."

        powershell '''
            $ErrorActionPreference = "Stop"

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

                Write-Host "Stopping existing application..."

                $connection |
                    Select-Object -ExpandProperty OwningProcess -Unique |
                    ForEach-Object {

                        Write-Host "Stopping PID $_"

                        Stop-Process `
                            -Id $_ `
                            -Force `
                            -ErrorAction SilentlyContinue
                    }

                Start-Sleep -Seconds 2
            }

            Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
            Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

            Write-Host "Starting test application..."

            $process = Start-Process `
                -FilePath $java `
                -ArgumentList "-jar `"$jar`"" `
                -WorkingDirectory "$env:WORKSPACE" `
                -RedirectStandardOutput $outputLog `
                -RedirectStandardError $errorLog `
                -WindowStyle Hidden `
                -PassThru

            Write-Host "Spring Boot PID: $($process.Id)"
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

                if ($process.HasExited) {

                    Write-Host "Application process exited unexpectedly."

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
            }

            if (-not $started) {

                Write-Host "Application failed to start within 30 seconds."

                Write-Host "===== APPLICATION OUTPUT ====="

                if (Test-Path $outputLog) {
                    Get-Content $outputLog -Tail 50
                }

                Write-Host "===== APPLICATION ERROR ====="

                if (Test-Path $errorLog) {
                    Get-Content $errorLog -Tail 50
                }

                if (-not $process.HasExited) {
                    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
                }

                exit 1
            }

            Write-Host "Test application started successfully."
            Write-Host "Test server PID: $($process.Id)"
            Write-Host "Proceeding to Selenium Tests..."

            # Explicitly finish the PowerShell step
            exit 0
        '''
    }
}

        stage('Selenium Tests') {
    options {
        timeout(time: 3, unit: 'MINUTES')
    }

    steps {
        echo "===== SELENIUM STAGE STARTED ====="

        bat '''
            echo ===== JAVA VERSION =====
            java -version

            echo ===== MAVEN VERSION =====
            mvnw.cmd -version

            echo ===== CHECKING APPLICATION =====
            powershell -Command "Get-NetTCPConnection -LocalPort 8081 -State Listen"

            echo ===== STARTING SELENIUM MAVEN TEST =====
            mvnw.cmd -Dtest=PortalSeleniumTests test

            echo ===== SELENIUM MAVEN COMMAND FINISHED =====
        '''

        echo "===== SELENIUM STAGE COMPLETED ====="
    }
}

        stage('Stop Test Server') {
            steps {

                echo "Stopping test application..."

                powershell '''
                    $connection = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connection) {

                        Write-Host "Stopping test application..."

                        Stop-Process `
                            -Id $connection.OwningProcess `
                            -Force `
                            -ErrorAction SilentlyContinue

                        Start-Sleep -Seconds 2

                        Write-Host "Test application stopped."
                    }
                    else {

                        Write-Host "No test application found."
                    }
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

                    Write-Host "Checking port 8081..."

                    $connection = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connection) {

                        Write-Host "Stopping existing application..."

                        Stop-Process `
                            -Id $connection.OwningProcess `
                            -Force `
                            -ErrorAction SilentlyContinue

                        Start-Sleep -Seconds 2
                    }

                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    Write-Host "Starting deployed application..."

                    Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "C:\\deploy" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden

                    Write-Host "Waiting for deployment startup..."

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

                        Write-Host "Deployment application failed to start."

                        Write-Host "===== DEPLOYMENT OUTPUT ====="

                        if (Test-Path $outputLog) {
                            Get-Content $outputLog -Tail 50
                        }

                        Write-Host "===== DEPLOYMENT ERROR ====="

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
        }

        success {

            echo '======================================'
            echo 'WEEK 10 PIPELINE SUCCESS'
            echo 'Selenium tests passed.'
            echo 'Application deployed successfully.'
            echo '======================================'
        }

        failure {

            echo '======================================'
            echo 'WEEK 10 PIPELINE FAILED'
            echo 'Deployment was stopped.'
            echo '======================================'
        }

        aborted {

            echo '======================================'
            echo 'WEEK 10 PIPELINE ABORTED'
            echo '======================================'
        }
    }
}