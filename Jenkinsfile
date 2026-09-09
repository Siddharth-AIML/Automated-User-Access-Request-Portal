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

        stage('Start Test Server') {
            steps {

                echo '=========================================='
                echo 'START TEST SERVER'
                echo '=========================================='

                powershell '''
                    $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"

                    Write-Host "JAR: $jar"

                    if (!(Test-Path $jar)) {
                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    # Make sure port 8081 is free
                    $existing = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existing) {
                        Write-Host "ERROR: Port 8081 is already in use"
                        exit 1
                    }

                    Write-Host "Starting Spring Boot application..."

                    $process = Start-Process `
                        -FilePath "java" `
                        -ArgumentList "-jar `"$jar`"" `
                        -PassThru `
                        -WindowStyle Hidden

                    Write-Host "Spring Boot PID: $($process.Id)"

                    # Save PID for the next stage
                    Set-Content `
                        -Path (Join-Path $env:WORKSPACE "test-server.pid") `
                        -Value $process.Id

                    Write-Host "PID saved."

                    Write-Host "Waiting for application startup..."

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        # Check whether process is still alive
                        $running = Get-Process `
                            -Id $process.Id `
                            -ErrorAction SilentlyContinue

                        if (!$running) {
                            Write-Host "ERROR: Spring Boot process stopped unexpectedly."
                            exit 1
                        }

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "http://localhost:8081/login" `
                                -UseBasicParsing `
                                -TimeoutSec 3

                            if ($response.StatusCode -eq 200) {

                                Write-Host "=========================================="
                                Write-Host "APPLICATION READY"
                                Write-Host "PID: $($process.Id)"
                                Write-Host "HTTP STATUS: $($response.StatusCode)"
                                Write-Host "URL: http://localhost:8081/login"
                                Write-Host "=========================================="

                                $ready = $true
                                break
                            }

                        }
                        catch {

                            Write-Host "Waiting for application... attempt $i"
                        }
                    }

                    if (!$ready) {

                        Write-Host "ERROR: APPLICATION DID NOT START"

                        Stop-Process `
                            -Id $process.Id `
                            -Force `
                            -ErrorAction SilentlyContinue

                        exit 1
                    }

                    Write-Host "=========================================="
                    Write-Host "START TEST PASSED"
                    Write-Host "SERVER IS STILL RUNNING"
                    Write-Host "PID: $($process.Id)"
                    Write-Host "=========================================="
                '''
            }
        }

        stage('Verify Server Still Running') {
            steps {

                echo '=========================================='
                echo 'VERIFYING TEST SERVER'
                echo '=========================================='

                powershell '''
                    $pidFile = Join-Path $env:WORKSPACE "test-server.pid"

                    if (!(Test-Path $pidFile)) {
                        Write-Host "ERROR: PID FILE NOT FOUND"
                        exit 1
                    }

                    $serverPid = [int](Get-Content $pidFile)

                    Write-Host "Saved PID: $serverPid"

                    $process = Get-Process `
                        -Id $serverPid `
                        -ErrorAction SilentlyContinue

                    if (!$process) {
                        Write-Host "ERROR: TEST SERVER IS NOT RUNNING"
                        exit 1
                    }

                    Write-Host "Server process is alive."

                    $connection = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if (!$connection) {
                        Write-Host "ERROR: Nothing is listening on port 8081"
                        exit 1
                    }

                    Write-Host "Port 8081 is listening."

                    $response = Invoke-WebRequest `
                        -Uri "http://localhost:8081/login" `
                        -UseBasicParsing `
                        -TimeoutSec 5

                    if ($response.StatusCode -ne 200) {
                        Write-Host "ERROR: Login page is not responding correctly"
                        exit 1
                    }

                    Write-Host "Login page returned HTTP $($response.StatusCode)"

                    Write-Host "=========================================="
                    Write-Host "SERVER VERIFICATION PASSED"
                    Write-Host "=========================================="
                '''
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