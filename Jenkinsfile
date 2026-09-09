pipeline {

    agent any

    stages {

        stage('Start Test Server') {

            steps {

                echo '=========================================='
                echo 'START TEST SERVER'
                echo '=========================================='

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"
                    $pidFile = Join-Path $env:WORKSPACE "test-server.pid"

                    Write-Host "JAR: $jar"

                    if (!(Test-Path $jar)) {
                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    Write-Host "JAR FOUND"

                    # Check whether port 8081 is already occupied
                    $existing = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existing) {
                        Write-Host "ERROR: Port 8081 is already in use."
                        exit 1
                    }

                    Write-Host "Starting Spring Boot application..."

                    $process = Start-Process `
                        -FilePath "java.exe" `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory $env:WORKSPACE `
                        -PassThru `
                        -WindowStyle Hidden

                    $serverPid = $process.Id

                    Write-Host "Spring Boot PID: $serverPid"

                    Set-Content `
                        -Path $pidFile `
                        -Value $serverPid

                    Write-Host "PID saved: $serverPid"
                    Write-Host "Waiting for application startup..."

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "http://localhost:8081/login" `
                                -UseBasicParsing `
                                -TimeoutSec 3

                            if ($response.StatusCode -eq 200) {

                                Write-Host "=========================================="
                                Write-Host "APPLICATION READY"
                                Write-Host "PID: $serverPid"
                                Write-Host "HTTP STATUS: $($response.StatusCode)"
                                Write-Host "URL: http://localhost:8081/login"
                                Write-Host "=========================================="

                                $ready = $true
                                break
                            }

                        }
                        catch {

                            Write-Host "Waiting... attempt $i"
                        }
                    }

                    if (!$ready) {

                        Write-Host "ERROR: APPLICATION DID NOT START"

                        if (Get-Process -Id $serverPid -ErrorAction SilentlyContinue) {
                            Stop-Process -Id $serverPid -Force
                        }

                        exit 1
                    }

                    Write-Host "=========================================="
                    Write-Host "START SERVER TEST PASSED"
                    Write-Host "SERVER IS STILL RUNNING"
                    Write-Host "PID: $serverPid"
                    Write-Host "=========================================="

                    Write-Host "POWERSHELL STEP FINISHING NOW"
                '''

                echo '=========================================='
                echo 'START TEST SERVER STAGE FINISHED'
                echo '=========================================='
            }
        }


        stage('Selenium Tests') {

            options {
                timeout(time: 4, unit: 'MINUTES')
            }

            steps {

                echo '=========================================='
                echo 'SELENIUM TEST STAGE STARTED'
                echo '=========================================='

                bat '''
                    echo ===== SELENIUM COMMAND START =====

                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    if errorlevel 1 (
                        echo ===== SELENIUM TESTS FAILED =====
                        exit /b 1
                    )

                    echo ===== SELENIUM TESTS PASSED =====
                '''

                echo '=========================================='
                echo 'SELENIUM TEST STAGE FINISHED'
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