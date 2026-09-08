pipeline {

    agent any

    environment {
        APP_PORT = '8081'
        APP_URL = 'http://localhost:8081'
        JAR_NAME = 'accessportal-0.0.1-SNAPSHOT.jar'
        PID_FILE = 'test-server.pid'
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

                echo 'CHECKOUT SUCCESS'
            }
        }


        // =========================================================
        // BUILD
        // =========================================================
        stage('Build Application') {
            steps {

                echo '=========================================='
                echo 'BUILD START'
                echo '=========================================='

                bat '''
                    call mvnw.cmd clean package -DskipTests
                '''

                echo '=========================================='
                echo 'BUILD SUCCESS'
                echo '=========================================='
            }
        }


        // =========================================================
        // START SPRING BOOT
        // =========================================================
        stage('Start Test Server') {
            steps {

                echo '=========================================='
                echo 'STARTING TEST SERVER'
                echo '=========================================='

                powershell '''

                    $ErrorActionPreference = "Stop"

                    $workspace = $env:WORKSPACE
                    $jar = Join-Path $workspace "target\\accessportal-0.0.1-SNAPSHOT.jar"

                    $stdout = Join-Path $workspace "springboot-output.log"
                    $stderr = Join-Path $workspace "springboot-error.log"
                    $pidFile = Join-Path $workspace "test-server.pid"

                    Write-Host "JAR: $jar"
                    Write-Host "PORT: $env:APP_PORT"

                    # -------------------------------------------------
                    # Check JAR
                    # -------------------------------------------------

                    if (!(Test-Path $jar)) {

                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    # -------------------------------------------------
                    # Make sure old application is not running
                    # -------------------------------------------------

                    Write-Host "Checking port $env:APP_PORT..."

                    $existingConnection = Get-NetTCPConnection `
                        -LocalPort $env:APP_PORT `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existingConnection) {

                        Write-Host "Port $env:APP_PORT is already in use."

                        foreach ($connection in $existingConnection) {

                            Write-Host "Stopping existing PID: $($connection.OwningProcess)"

                            Stop-Process `
                                -Id $connection.OwningProcess `
                                -Force `
                                -ErrorAction SilentlyContinue
                        }

                        Start-Sleep -Seconds 2
                    }

                    # -------------------------------------------------
                    # Delete old log files
                    # -------------------------------------------------

                    Remove-Item $stdout -Force -ErrorAction SilentlyContinue
                    Remove-Item $stderr -Force -ErrorAction SilentlyContinue
                    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

                    # -------------------------------------------------
                    # Start Spring Boot
                    #
                    # IMPORTANT:
                    # Redirect stdout/stderr so java does not keep
                    # Jenkins PowerShell output handles open.
                    # -------------------------------------------------

                    Write-Host "Starting Spring Boot application..."

                    $process = Start-Process `
                        -FilePath "java.exe" `
                        -ArgumentList @(
                            "-jar",
                            "`"$jar`""
                        ) `
                        -RedirectStandardOutput $stdout `
                        -RedirectStandardError $stderr `
                        -WindowStyle Hidden `
                        -PassThru

                    $processId = $process.Id

                    Write-Host "Spring Boot PID: $processId"

                    # Save PID for later cleanup
                    Set-Content `
                        -Path $pidFile `
                        -Value $processId

                    Write-Host "Test server PID saved."

                    # -------------------------------------------------
                    # Wait for application
                    # -------------------------------------------------

                    Write-Host "Waiting for application startup..."

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        # Check whether Java process is still alive
                        $runningProcess = Get-Process `
                            -Id $processId `
                            -ErrorAction SilentlyContinue

                        if (!$runningProcess) {

                            Write-Host "ERROR: Spring Boot process stopped."

                            Write-Host "===== SPRING BOOT ERROR LOG ====="

                            if (Test-Path $stderr) {
                                Get-Content $stderr -Tail 50
                            }

                            Write-Host "===== SPRING BOOT OUTPUT LOG ====="

                            if (Test-Path $stdout) {
                                Get-Content $stdout -Tail 50
                            }

                            exit 1
                        }

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "$env:APP_URL/login" `
                                -UseBasicParsing `
                                -TimeoutSec 3 `
                                -ErrorAction Stop

                            if ($response.StatusCode -eq 200) {

                                Write-Host "Application detected on port $env:APP_PORT."
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

                        Write-Host "=========================================="
                        Write-Host "ERROR: APPLICATION DID NOT START"
                        Write-Host "=========================================="

                        Write-Host "===== SPRING BOOT ERROR LOG ====="

                        if (Test-Path $stderr) {
                            Get-Content $stderr -Tail 100
                        }

                        Write-Host "===== SPRING BOOT OUTPUT LOG ====="

                        if (Test-Path $stdout) {
                            Get-Content $stdout -Tail 100
                        }

                        Stop-Process `
                            -Id $processId `
                            -Force `
                            -ErrorAction SilentlyContinue

                        exit 1
                    }

                    # -------------------------------------------------
                    # SUCCESS
                    # -------------------------------------------------

                    Write-Host "=========================================="
                    Write-Host "TEST SERVER READY"
                    Write-Host "PID: $processId"
                    Write-Host "URL: $env:APP_URL/login"
                    Write-Host "=========================================="

                    # IMPORTANT:
                    # Do NOT Stop-Process here.
                    #
                    # Selenium needs the application to remain running.
                    #
                    # Also, because stdout/stderr are redirected,
                    # Jenkins should be able to finish this PowerShell
                    # step without waiting for Java's console handles.

                '''
            }
        }


        // =========================================================
        // SELENIUM TESTS
        // =========================================================
        stage('Run Selenium Tests') {
            steps {

                echo '=========================================='
                echo 'SELENIUM TESTS START'
                echo '=========================================='

                bat '''
                    echo ===== SELENIUM START =====

                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    echo ===== SELENIUM FINISHED =====
                '''

                echo '=========================================='
                echo 'SELENIUM TESTS SUCCESS'
                echo '=========================================='
            }
        }


        // =========================================================
        // STOP SERVER
        // =========================================================
        stage('Stop Test Server') {
            steps {

                echo '=========================================='
                echo 'STOPPING TEST SERVER'
                echo '=========================================='

                powershell '''

                    $pidFile = Join-Path $env:WORKSPACE "test-server.pid"

                    if (Test-Path $pidFile) {

                        $processId = Get-Content $pidFile

                        Write-Host "Server PID: $processId"

                        $process = Get-Process `
                            -Id $processId `
                            -ErrorAction SilentlyContinue

                        if ($process) {

                            Write-Host "Stopping Spring Boot..."

                            Stop-Process `
                                -Id $processId `
                                -Force `
                                -ErrorAction SilentlyContinue

                            Write-Host "Spring Boot stopped."

                        } else {

                            Write-Host "Spring Boot process already stopped."
                        }

                        Remove-Item `
                            $pidFile `
                            -Force `
                            -ErrorAction SilentlyContinue

                    } else {

                        Write-Host "PID file not found."
                    }

                '''

                echo '=========================================='
                echo 'TEST SERVER STOPPED'
                echo '=========================================='
            }
        }
    }


    // =============================================================
    // ALWAYS CLEANUP
    // =============================================================
    post {

        always {

            echo '=========================================='
            echo 'PIPELINE CLEANUP'
            echo '=========================================='

            powershell '''

                $pidFile = Join-Path $env:WORKSPACE "test-server.pid"

                if (Test-Path $pidFile) {

                    $processId = Get-Content $pidFile

                    Write-Host "Cleanup: checking PID $processId"

                    Stop-Process `
                        -Id $processId `
                        -Force `
                        -ErrorAction SilentlyContinue

                    Remove-Item `
                        $pidFile `
                        -Force `
                        -ErrorAction SilentlyContinue

                    Write-Host "Cleanup completed."

                } else {

                    Write-Host "No server PID file found."
                }

            '''

            echo '=========================================='
            echo 'PIPELINE FINISHED'
            echo '=========================================='
        }
    }
}