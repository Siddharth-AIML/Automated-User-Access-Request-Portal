pipeline {

    agent any

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build Application') {
            steps {
                bat '''
                    call mvnw.cmd clean package -DskipTests
                '''
            }
        }

        stage('Start Test Server - OLD LOGIC') {
            steps {

                echo '=========================================='
                echo 'START TEST SERVER - OLD LOGIC'
                echo '=========================================='

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"
                    $port = 8081

                    Write-Host "JAR: $jar"
                    Write-Host "Port: $port"

                    # ==========================================
                    # CHECK JAR
                    # ==========================================

                    if (!(Test-Path $jar)) {
                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    Write-Host "JAR FOUND"

                    # ==========================================
                    # CHECK PORT
                    # ==========================================

                    Write-Host "Checking port $port..."

                    $existingConnections = Get-NetTCPConnection `
                        -LocalPort $port `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existingConnections) {

                        Write-Host "Port $port is already in use."

                        foreach ($connection in $existingConnections) {

                            $existingPid = $connection.OwningProcess

                            Write-Host "Existing process PID: $existingPid"

                            Stop-Process `
                                -Id $existingPid `
                                -Force `
                                -ErrorAction SilentlyContinue
                        }

                        Start-Sleep -Seconds 2
                    }

                    # ==========================================
                    # LOG FILES
                    # ==========================================

                    $serverLog = Join-Path `
                        $env:WORKSPACE `
                        "server.log"

                    $serverErrorLog = Join-Path `
                        $env:WORKSPACE `
                        "server-error.log"

                    $pidFile = Join-Path `
                        $env:WORKSPACE `
                        "server.pid"

                    Write-Host "Server log: $serverLog"
                    Write-Host "Server error log: $serverErrorLog"
                    Write-Host "PID file: $pidFile"

                    # Remove old files

                    Remove-Item `
                        $serverLog `
                        -Force `
                        -ErrorAction SilentlyContinue

                    Remove-Item `
                        $serverErrorLog `
                        -Force `
                        -ErrorAction SilentlyContinue

                    Remove-Item `
                        $pidFile `
                        -Force `
                        -ErrorAction SilentlyContinue

                    Write-Host "Old log/PID files removed."

                    # ==========================================
                    # START SPRING BOOT
                    # ==========================================

                    Write-Host "Starting Spring Boot application..."

                    $process = Start-Process `
                        -FilePath "java" `
                        -ArgumentList "-jar `"$jar`"" `
                        -RedirectStandardOutput $serverLog `
                        -RedirectStandardError $serverErrorLog `
                        -WindowStyle Hidden `
                        -PassThru

                    Write-Host "Spring Boot PID: $($process.Id)"

                    # ==========================================
                    # SAVE PID
                    # ==========================================

                    Set-Content `
                        -Path $pidFile `
                        -Value $process.Id

                    Write-Host "Test server PID saved."

                    Write-Host "Waiting for application startup..."

                    # ==========================================
                    # WAIT FOR APPLICATION
                    # ==========================================

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        Write-Host "Startup check attempt $i"

                        # Check Java process

                        if ($process.HasExited) {

                            Write-Host "ERROR: Spring Boot process exited."

                            if (Test-Path $serverLog) {
                                Write-Host "===== SERVER LOG ====="
                                Get-Content $serverLog -Tail 100
                            }

                            if (Test-Path $serverErrorLog) {
                                Write-Host "===== SERVER ERROR LOG ====="
                                Get-Content $serverErrorLog -Tail 100
                            }

                            exit 1
                        }

                        # Check HTTP

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "http://localhost:8081/login" `
                                -UseBasicParsing `
                                -TimeoutSec 3

                            if ($response.StatusCode -eq 200) {

                                Write-Host "Application detected on port 8081."
                                Write-Host "Login page returned HTTP 200."

                                $ready = $true

                                break
                            }

                        }
                        catch {

                            Write-Host "Application not ready yet."
                        }
                    }

                    # ==========================================
                    # START FAILURE
                    # ==========================================

                    if (!$ready) {

                        Write-Host "ERROR: APPLICATION DID NOT START"

                        if (Test-Path $serverLog) {
                            Write-Host "===== SERVER LOG ====="
                            Get-Content $serverLog -Tail 100
                        }

                        if (Test-Path $serverErrorLog) {
                            Write-Host "===== SERVER ERROR LOG ====="
                            Get-Content $serverErrorLog -Tail 100
                        }

                        if ($process -and !$process.HasExited) {

                            Stop-Process `
                                -Id $process.Id `
                                -Force `
                                -ErrorAction SilentlyContinue
                        }

                        exit 1
                    }

                    # ==========================================
                    # SERVER READY
                    # ==========================================

                    Write-Host ""
                    Write-Host "=========================================="
                    Write-Host "TEST SERVER READY"
                    Write-Host "PID: $($process.Id)"
                    Write-Host "URL: http://localhost:8081/login"
                    Write-Host "=========================================="
                    Write-Host ""

                    Write-Host "PowerShell Start Server step is about to finish."

                '''

                echo '=========================================='
                echo 'START SERVER STAGE FINISHED'
                echo '=========================================='
            }
        }
    }

    post {
        always {

            echo '=========================================='
            echo 'PIPELINE FINISHED'
            echo '=========================================='
        }
    }
}