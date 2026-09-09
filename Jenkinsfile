pipeline {

    agent any

    stages {

        stage('Start Test Server') {

            steps {

                echo '=========================================='
                echo 'START TEST SERVER'
                echo '=========================================='

                powershell '''
                    Write-Host "=========================================="
                    Write-Host "POWERSHELL START"
                    Write-Host "=========================================="

                    Write-Host "Workspace: $env:WORKSPACE"

                    $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"

                    Write-Host "JAR: $jar"

                    if (!(Test-Path $jar)) {
                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    Write-Host "JAR FOUND"

                    Write-Host "Starting Java..."

                    $process = Start-Process `
                        -FilePath "java.exe" `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory $env:WORKSPACE `
                        -PassThru `
                        -WindowStyle Hidden

                    Write-Host "Spring Boot PID: $($process.Id)"

                    $pidFile = Join-Path $env:WORKSPACE "test-server.pid"

                    Set-Content `
                        -Path $pidFile `
                        -Value $process.Id

                    Write-Host "PID saved: $($process.Id)"

                    Write-Host "Waiting for application..."

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
                                Write-Host "PID: $($process.Id)"
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

                        if (Get-Process -Id $process.Id -ErrorAction SilentlyContinue) {
                            Stop-Process -Id $process.Id -Force
                        }

                        exit 1
                    }

                    Write-Host "=========================================="
                    Write-Host "START SERVER TEST PASSED"
                    Write-Host "SERVER IS STILL RUNNING"
                    Write-Host "PID: $($process.Id)"
                    Write-Host "=========================================="

                    Write-Host "POWERSHELL STEP FINISHING NOW"
                '''

                echo '=========================================='
                echo 'START TEST SERVER STAGE FINISHED'
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