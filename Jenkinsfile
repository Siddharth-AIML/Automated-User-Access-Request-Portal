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

                    if errorlevel 1 (
                        echo ===== MAVEN WRAPPER FAILED =====
                        exit /b 1
                    )

                    echo ===== MAVEN VERSION FINISHED =====
                '''

                echo '===== MAVEN TEST END ====='
            }
        }

        stage('Build Application') {
            steps {

                echo '===== BUILD START ====='

                bat '''
                    call mvnw.cmd clean package -DskipTests

                    if errorlevel 1 (
                        echo ===== BUILD FAILED =====
                        exit /b 1
                    )
                '''

                echo '===== BUILD SUCCESS ====='
            }
        }

        stage('Start Test Server') {
            steps {

                echo '===== STARTING TEST SERVER ====='

                powershell '''
                    Write-Host "=========================================="
                    Write-Host "START TEST SERVER"
                    Write-Host "=========================================="

                    $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"

                    Write-Host "JAR: $jar"
                    Write-Host "Port: 8081"

                    if (!(Test-Path $jar)) {
                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    # Check whether port 8081 is already occupied
                    Write-Host "Checking port 8081..."

                    $existingConnection = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existingConnection) {

                        Write-Host "Port 8081 is already in use."

                        $existingPid = $existingConnection.OwningProcess

                        Write-Host "Existing PID: $existingPid"

                        try {
                            Stop-Process `
                                -Id $existingPid `
                                -Force `
                                -ErrorAction SilentlyContinue

                            Write-Host "Existing application stopped."
                        }
                        catch {
                            Write-Host "Could not stop existing process."
                        }

                        Start-Sleep -Seconds 2
                    }

                    Write-Host "Starting Spring Boot application..."

                    $stdoutLog = Join-Path $env:WORKSPACE "target\\jenkins-springboot-output.log"
                    $stderrLog = Join-Path $env:WORKSPACE "target\\jenkins-springboot-error.log"

                    $process = Start-Process `
                        -FilePath "java" `
                        -ArgumentList "-jar `"$jar`"" `
                        -RedirectStandardOutput $stdoutLog `
                        -RedirectStandardError $stderrLog `
                        -PassThru `
                        -WindowStyle Hidden

                    Write-Host "Spring Boot PID: $($process.Id)"

                    # Save PID so later stages/post block can find it
                    $process.Id | Out-File `
                        -FilePath (Join-Path $env:WORKSPACE "test-server.pid") `
                        -Encoding ascii

                    Write-Host "Test server PID saved."

                    Write-Host "Waiting for application startup..."

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        # First check whether Java process is still alive
                        $runningProcess = Get-Process `
                            -Id $process.Id `
                            -ErrorAction SilentlyContinue

                        if (!$runningProcess) {

                            Write-Host "ERROR: Spring Boot process stopped unexpectedly."

                            Write-Host "===== SPRING BOOT ERROR LOG ====="

                            if (Test-Path $stderrLog) {
                                Get-Content $stderrLog -Tail 50
                            }

                            Write-Host "===== SPRING BOOT OUTPUT LOG ====="

                            if (Test-Path $stdoutLog) {
                                Get-Content $stdoutLog -Tail 50
                            }

                            exit 1
                        }

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

                            Write-Host "Waiting for application... attempt $i"
                        }
                    }

                    if (!$ready) {

                        Write-Host "ERROR: APPLICATION DID NOT START"

                        Write-Host "===== SPRING BOOT ERROR LOG ====="

                        if (Test-Path $stderrLog) {
                            Get-Content $stderrLog -Tail 100
                        }

                        Write-Host "===== SPRING BOOT OUTPUT LOG ====="

                        if (Test-Path $stdoutLog) {
                            Get-Content $stdoutLog -Tail 100
                        }

                        Stop-Process `
                            -Id $process.Id `
                            -Force `
                            -ErrorAction SilentlyContinue

                        exit 1
                    }

                    Write-Host "=========================================="
                    Write-Host "TEST SERVER READY"
                    Write-Host "PID: $($process.Id)"
                    Write-Host "URL: http://localhost:8081/login"
                    Write-Host "=========================================="

                    # IMPORTANT:
                    # DO NOT STOP THE APPLICATION HERE.
                    # Selenium needs the application to remain running.
                '''

                echo '===== TEST SERVER STARTED SUCCESSFULLY ====='
            }
        }

        stage('Selenium Tests') {

            options {
                timeout(time: 5, unit: 'MINUTES')
            }

            steps {

                echo '=========================================='
                echo 'SELENIUM STAGE STARTED'
                echo '=========================================='

                echo 'Checking whether test server is still running...'

                powershell '''
                    if (!(Test-Path "$env:WORKSPACE\\test-server.pid")) {
                        Write-Host "ERROR: test-server.pid not found"
                        exit 1
                    }

                    $pid = Get-Content "$env:WORKSPACE\\test-server.pid"

                    Write-Host "Test server PID: $pid"

                    $process = Get-Process `
                        -Id $pid `
                        -ErrorAction SilentlyContinue

                    if (!$process) {
                        Write-Host "ERROR: Test server is not running."
                        exit 1
                    }

                    Write-Host "Test server is still running."

                    try {

                        $response = Invoke-WebRequest `
                            -Uri "http://localhost:8081/login" `
                            -UseBasicParsing `
                            -TimeoutSec 5

                        Write-Host "Login page HTTP status: $($response.StatusCode)"

                        if ($response.StatusCode -ne 200) {
                            Write-Host "ERROR: Login page is not healthy."
                            exit 1
                        }

                    }
                    catch {

                        Write-Host "ERROR: Cannot access http://localhost:8081/login"
                        Write-Host $_
                        exit 1
                    }
                '''

                echo '===== TEST SERVER HEALTH CHECK PASSED ====='

                bat '''
                    echo ==========================================
                    echo RUNNING SELENIUM TESTS
                    echo ==========================================

                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    if errorlevel 1 (
                        echo ==========================================
                        echo SELENIUM TESTS FAILED
                        echo ==========================================
                        exit /b 1
                    )

                    echo ==========================================
                    echo SELENIUM TESTS PASSED
                    echo ==========================================
                '''
            }
        }
    }

    post {

        always {

            echo '=========================================='
            echo 'CLEANUP'
            echo '=========================================='

            powershell '''
                $pidFile = Join-Path $env:WORKSPACE "test-server.pid"

                if (Test-Path $pidFile) {

                    $pid = Get-Content $pidFile

                    Write-Host "Test server PID: $pid"

                    $process = Get-Process `
                        -Id $pid `
                        -ErrorAction SilentlyContinue

                    if ($process) {

                        Write-Host "Stopping test server..."

                        Stop-Process `
                            -Id $pid `
                            -Force `
                            -ErrorAction SilentlyContinue

                        Write-Host "Test server stopped."

                    } else {

                        Write-Host "Test server is already stopped."
                    }

                    Remove-Item $pidFile `
                        -Force `
                        -ErrorAction SilentlyContinue

                } else {

                    Write-Host "No test-server.pid found."
                }

                Write-Host "===== CLEANUP COMPLETE ====="
            '''

            echo '===== PIPELINE FINISHED ====='
        }
    }
}