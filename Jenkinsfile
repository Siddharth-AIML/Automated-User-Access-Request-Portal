pipeline {

    agent any

    stages {

        // =========================================================
        // 1. CHECKOUT
        // =========================================================
        stage('Checkout') {
            steps {

                echo '=========================================='
                echo '===== CHECKOUT START ====='
                echo '=========================================='

                checkout scm

                echo '===== CHECKOUT END ====='
            }
        }


        // =========================================================
        // 2. TEST POWERSHELL
        // =========================================================
        stage('Test PowerShell') {
            steps {

                echo '=========================================='
                echo '===== POWERSHELL TEST ====='
                echo '=========================================='

                powershell '''
                    Write-Host "===== POWERSHELL START ====="
                    Write-Host "Workspace: $env:WORKSPACE"
                    Write-Host "User: $env:USERNAME"
                    Write-Host "Java version:"
                    java -version
                    Write-Host "===== POWERSHELL END ====="
                '''

                echo '===== POWERSHELL TEST PASSED ====='
            }
        }


        // =========================================================
        // 3. TEST MAVEN WRAPPER
        // =========================================================
        stage('Test Maven Wrapper') {
            steps {

                echo '=========================================='
                echo '===== MAVEN TEST START ====='
                echo '=========================================='

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


        // =========================================================
        // 4. BUILD APPLICATION
        // =========================================================
        stage('Build Application') {
            steps {

                echo '=========================================='
                echo '===== BUILD START ====='
                echo '=========================================='

                bat '''
                    call mvnw.cmd clean package -DskipTests
                '''

                echo '===== BUILD SUCCESS ====='
            }
        }


        // =========================================================
        // 5. START SPRING BOOT TEST SERVER
        // =========================================================
        stage('Start Test Server') {
            steps {

                echo '=========================================='
                echo '===== STARTING TEST SERVER ====='
                echo '=========================================='

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"
                    $port = 8081

                    Write-Host "JAR: $jar"
                    Write-Host "Port: $port"

                    # -------------------------------------------------
                    # Check JAR
                    # -------------------------------------------------

                    if (!(Test-Path $jar)) {

                        Write-Host "ERROR: JAR NOT FOUND"
                        exit 1
                    }

                    Write-Host "JAR FOUND"


                    # -------------------------------------------------
                    # Check whether port 8081 is already occupied
                    # -------------------------------------------------

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


                    # -------------------------------------------------
                    # Log files
                    # -------------------------------------------------

                    $serverLog = Join-Path `
                        $env:WORKSPACE `
                        "server.log"

                    $serverErrorLog = Join-Path `
                        $env:WORKSPACE `
                        "server-error.log"

                    $pidFile = Join-Path `
                        $env:WORKSPACE `
                        "server.pid"


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


                    # -------------------------------------------------
                    # START SPRING BOOT
                    # -------------------------------------------------

                    Write-Host "Starting Spring Boot application..."

                    $process = Start-Process `
                        -FilePath "java" `
                        -ArgumentList "-jar `"$jar`"" `
                        -RedirectStandardOutput $serverLog `
                        -RedirectStandardError $serverErrorLog `
                        -WindowStyle Hidden `
                        -PassThru


                    Write-Host "Spring Boot PID: $($process.Id)"


                    # -------------------------------------------------
                    # SAVE PID
                    # -------------------------------------------------

                    Set-Content `
                        -Path $pidFile `
                        -Value $process.Id

                    Write-Host "Test server PID saved."

                    Write-Host "Waiting for application startup..."


                    # -------------------------------------------------
                    # WAIT FOR APPLICATION
                    # -------------------------------------------------

                    $ready = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 2

                        # Check whether Java process has died

                        if ($process.HasExited) {

                            Write-Host "ERROR: Spring Boot process exited."

                            if (Test-Path $serverLog) {

                                Write-Host "===== SERVER LOG ====="

                                Get-Content `
                                    $serverLog `
                                    -Tail 100
                            }

                            if (Test-Path $serverErrorLog) {

                                Write-Host "===== SERVER ERROR LOG ====="

                                Get-Content `
                                    $serverErrorLog `
                                    -Tail 100
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

                            Write-Host `
                                "Waiting for application... attempt $i"
                        }
                    }


                    # -------------------------------------------------
                    # APPLICATION FAILED TO START
                    # -------------------------------------------------

                    if (!$ready) {

                        Write-Host "ERROR: APPLICATION DID NOT START"

                        if (Test-Path $serverLog) {

                            Write-Host "===== SERVER LOG ====="

                            Get-Content `
                                $serverLog `
                                -Tail 100
                        }

                        if (Test-Path $serverErrorLog) {

                            Write-Host "===== SERVER ERROR LOG ====="

                            Get-Content `
                                $serverErrorLog `
                                -Tail 100
                        }


                        if ($process -and !$process.HasExited) {

                            Stop-Process `
                                -Id $process.Id `
                                -Force `
                                -ErrorAction SilentlyContinue
                        }

                        exit 1
                    }


                    # -------------------------------------------------
                    # SERVER READY
                    # -------------------------------------------------

                    Write-Host ""
                    Write-Host "=========================================="
                    Write-Host "TEST SERVER READY"
                    Write-Host "PID: $($process.Id)"
                    Write-Host "URL: http://localhost:8081/login"
                    Write-Host "=========================================="
                    Write-Host ""

                    # IMPORTANT:
                    # DO NOT STOP JAVA HERE.
                    #
                    # Selenium needs Spring Boot to remain running.
                    #
                    # The server will be stopped in the post section.
                '''
            }
        }


        // =========================================================
        // 6. SELENIUM TESTS
        // =========================================================
        stage('Selenium Tests') {
            steps {

                echo '=========================================='
                echo '===== SELENIUM STAGE STARTED ====='
                echo '=========================================='

                bat '''
                    echo ===== BEFORE SELENIUM =====

                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    echo ===== AFTER SELENIUM =====
                '''

                echo '=========================================='
                echo '===== SELENIUM STAGE FINISHED ====='
                echo '=========================================='
            }
        }


        // =========================================================
        // 7. SHOW SERVER LOG
        // =========================================================
        stage('Show Server Logs') {
            steps {

                echo '=========================================='
                echo '===== SERVER LOG ====='
                echo '=========================================='

                powershell '''
                    $serverLog = Join-Path `
                        $env:WORKSPACE `
                        "server.log"

                    $serverErrorLog = Join-Path `
                        $env:WORKSPACE `
                        "server-error.log"


                    if (Test-Path $serverLog) {

                        Write-Host "===== STDOUT ====="

                        Get-Content `
                            $serverLog `
                            -Tail 100
                    }


                    if (Test-Path $serverErrorLog) {

                        Write-Host "===== STDERR ====="

                        Get-Content `
                            $serverErrorLog `
                            -Tail 100
                    }
                '''
            }
        }

    }


    // =============================================================
    // CLEANUP
    // =============================================================
    post {

        always {

            echo '=========================================='
            echo '===== CLEANING UP TEST SERVER ====='
            echo '=========================================='


            powershell '''
                $pidFile = Join-Path `
                    $env:WORKSPACE `
                    "server.pid"


                if (Test-Path $pidFile) {

                    $serverPid = Get-Content $pidFile

                    Write-Host "Server PID found: $serverPid"

                    Write-Host "Stopping Spring Boot server..."


                    Stop-Process `
                        -Id ([int]$serverPid) `
                        -Force `
                        -ErrorAction SilentlyContinue


                    Write-Host "Spring Boot server stopped."


                    Remove-Item `
                        $pidFile `
                        -Force `
                        -ErrorAction SilentlyContinue
                }
                else {

                    Write-Host "No server PID file found."
                }


                # -------------------------------------------------
                # Extra safety:
                # Make sure port 8081 is free
                # -------------------------------------------------

                $connections = Get-NetTCPConnection `
                    -LocalPort 8081 `
                    -State Listen `
                    -ErrorAction SilentlyContinue


                if ($connections) {

                    foreach ($connection in $connections) {

                        Write-Host `
                            "Cleaning remaining process PID: $($connection.OwningProcess)"

                        Stop-Process `
                            -Id $connection.OwningProcess `
                            -Force `
                            -ErrorAction SilentlyContinue
                    }
                }


                Write-Host "===== SERVER CLEANUP COMPLETE ====="
            '''


            echo '=========================================='
            echo '===== PIPELINE FINISHED ====='
            echo '=========================================='
        }
    }
}