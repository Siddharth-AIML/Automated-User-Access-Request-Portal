pipeline {

    agent any

    options {
        skipStagesAfterUnstable()
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 10, unit: 'MINUTES')
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['development', 'staging'],
            description: 'Select the deployment environment'
        )
    }

    environment {
        APP_PORT = '8081'
        TEST_SERVER_PID_FILE = 'target\\selenium-server.pid'
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building the application...'

                bat '''
                    echo ===== BUILD STARTED =====
                    call mvnw.cmd clean compile -DskipTests
                    if errorlevel 1 exit /b 1
                    echo ===== BUILD SUCCESS =====
                '''
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging Spring Boot application...'

                bat '''
                    echo ===== PACKAGE STARTED =====
                    call mvnw.cmd package -DskipTests
                    if errorlevel 1 exit /b 1
                    echo ===== PACKAGE SUCCESS =====
                '''
            }
        }

       stage('Start Test Server') {

    steps {

        echo 'Starting application for Selenium testing...'

        powershell '''
            Write-Host "=========================================="
            Write-Host "START TEST SERVER"
            Write-Host "=========================================="

            $java = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"
            $jar = "$env:WORKSPACE\\target\\accessportal-0.0.1-SNAPSHOT.jar"

            $outputLog = "$env:WORKSPACE\\target\\selenium-output.log"
            $errorLog = "$env:WORKSPACE\\target\\selenium-error.log"
            $pidFile = "$env:WORKSPACE\\target\\test-server.pid"

            $port = 8081

            Write-Host "Java: $java"
            Write-Host "JAR: $jar"
            Write-Host "Port: $port"

            # --------------------------------------------------
            # STOP ANY OLD PROCESS ON PORT 8081
            # --------------------------------------------------

            Write-Host "Checking port $port..."

            $connection = Get-NetTCPConnection `
                -LocalPort $port `
                -State Listen `
                -ErrorAction SilentlyContinue

            if ($connection) {

                $oldPid = $connection.OwningProcess

                Write-Host "Stopping existing application PID: $oldPid"

                Stop-Process `
                    -Id $oldPid `
                    -Force `
                    -ErrorAction SilentlyContinue

                Start-Sleep -Seconds 2
            }

            # --------------------------------------------------
            # CLEAN OLD LOGS
            # --------------------------------------------------

            Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
            Remove-Item $errorLog -Force -ErrorAction SilentlyContinue
            Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

            # --------------------------------------------------
            # START SPRING BOOT
            # --------------------------------------------------

            Write-Host "Starting test application..."

            $process = Start-Process `
                -FilePath $java `
                -ArgumentList "-jar `"$jar`"" `
                -WorkingDirectory "$env:WORKSPACE" `
                -RedirectStandardOutput $outputLog `
                -RedirectStandardError $errorLog `
                -WindowStyle Hidden `
                -PassThru

            $serverPid = $process.Id

            Write-Host "Spring Boot PID: $serverPid"

            # Save PID for cleanup
            Set-Content `
                -Path $pidFile `
                -Value $serverPid `
                -Encoding ASCII

            Write-Host "Test server PID saved: $serverPid"

            # --------------------------------------------------
            # WAIT FOR PORT
            # --------------------------------------------------

            Write-Host "Waiting for application startup..."

            $started = $false

            for ($i = 1; $i -le 30; $i++) {

                Start-Sleep -Seconds 1

                $running = Get-NetTCPConnection `
                    -LocalPort $port `
                    -State Listen `
                    -ErrorAction SilentlyContinue

                if ($running) {

                    $started = $true

                    Write-Host "Application detected on port $port after $i seconds."

                    break
                }

                # Check if Java process died
                if ($process.HasExited) {

                    Write-Host "Spring Boot process exited unexpectedly."

                    Write-Host "===== APPLICATION OUTPUT ====="

                    if (Test-Path $outputLog) {
                        Get-Content $outputLog -Tail 100
                    }

                    Write-Host "===== APPLICATION ERROR ====="

                    if (Test-Path $errorLog) {
                        Get-Content $errorLog -Tail 100
                    }

                    exit 1
                }
            }

            if (-not $started) {

                Write-Host "Application failed to start within 30 seconds."

                Write-Host "===== APPLICATION OUTPUT ====="

                if (Test-Path $outputLog) {
                    Get-Content $outputLog -Tail 100
                }

                Write-Host "===== APPLICATION ERROR ====="

                if (Test-Path $errorLog) {
                    Get-Content $errorLog -Tail 100
                }

                exit 1
            }

            # --------------------------------------------------
            # VERIFY LOGIN PAGE
            # --------------------------------------------------

            Write-Host "Checking http://localhost:8081/login ..."

            try {

                $response = Invoke-WebRequest `
                    -Uri "http://localhost:8081/login" `
                    -UseBasicParsing `
                    -TimeoutSec 10

                if ($response.StatusCode -eq 200) {

                    Write-Host "Login page returned HTTP 200."

                }
                else {

                    Write-Host "Unexpected HTTP status: $($response.StatusCode)"

                    exit 1
                }

            }
            catch {

                Write-Host "Login page check failed."
                Write-Host $_

                exit 1
            }

            # --------------------------------------------------
            # SERVER READY
            # --------------------------------------------------

            Write-Host "=========================================="
            Write-Host "TEST SERVER READY"
            Write-Host "PID: $serverPid"
            Write-Host "URL: http://localhost:8081/login"
            Write-Host "=========================================="

            # IMPORTANT:
            # Explicitly terminate this PowerShell script so Jenkins
            # can continue to the Selenium stage.
            exit 0
        '''
    }
}

        stage('Selenium Tests') {

            options {
                timeout(time: 4, unit: 'MINUTES')
            }

           stage('Selenium Tests') {

    options {
        timeout(time: 4, unit: 'MINUTES')
    }

    steps {

        echo '=========================================='
        echo 'STARTING SELENIUM TESTS'
        echo '=========================================='

        bat '''
            echo ===== SELENIUM ENVIRONMENT =====
            java -version
            mvnw.cmd -version

            echo ===== STARTING SELENIUM TESTS =====

            call mvnw.cmd -Dtest=PortalSeleniumTests test -DfailIfNoTests=true

            if errorlevel 1 (
                echo ===== SELENIUM TESTS FAILED =====
                exit /b 1
            )

            echo ===== SELENIUM TESTS PASSED =====
        '''

        echo '=========================================='
        echo 'SELENIUM TESTS PASSED'
        echo '=========================================='
    }
}
        stage('Stop Test Server') {

    steps {

        echo 'Stopping test application...'

        powershell '''
            $pidFile = "$env:WORKSPACE\\target\\test-server.pid"

            Write-Host "=========================================="
            Write-Host "STOP TEST SERVER"
            Write-Host "=========================================="

            if (Test-Path $pidFile) {

                $serverPid = Get-Content $pidFile

                Write-Host "Saved server PID: $serverPid"

                $process = Get-Process `
                    -Id $serverPid `
                    -ErrorAction SilentlyContinue

                if ($process) {

                    Write-Host "Stopping Java process..."

                    Stop-Process `
                        -Id $serverPid `
                        -Force `
                        -ErrorAction SilentlyContinue

                    Start-Sleep -Seconds 2

                    Write-Host "Test server stopped."
                }
                else {

                    Write-Host "Server process already stopped."
                }

                Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

            }
            else {

                Write-Host "No PID file found."

                $connection = Get-NetTCPConnection `
                    -LocalPort 8081 `
                    -State Listen `
                    -ErrorAction SilentlyContinue

                if ($connection) {

                    Write-Host "Stopping process using port 8081..."

                    Stop-Process `
                        -Id $connection.OwningProcess `
                        -Force `
                        -ErrorAction SilentlyContinue
                }
            }

            Write-Host "=========================================="
            Write-Host "TEST SERVER CLEANUP COMPLETE"
            Write-Host "=========================================="

            exit 0
        '''
    }
}
        }   
        stage('Deploy') {

            steps {

                echo "Deploying application to ${params.DEPLOY_ENV} environment..."

                bat '''
                    if not exist C:\\deploy mkdir C:\\deploy

                    copy /Y target\\accessportal-0.0.1-SNAPSHOT.jar C:\\deploy\\accessportal.jar

                    if errorlevel 1 exit /b 1
                '''

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $java = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"
                    $jar = "C:\\deploy\\accessportal.jar"

                    $outputLog = "C:\\deploy\\accessportal-output.log"
                    $errorLog = "C:\\deploy\\accessportal-error.log"

                    Write-Host "=========================================="
                    Write-Host "DEPLOYMENT"
                    Write-Host "=========================================="

                    # Stop existing application on port 8081

                    $connection = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connection) {

                        $existingPid = $connection[0].OwningProcess

                        Write-Host "Stopping existing application PID: $existingPid"

                        try {

                            Stop-Process `
                                -Id $existingPid `
                                -Force `
                                -ErrorAction Stop

                            Start-Sleep -Seconds 2

                        }
                        catch {

                            Write-Host "Could not stop existing application."
                            Write-Host $_.Exception.Message
                            exit 1
                        }
                    }

                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    Write-Host "Starting deployed application..."

                    $process = Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "C:\\deploy" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden `
                        -PassThru

                    Write-Host "Deployment PID: $($process.Id)"

                    Write-Host "Waiting for deployed application..."

                    $started = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 1

                        $running = Get-NetTCPConnection `
                            -LocalPort 8081 `
                            -State Listen `
                            -ErrorAction SilentlyContinue

                        if ($running) {

                            $started = $true

                            Write-Host "Application is running successfully on port 8081."

                            break
                        }
                    }

                    if (-not $started) {

                        Write-Host "ERROR: Deployment application failed to start."

                        Write-Host "===== APPLICATION OUTPUT ====="

                        if (Test-Path $outputLog) {
                            Get-Content $outputLog -Tail 100
                        }

                        Write-Host "===== APPLICATION ERROR ====="

                        if (Test-Path $errorLog) {
                            Get-Content $errorLog -Tail 100
                        }

                        exit 1
                    }

                    Write-Host "Application deployed successfully."
                '''

                echo "Environment: ${params.DEPLOY_ENV}"
                echo "Application URL: http://localhost:8081/login"
            }
        }
    }

    post {

        always {

            echo '=========================================='
            echo 'POST BUILD ACTIONS'
            echo '=========================================='

            // Always attempt to stop test server
            powershell '''
                $pidFile = "$env:WORKSPACE\\target\\selenium-server.pid"

                if (Test-Path $pidFile) {

                    $serverPid = Get-Content $pidFile -ErrorAction SilentlyContinue

                    if ($serverPid) {

                        $process = Get-Process `
                            -Id $serverPid `
                            -ErrorAction SilentlyContinue

                        if ($process) {

                            Write-Host "Cleaning up test server PID: $serverPid"

                            try {

                                Stop-Process `
                                    -Id $serverPid `
                                    -Force `
                                    -ErrorAction Stop

                                Write-Host "Test server cleanup completed."

                            }
                            catch {

                                Write-Host "Cleanup warning: $($_.Exception.Message)"
                            }
                        }
                    }

                    Remove-Item `
                        $pidFile `
                        -Force `
                        -ErrorAction SilentlyContinue
                }
            '''

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

            echo 'Post-build actions completed.'
        }

        success {

            echo '=========================================='
            echo 'WEEK 10 PIPELINE SUCCESS'
            echo 'Selenium tests passed.'
            echo 'Application deployed successfully.'
            echo '=========================================='
        }

        failure {

            echo '=========================================='
            echo 'WEEK 10 PIPELINE FAILED'
            echo 'Check the stage where the failure occurred.'
            echo '=========================================='
        }

        aborted {

            echo '=========================================='
            echo 'WEEK 10 PIPELINE ABORTED'
            echo '=========================================='
        }
    }
}