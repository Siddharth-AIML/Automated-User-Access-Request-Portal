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
            $ErrorActionPreference = "Stop"

            $java = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"
            $jar = "$env:WORKSPACE\\target\\accessportal-0.0.1-SNAPSHOT.jar"

            $outputLog = "$env:WORKSPACE\\target\\selenium-output.log"
            $errorLog = "$env:WORKSPACE\\target\\selenium-error.log"
            $pidFile = "$env:WORKSPACE\\target\\selenium-server.pid"

            Write-Host "=========================================="
            Write-Host "START TEST SERVER"
            Write-Host "=========================================="

            Write-Host "Java: $java"
            Write-Host "JAR:  $jar"
            Write-Host "Port: 8081"

            # ---------------------------------------------
            # Clean old PID file
            # ---------------------------------------------

            if (Test-Path $pidFile) {

                $oldPid = Get-Content $pidFile -ErrorAction SilentlyContinue

                if ($oldPid) {

                    Write-Host "Old test server PID found: $oldPid"

                    $oldProcess = Get-Process `
                        -Id $oldPid `
                        -ErrorAction SilentlyContinue

                    if ($oldProcess) {

                        Write-Host "Stopping old test server..."

                        taskkill /F /PID $oldPid 2>$null

                        Start-Sleep -Seconds 2
                    }
                }

                Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
            }

            # ---------------------------------------------
            # Check port
            # ---------------------------------------------

            Write-Host "Checking port 8081..."

            $connection = Get-NetTCPConnection `
                -LocalPort 8081 `
                -State Listen `
                -ErrorAction SilentlyContinue

            if ($connection) {

                $existingPid = $connection[0].OwningProcess

                Write-Host "Stopping existing process on port 8081..."
                Write-Host "Existing PID: $existingPid"

                taskkill /F /PID $existingPid 2>$null

                Start-Sleep -Seconds 2
            }

            # ---------------------------------------------
            # Remove old logs
            # ---------------------------------------------

            Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
            Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

            # ---------------------------------------------
            # Verify JAR
            # ---------------------------------------------

            if (-not (Test-Path $jar)) {

                Write-Host "ERROR: JAR does not exist:"
                Write-Host $jar

                exit 1
            }

            # ---------------------------------------------
            # Start Java using CMD START
            # ---------------------------------------------
            #
            # IMPORTANT:
            # Using START makes the Java process completely
            # independent from the Jenkins PowerShell process.
            #

            Write-Host "Starting test application..."

            $startCommand = @"
start "" /B "$java" -jar "$jar" 1>>"$outputLog" 2>>"$errorLog"
"@

            cmd.exe /c $startCommand

            Write-Host "Java launch command completed."

            # ---------------------------------------------
            # Wait for port 8081
            # ---------------------------------------------

            Write-Host "Waiting for application startup..."

            $started = $false
            $serverPid = $null

            for ($i = 1; $i -le 30; $i++) {

                Start-Sleep -Seconds 1

                $running = Get-NetTCPConnection `
                    -LocalPort 8081 `
                    -State Listen `
                    -ErrorAction SilentlyContinue

                if ($running) {

                    $serverPid = $running[0].OwningProcess
                    $started = $true

                    Write-Host "Application detected on port 8081 after $i seconds."
                    Write-Host "Test server PID: $serverPid"

                    break
                }
            }

            if (-not $started) {

                Write-Host "ERROR: Application failed to start."

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

            # ---------------------------------------------
            # Save actual Java PID
            # ---------------------------------------------

            Set-Content `
                -Path $pidFile `
                -Value $serverPid

            Write-Host "Test server PID saved: $serverPid"

            # ---------------------------------------------
            # Check login page
            # ---------------------------------------------

            Write-Host "Checking http://localhost:8081/login ..."

            $httpOk = $false

            for ($i = 1; $i -le 15; $i++) {

                try {

                    $response = Invoke-WebRequest `
                        -Uri "http://localhost:8081/login" `
                        -UseBasicParsing `
                        -TimeoutSec 5

                    if ($response.StatusCode -eq 200) {

                        $httpOk = $true

                        Write-Host "Login page returned HTTP 200."

                        break
                    }
                }
                catch {

                    Write-Host "HTTP check attempt $i failed."
                }

                Start-Sleep -Seconds 1
            }

            if (-not $httpOk) {

                Write-Host "ERROR: Login page did not return HTTP 200."

                exit 1
            }

            Write-Host "=========================================="
            Write-Host "TEST SERVER READY"
            Write-Host "PID: $serverPid"
            Write-Host "URL: http://localhost:8081/login"
            Write-Host "=========================================="

            # ---------------------------------------------
            # Explicitly terminate PowerShell step
            # ---------------------------------------------

            exit 0
        '''
    }
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
                    call mvnw.cmd -Dtest=PortalSeleniumTests test
                    if errorlevel 1 exit /b 1
                '''

                echo '=========================================='
                echo 'SELENIUM TESTS PASSED'
                echo '=========================================='
            }
        }

        stage('Stop Test Server') {
    steps {

        echo 'Stopping Selenium test server...'

        powershell '''
            $pidFile = "$env:WORKSPACE\\target\\selenium-server.pid"

            Write-Host "=========================================="
            Write-Host "STOP TEST SERVER"
            Write-Host "=========================================="

            if (Test-Path $pidFile) {

                $serverPid = Get-Content $pidFile -ErrorAction SilentlyContinue

                if ($serverPid) {

                    Write-Host "Test server PID: $serverPid"

                    taskkill /F /PID $serverPid 2>$null

                    Write-Host "Test server stopped."
                }

                Remove-Item `
                    $pidFile `
                    -Force `
                    -ErrorAction SilentlyContinue
            }
            else {

                Write-Host "No test server PID file found."
            }

            Start-Sleep -Seconds 2

            $remaining = Get-NetTCPConnection `
                -LocalPort 8081 `
                -State Listen `
                -ErrorAction SilentlyContinue

            if ($remaining) {

                Write-Host "WARNING: Port 8081 is still occupied."

            }
            else {

                Write-Host "Port 8081 is free."
            }

            exit 0
        '''
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