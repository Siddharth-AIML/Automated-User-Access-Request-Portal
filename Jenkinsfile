pipeline {

    agent any

    stages {

        stage('Checkout') {
            steps {
                echo '===== CHECKOUT START ====='

                checkout scm

                echo '===== CHECKOUT END ====='
            }
        }

        stage('Test PowerShell') {
            steps {

                echo '===== BEFORE POWERSHELL ====='

                powershell '''
                    Write-Host "===== POWERSHELL START ====="
                    Write-Host "Workspace: $env:WORKSPACE"
                    Write-Host "User: $env:USERNAME"
                    Write-Host "===== POWERSHELL END ====="
                '''

                echo '===== AFTER POWERSHELL ====='
            }
        }

        stage('Test Maven Wrapper') {
            steps {

                echo '===== MAVEN TEST START ====='

                bat '''
                    echo ===== CMD START =====
                    cd
                    dir
                    echo ===== RUNNING MAVEN VERSION =====
                    call mvnw.cmd -version
                    echo ===== MAVEN VERSION FINISHED =====
                '''

                echo '===== MAVEN TEST END ====='
            }
        }

        stage('Test Spring Boot Start') {
            steps {

                echo '===== SPRING BOOT TEST START ====='

                bat '''
                    call mvnw.cmd package -DskipTests
                '''

                powershell '''
                    Write-Host "===== STARTING SPRING BOOT ====="

                    $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"

                    Write-Host "JAR: $jar"

                    if (!(Test-Path $jar)) {
                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    $process = Start-Process `
                        -FilePath "java" `
                        -ArgumentList "-jar `"$jar`"" `
                        -PassThru

                    Write-Host "JAVA PID: $($process.Id)"

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        try {
                            $response = Invoke-WebRequest `
                                -Uri "http://localhost:8081/login" `
                                -UseBasicParsing `
                                -TimeoutSec 3

                            if ($response.StatusCode -eq 200) {
                                Write-Host "===== APPLICATION READY ====="
                                Write-Host "HTTP STATUS: $($response.StatusCode)"
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
                        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
                        exit 1
                    }

                    Write-Host "===== APPLICATION TEST PASSED ====="

                    Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue

                    Write-Host "===== APPLICATION STOPPED ====="
                '''

                echo '===== SPRING BOOT TEST END ====='
            }
        }

        stage('Test Selenium Command') {
            steps {

                echo '===== SELENIUM STAGE STARTED ====='

                bat '''
                    echo ===== BEFORE SELENIUM =====
                    call mvnw.cmd -Dtest=PortalSeleniumTests test
                    echo ===== AFTER SELENIUM =====
                '''

                echo '===== SELENIUM STAGE FINISHED ====='
            }
        }
    }

    post {
        always {
            echo '===== PIPELINE FINISHED ====='
        }
    }
}