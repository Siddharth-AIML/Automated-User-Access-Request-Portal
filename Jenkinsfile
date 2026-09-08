pipeline {

    agent any

    options {
        timestamps()
        skipDefaultCheckout(true)
    }

    stages {

        stage('Checkout') {
            steps {
                echo '===== CHECKOUT START ====='

                checkout scm

                echo '===== CHECKOUT END ====='
            }
        }


        stage('Build Application') {
            steps {

                echo '===== BUILD START ====='

                bat '''
                    call mvnw.cmd clean package -DskipTests
                    if errorlevel 1 exit /b 1
                '''

                echo '===== BUILD SUCCESS ====='
            }
        }


        stage('Start Test Server') {
            steps {

                echo '===== STARTING TEST SERVER ====='

                powershell '''
                    $ErrorActionPreference = "Stop"

                    Write-Host "=========================================="
                    Write-Host "START TEST SERVER"
                    Write-Host "=========================================="

                    $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"
                    $pidFile = Join-Path $env:WORKSPACE "test-server.pid"

                    Write-Host "JAR: $jar"
                    Write-Host "Port: 8081"

                    if (!(Test-Path $jar)) {
                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    # ------------------------------------------
                    # Kill anything already using port 8081
                    # ------------------------------------------

                    Write-Host "Checking port 8081..."

                    $existing = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existing) {

                        foreach ($connection in $existing) {

                            $oldPid = $connection.OwningProcess

                            Write-Host "Port 8081 already used by PID: $oldPid"

                            if ($oldPid -and $oldPid -ne 0) {

                                taskkill /F /PID $oldPid 2>$null

                                Start-Sleep -Seconds 2
                            }
                        }
                    }

                    # ------------------------------------------
                    # Start Java COMPLETELY DETACHED
                    # ------------------------------------------

                    Write-Host "Starting Spring Boot application..."

                    $javaPath = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"

                    if (!(Test-Path $javaPath)) {
                        Write-Host "ERROR: Java not found:"
                        Write-Host $javaPath
                        exit 1
                    }

                    $stdout = Join-Path $env:WORKSPACE "springboot-output.log"
                    $stderr = Join-Path $env:WORKSPACE "springboot-error.log"

                    $process = Start-Process `
                        -FilePath $javaPath `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory $env:WORKSPACE `
                        -RedirectStandardOutput $stdout `
                        -RedirectStandardError $stderr `
                        -WindowStyle Hidden `
                        -PassThru

                    $serverPid = $process.Id

                    Write-Host "Spring Boot PID: $serverPid"

                    # Save PID
                    Set-Content -Path $pidFile -Value $serverPid

                    Write-Host "Test server PID saved."
                    Write-Host "Waiting for application startup..."

                    # ------------------------------------------
                    # Wait for application
                    # ------------------------------------------

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        Write-Host "Startup check $i/30"

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "http://127.0.0.1:8081/login" `
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

                            Write-Host "Application not ready yet..."
                        }
                    }

                    if (!$ready) {

                        Write-Host "=========================================="
                        Write-Host "APPLICATION FAILED TO START"
                        Write-Host "=========================================="

                        if (Test-Path $stdout) {
                            Write-Host "----- SPRING BOOT OUTPUT -----"
                            Get-Content $stdout -Tail 50
                        }

                        if (Test-Path $stderr) {
                            Write-Host "----- SPRING BOOT ERROR -----"
                            Get-Content $stderr -Tail 50
                        }

                        taskkill /F /PID $serverPid 2>$null

                        exit 1
                    }

                    Write-Host "=========================================="
                    Write-Host "TEST SERVER READY"
                    Write-Host "PID: $serverPid"
                    Write-Host "URL: http://localhost:8081/login"
                    Write-Host "=========================================="

                    # IMPORTANT:
                    # DO NOT Stop-Process here.
                    # DO NOT Wait-Process here.
                    # This PowerShell step must finish now.

                    Write-Host "Returning control to Jenkins..."
                '''

                echo '===== START TEST SERVER STAGE FINISHED ====='
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

                bat '''
                    echo ===== BEFORE SELENIUM =====

                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    if errorlevel 1 (
                        echo ===== SELENIUM TESTS FAILED =====
                        exit /b 1
                    )

                    echo ===== SELENIUM TESTS PASSED =====
                '''

                echo '=========================================='
                echo 'SELENIUM STAGE FINISHED'
                echo '=========================================='
            }
        }
    }


    post {

        always {

            echo '===== CLEANUP START ====='

            powershell '''
                $pidFile = Join-Path $env:WORKSPACE "test-server.pid"

                if (Test-Path $pidFile) {

                    $serverPid = Get-Content $pidFile

                    Write-Host "Test server PID: $serverPid"

                    if ($serverPid) {

                        Write-Host "Stopping test server..."

                        taskkill /F /PID $serverPid 2>$null

                        Start-Sleep -Seconds 2
                    }

                    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
                }

                Write-Host "Checking port 8081..."

                $remaining = Get-NetTCPConnection `
                    -LocalPort 8081 `
                    -State Listen `
                    -ErrorAction SilentlyContinue

                if ($remaining) {

                    foreach ($connection in $remaining) {

                        $remainingPid = $connection.OwningProcess

                        Write-Host "Port still occupied by PID: $remainingPid"

                        taskkill /F /PID $remainingPid 2>$null
                    }
                }

                Write-Host "===== CLEANUP FINISHED ====="
            '''

            archiveArtifacts artifacts: 'springboot-output.log,springboot-error.log', allowEmptyArchive: true
        }
    }
}