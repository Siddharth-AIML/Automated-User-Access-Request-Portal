pipeline {

    agent any

    stages {

        stage('Checkout') {
            steps {
                echo '=========================================='
                echo 'CHECKOUT'
                echo '=========================================='

                checkout scm
            }
        }


        stage('Build Application') {
            steps {
                echo '=========================================='
                echo 'BUILD APPLICATION'
                echo '=========================================='

                bat '''
                    echo ===== MAVEN BUILD START =====
                    call mvnw.cmd clean package -DskipTests
                    echo ===== MAVEN BUILD FINISHED =====
                '''

                echo '=========================================='
                echo 'BUILD SUCCESS'
                echo '=========================================='
            }
        }


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

                    # Start Spring Boot
                    $process = Start-Process `
                        -FilePath "java" `
                        -ArgumentList "-jar `"$jar`"" `
                        -PassThru `
                        -WindowStyle Hidden

                    Write-Host "Spring Boot PID: $($process.Id)"

                    # Save PID for later stages
                    $process.Id | Out-File `
                        -FilePath (Join-Path $env:WORKSPACE "test-server.pid") `
                        -Encoding ascii `
                        -Force

                    Write-Host "PID saved: $($process.Id)"
                    Write-Host "Waiting for application..."

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        Write-Host "Waiting... attempt $i"

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

                            Write-Host "Application not ready yet..."
                        }
                    }

                    if (!$ready) {

                        Write-Host "=========================================="
                        Write-Host "APPLICATION FAILED TO START"
                        Write-Host "=========================================="

                        Stop-Process `
                            -Id $process.Id `
                            -Force `
                            -ErrorAction SilentlyContinue

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


        stage('Selenium Tests') {

            steps {

                timeout(time: 4, unit: 'MINUTES') {

                    echo '=========================================='
                    echo 'SELENIUM TEST STAGE STARTED'
                    echo '=========================================='

                    bat '''
                        echo ===== SELENIUM COMMAND START =====

                        call mvnw.cmd -Dtest=PortalSeleniumTests test

                        echo ===== SELENIUM COMMAND END =====
                    '''

                    echo '=========================================='
                    echo 'SELENIUM TESTS PASSED'
                    echo '=========================================='
                }
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