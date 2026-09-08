pipeline {

    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_PORT = '8081'
        APP_JAR = 'target\\accessportal-0.0.1-SNAPSHOT.jar'
        SERVER_PID_FILE = 'server.pid'
        SERVER_LOG = 'spring-boot.log'
    }

    stages {

        // =========================================================
        // CHECKOUT
        // =========================================================
        stage('Checkout') {
            steps {
                echo '=========================================='
                echo 'CHECKOUT'
                echo '=========================================='

                checkout scm

                echo 'Checkout completed.'
            }
        }


        // =========================================================
        // BUILD
        // =========================================================
        stage('Build Application') {
            steps {
                echo '=========================================='
                echo 'BUILD APPLICATION'
                echo '=========================================='

                bat '''
                    call mvnw.cmd clean package -DskipTests
                    if errorlevel 1 exit /b 1
                '''

                echo '===== BUILD SUCCESS ====='
            }
        }


        // =========================================================
        // START SPRING BOOT SERVER
        // =========================================================
        stage('Start Test Server') {

            options {
                timeout(time: 2, unit: 'MINUTES')
            }

            steps {

                echo '=========================================='
                echo 'STARTING TEST SERVER'
                echo '=========================================='

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $workspace = $env:WORKSPACE
                    $jar = Join-Path $workspace $env:APP_JAR
                    $pidFile = Join-Path $workspace $env:SERVER_PID_FILE
                    $logFile = Join-Path $workspace $env:SERVER_LOG
                    $port = $env:APP_PORT

                    Write-Host "Workspace: $workspace"
                    Write-Host "JAR: $jar"
                    Write-Host "Port: $port"

                    # -------------------------------------------------
                    # Remove old PID file
                    # -------------------------------------------------
                    if (Test-Path $pidFile) {
                        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
                    }

                    # -------------------------------------------------
                    # Make sure port 8081 is free
                    # -------------------------------------------------
                    Write-Host "Checking port $port..."

                    $existingConnections = Get-NetTCPConnection `
                        -LocalPort ([int]$port) `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existingConnections) {

                        foreach ($connection in $existingConnections) {

                            $oldPid = $connection.OwningProcess

                            Write-Host "Port $port is already in use by PID $oldPid"
                            Write-Host "Stopping old process..."

                            taskkill /F /T /PID $oldPid 2>$null

                            Start-Sleep -Seconds 2
                        }
                    }

                    # -------------------------------------------------
                    # Verify JAR exists
                    # -------------------------------------------------
                    if (!(Test-Path $jar)) {
                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    Write-Host "JAR FOUND"

                    # -------------------------------------------------
                    # Remove old logs
                    # -------------------------------------------------
                    Remove-Item $logFile -Force -ErrorAction SilentlyContinue

                    $errorLog = Join-Path $workspace "spring-boot-error.log"

                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    # -------------------------------------------------
                    # Start Spring Boot
                    # -------------------------------------------------
                    Write-Host "Starting Spring Boot application..."

                    $javaPath = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"

                    if (!(Test-Path $javaPath)) {
                        $javaPath = "java"
                    }

                    Write-Host "Java: $javaPath"

                    $process = Start-Process `
                        -FilePath $javaPath `
                        -ArgumentList "-jar `"$jar`" --server.port=$port" `
                        -WorkingDirectory $workspace `
                        -RedirectStandardOutput $logFile `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden `
                        -PassThru

                    $serverPid = $process.Id

                    Write-Host "Spring Boot PID: $serverPid"

                    # -------------------------------------------------
                    # Save PID
                    # -------------------------------------------------
                    Set-Content `
                        -Path $pidFile `
                        -Value $serverPid `
                        -Encoding ASCII

                    Write-Host "Test server PID saved: $serverPid"

                    # -------------------------------------------------
                    # Wait for application
                    # -------------------------------------------------
                    Write-Host "Waiting for application startup..."

                    $ready = $false

                    for ($i = 1; $i -le 60; $i++) {

                        Start-Sleep -Seconds 2

                        # Check if Java process has died
                        if ($process.HasExited) {

                            Write-Host "ERROR: Spring Boot process exited."

                            Write-Host "===== SPRING BOOT OUTPUT ====="

                            if (Test-Path $logFile) {
                                Get-Content $logFile -Tail 80
                            }

                            Write-Host "===== SPRING BOOT ERROR ====="

                            if (Test-Path $errorLog) {
                                Get-Content $errorLog -Tail 80
                            }

                            exit 1
                        }

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "http://localhost:$port/login" `
                                -UseBasicParsing `
                                -TimeoutSec 3

                            if ($response.StatusCode -eq 200) {

                                Write-Host "Application detected on port $port."
                                Write-Host "Login page returned HTTP 200."

                                $ready = $true
                                break
                            }

                        }
                        catch {

                            Write-Host "Waiting for application... attempt $i"
                        }
                    }

                    # -------------------------------------------------
                    # Application failed to start
                    # -------------------------------------------------
                    if (!$ready) {

                        Write-Host "ERROR: APPLICATION DID NOT START"

                        Write-Host "===== SPRING BOOT OUTPUT ====="

                        if (Test-Path $logFile) {
                            Get-Content $logFile -Tail 100
                        }

                        Write-Host "===== SPRING BOOT ERROR ====="

                        if (Test-Path $errorLog) {
                            Get-Content $errorLog -Tail 100
                        }

                        taskkill /F /T /PID $serverPid 2>$null

                        exit 1
                    }

                    # -------------------------------------------------
                    # IMPORTANT
                    # -------------------------------------------------
                    Write-Host "=========================================="
                    Write-Host "TEST SERVER READY"
                    Write-Host "PID: $serverPid"
                    Write-Host "URL: http://localhost:$port/login"
                    Write-Host "=========================================="

                    # Explicitly return control to Jenkins
                    exit 0
                '''
            }
        }


        // =========================================================
        // SELENIUM TESTS
        // =========================================================
        stage('Selenium Tests') {

            options {
                timeout(time: 5, unit: 'MINUTES')
            }

            steps {

                echo '=========================================='
                echo 'SELENIUM TESTS STARTED'
                echo '=========================================='

                echo 'Checking test server before Selenium...'

                powershell '''
                    $response = Invoke-WebRequest `
                        -Uri "http://localhost:8081/login" `
                        -UseBasicParsing `
                        -TimeoutSec 10

                    if ($response.StatusCode -ne 200) {
                        Write-Host "ERROR: Test server is not available."
                        exit 1
                    }

                    Write-Host "Test server is available."
                    Write-Host "HTTP Status: $($response.StatusCode)"
                '''

                echo 'Running Selenium tests...'

                bat '''
                    echo ===== MAVEN SELENIUM TEST START =====

                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    if errorlevel 1 (
                        echo ===== SELENIUM TESTS FAILED =====
                        exit /b 1
                    )

                    echo ===== SELENIUM TESTS PASSED =====
                '''

                echo '=========================================='
                echo 'SELENIUM TESTS FINISHED'
                echo '=========================================='
            }
        }
    }


    // =============================================================
    // ALWAYS CLEANUP SERVER
    // =============================================================
    post {

        always {

            echo '=========================================='
            echo 'CLEANUP TEST SERVER'
            echo '=========================================='

            powershell '''
                $workspace = $env:WORKSPACE
                $pidFile = Join-Path $workspace $env:SERVER_PID_FILE

                if (Test-Path $pidFile) {

                    $serverPid = Get-Content $pidFile

                    Write-Host "Stopping test server PID: $serverPid"

                    taskkill /F /T /PID $serverPid 2>$null

                    Start-Sleep -Seconds 2

                    Write-Host "Test server cleanup completed."

                    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

                } else {

                    Write-Host "No server PID file found."
                    Write-Host "Nothing to clean up."
                }
            '''

            echo '=========================================='
            echo 'PIPELINE FINISHED'
            echo '=========================================='
        }
    }
}