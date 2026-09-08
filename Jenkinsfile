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

                    # ------------------------------------------------
                    # Stop previously tracked test server
                    # ------------------------------------------------

                    if (Test-Path $pidFile) {

                        $oldPid = Get-Content $pidFile -ErrorAction SilentlyContinue

                        if ($oldPid) {

                            Write-Host "Found previous test server PID: $oldPid"

                            $oldProcess = Get-Process -Id $oldPid -ErrorAction SilentlyContinue

                            if ($oldProcess) {

                                Write-Host "Stopping previous test server..."

                                try {
                                    Stop-Process -Id $oldPid -Force -ErrorAction Stop
                                    Write-Host "Previous test server stopped."
                                }
                                catch {
                                    Write-Host "Could not stop previous process: $($_.Exception.Message)"
                                }

                                Start-Sleep -Seconds 2
                            }
                        }

                        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
                    }

                    # ------------------------------------------------
                    # Check port 8081
                    # ------------------------------------------------

                    Write-Host "Checking port 8081..."

                    $connection = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connection) {

                        Write-Host "Port 8081 is already occupied."

                        $ownerPid = $connection[0].OwningProcess

                        Write-Host "Existing process PID: $ownerPid"

                        try {

                            Stop-Process `
                                -Id $ownerPid `
                                -Force `
                                -ErrorAction Stop

                            Write-Host "Existing process stopped."

                        }
                        catch {

                            Write-Host "Unable to stop existing process."
                            Write-Host "Error: $($_.Exception.Message)"
                            exit 1
                        }

                        Start-Sleep -Seconds 2
                    }

                    # ------------------------------------------------
                    # Clean old logs
                    # ------------------------------------------------

                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    # ------------------------------------------------
                    # Verify JAR
                    # ------------------------------------------------

                    if (-not (Test-Path $jar)) {

                        Write-Host "ERROR: Application JAR not found."
                        Write-Host $jar

                        exit 1
                    }

                    # ------------------------------------------------
                    # Start Spring Boot
                    # ------------------------------------------------

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

                    # Save PID
                    Set-Content `
                        -Path $pidFile `
                        -Value $serverPid

                    Write-Host "Waiting for application startup..."

                    # ------------------------------------------------
                    # Wait for port
                    # ------------------------------------------------

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

                        # Check whether Java process died
                        $currentProcess = Get-Process `
                            -Id $serverPid `
                            -ErrorAction SilentlyContinue

                        if (-not $currentProcess) {

                            Write-Host "ERROR: Spring Boot process terminated unexpectedly."

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

                    Write-Host "Test application started successfully."
                    Write-Host "Test server PID: $serverPid"

                    # ------------------------------------------------
                    # Verify HTTP endpoint
                    # ------------------------------------------------

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

                            Write-Host "HTTP check attempt $i failed. Retrying..."
                        }

                        Start-Sleep -Seconds 1
                    }

                    if (-not $httpOk) {

                        Write-Host "ERROR: Login page is not responding."

                        exit 1
                    }

                    Write-Host "=========================================="
                    Write-Host "TEST SERVER READY"
                    Write-Host "PID: $serverPid"
                    Write-Host "URL: http://localhost:8081/login"
                    Write-Host "=========================================="
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

                            $process = Get-Process `
                                -Id $serverPid `
                                -ErrorAction SilentlyContinue

                            if ($process) {

                                Write-Host "Stopping test server..."

                                try {

                                    Stop-Process `
                                        -Id $serverPid `
                                        -Force `
                                        -ErrorAction Stop

                                    Write-Host "Test server stopped."

                                }
                                catch {

                                    Write-Host "Could not stop test server."
                                    Write-Host $_.Exception.Message
                                }
                            }
                            else {

                                Write-Host "Test server process is already stopped."
                            }
                        }

                        Remove-Item `
                            $pidFile `
                            -Force `
                            -ErrorAction SilentlyContinue
                    }
                    else {

                        Write-Host "No PID file found."
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