pipeline {

    agent any

    options {
        disableConcurrentBuilds()
        skipStagesAfterUnstable()
        timestamps()
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['development', 'staging'],
            description: 'Select deployment environment'
        )
    }

    environment {
        JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot'
        APP_PORT = '8081'
        APP_URL = 'http://localhost:8081/login'
    }

    stages {

        // ============================================================
        // CHECKOUT
        // ============================================================

        stage('Checkout') {
            steps {
                echo '=========================================='
                echo 'CHECKOUT'
                echo '=========================================='

                checkout scm
            }
        }


        // ============================================================
        // BUILD
        // ============================================================

        stage('Build') {
            steps {
                echo '=========================================='
                echo 'BUILD'
                echo '=========================================='

                bat '''
                    call mvnw.cmd clean compile -DskipTests

                    if errorlevel 1 (
                        echo BUILD FAILED
                        exit /b 1
                    )

                    echo BUILD SUCCESS
                '''
            }
        }


        // ============================================================
        // PACKAGE
        // ============================================================

        stage('Package') {
            steps {
                echo '=========================================='
                echo 'PACKAGE'
                echo '=========================================='

                bat '''
                    call mvnw.cmd package -DskipTests

                    if errorlevel 1 (
                        echo PACKAGE FAILED
                        exit /b 1
                    )

                    echo PACKAGE SUCCESS
                '''
            }
        }


        // ============================================================
        // START TEST SERVER
        // ============================================================

        stage('Start Test Server') {

            steps {

                echo '=========================================='
                echo 'STARTING TEST SERVER'
                echo '=========================================='

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $java = "$env:JAVA_HOME\\\\bin\\\\java.exe"
                    $jar = "$env:WORKSPACE\\\\target\\\\accessportal-0.0.1-SNAPSHOT.jar"

                    $pidFile = "$env:WORKSPACE\\\\target\\\\test-server.pid"
                    $outputLog = "$env:WORKSPACE\\\\target\\\\selenium-output.log"
                    $errorLog = "$env:WORKSPACE\\\\target\\\\selenium-error.log"

                    Write-Host "Java: $java"
                    Write-Host "JAR:  $jar"
                    Write-Host "Port: $env:APP_PORT"

                    # ------------------------------------------------
                    # CHECK EXISTING PROCESS
                    # ------------------------------------------------

                    Write-Host "Checking port $env:APP_PORT..."

                    $connection = Get-NetTCPConnection `
                        -LocalPort $env:APP_PORT `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connection) {

                        $oldPid = $connection[0].OwningProcess

                        Write-Host "Port $env:APP_PORT is already in use."
                        Write-Host "Stopping existing process PID: $oldPid"

                        taskkill /F /PID $oldPid 2>$null

                        Start-Sleep -Seconds 3
                    }

                    # ------------------------------------------------
                    # CLEAN OLD FILES
                    # ------------------------------------------------

                    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    # ------------------------------------------------
                    # VERIFY JAR
                    # ------------------------------------------------

                    if (!(Test-Path $jar)) {

                        Write-Host "ERROR: JAR file does not exist:"
                        Write-Host $jar

                        exit 1
                    }

                    Write-Host "JAR verified."

                    # ------------------------------------------------
                    # START APPLICATION
                    # ------------------------------------------------

                    Write-Host "Starting test application..."

                    $process = Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "$env:WORKSPACE" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden `
                        -PassThru

                    $appPid = $process.Id

                    Write-Host "Spring Boot PID: $appPid"

                    # Save PID for cleanup stage
                    Set-Content `
                        -Path $pidFile `
                        -Value $appPid

                    Write-Host "Test server PID saved: $appPid"

                    # ------------------------------------------------
                    # WAIT FOR SERVER
                    # ------------------------------------------------

                    Write-Host "Waiting for application startup..."

                    $started = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 1

                        $running = Get-NetTCPConnection `
                            -LocalPort $env:APP_PORT `
                            -State Listen `
                            -ErrorAction SilentlyContinue

                        if ($running) {

                            Write-Host "Application detected on port $env:APP_PORT after $i seconds."

                            $started = $true

                            break
                        }

                        # Check whether Java process died
                        $processCheck = Get-Process `
                            -Id $appPid `
                            -ErrorAction SilentlyContinue

                        if (!$processCheck) {

                            Write-Host "ERROR: Spring Boot process stopped unexpectedly."

                            if (Test-Path $outputLog) {
                                Write-Host "===== APPLICATION OUTPUT ====="
                                Get-Content $outputLog -Tail 50
                            }

                            if (Test-Path $errorLog) {
                                Write-Host "===== APPLICATION ERROR ====="
                                Get-Content $errorLog -Tail 50
                            }

                            exit 1
                        }
                    }

                    if (!$started) {

                        Write-Host "ERROR: Application failed to start."

                        Write-Host "===== APPLICATION OUTPUT ====="

                        if (Test-Path $outputLog) {
                            Get-Content $outputLog -Tail 100
                        }

                        Write-Host "===== APPLICATION ERROR ====="

                        if (Test-Path $errorLog) {
                            Get-Content $errorLog -Tail 100
                        }

                        taskkill /F /PID $appPid 2>$null

                        exit 1
                    }

                    # ------------------------------------------------
                    # VERIFY LOGIN PAGE
                    # ------------------------------------------------

                    Write-Host "Checking $env:APP_URL ..."

                    try {

                        $response = Invoke-WebRequest `
                            -Uri $env:APP_URL `
                            -UseBasicParsing `
                            -TimeoutSec 10

                        if ($response.StatusCode -ne 200) {

                            Write-Host "ERROR: Login page returned HTTP $($response.StatusCode)"

                            taskkill /F /PID $appPid 2>$null

                            exit 1
                        }

                        Write-Host "Login page returned HTTP 200."
                    }

                    catch {

                        Write-Host "ERROR: Login page could not be reached."
                        Write-Host $_.Exception.Message

                        taskkill /F /PID $appPid 2>$null

                        exit 1
                    }

                    Write-Host "=========================================="
                    Write-Host "TEST SERVER READY"
                    Write-Host "PID: $appPid"
                    Write-Host "URL: $env:APP_URL"
                    Write-Host "=========================================="
                '''
            }
        }


        // ============================================================
        // SELENIUM TESTS
        // ============================================================

        stage('Selenium Tests') {

            options {
                timeout(
                    time: 4,
                    unit: 'MINUTES'
                )
            }

            steps {

                echo '=========================================='
                echo 'STARTING SELENIUM TESTS'
                echo '=========================================='

                bat '''
                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    if errorlevel 1 (
                        echo SELENIUM TESTS FAILED
                        exit /b 1
                    )

                    echo SELENIUM TESTS PASSED
                '''

                echo '=========================================='
                echo 'SELENIUM TESTS COMPLETED'
                echo '=========================================='
            }
        }


        // ============================================================
        // STOP TEST SERVER
        // ============================================================

        stage('Stop Test Server') {

            steps {

                echo '=========================================='
                echo 'STOPPING TEST SERVER'
                echo '=========================================='

                powershell '''
                    $ErrorActionPreference = "SilentlyContinue"

                    $pidFile = "$env:WORKSPACE\\\\target\\\\test-server.pid"

                    if (Test-Path $pidFile) {

                        $appPid = Get-Content $pidFile

                        Write-Host "Stopping test server PID: $appPid"

                        taskkill /F /PID $appPid

                        Remove-Item $pidFile -Force
                    }
                    else {

                        Write-Host "PID file not found."

                        $connection = Get-NetTCPConnection `
                            -LocalPort $env:APP_PORT `
                            -State Listen `
                            -ErrorAction SilentlyContinue

                        if ($connection) {

                            $appPid = $connection[0].OwningProcess

                            Write-Host "Stopping process using port $env:APP_PORT"
                            Write-Host "PID: $appPid"

                            taskkill /F /PID $appPid
                        }
                    }

                    Start-Sleep -Seconds 2

                    $remaining = Get-NetTCPConnection `
                        -LocalPort $env:APP_PORT `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($remaining) {

                        Write-Host "WARNING: Port $env:APP_PORT is still in use."
                    }
                    else {

                        Write-Host "Test server stopped successfully."
                    }
                '''
            }
        }


        // ============================================================
        // DEPLOY
        // ============================================================

        stage('Deploy') {

            steps {

                echo '=========================================='
                echo 'DEPLOY'
                echo '=========================================='

                echo "Deploying to ${params.DEPLOY_ENV} environment..."

                bat '''
                    if not exist C:\\deploy (
                        mkdir C:\\deploy
                    )

                    copy /Y target\\accessportal-0.0.1-SNAPSHOT.jar C:\\deploy\\accessportal.jar

                    if errorlevel 1 (
                        echo DEPLOYMENT COPY FAILED
                        exit /b 1
                    )

                    echo JAR copied successfully.
                '''

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $java = "$env:JAVA_HOME\\\\bin\\\\java.exe"
                    $jar = "C:\\\\deploy\\\\accessportal.jar"

                    $outputLog = "C:\\\\deploy\\\\accessportal-output.log"
                    $errorLog = "C:\\\\deploy\\\\accessportal-error.log"

                    Write-Host "Checking port $env:APP_PORT..."

                    $connection = Get-NetTCPConnection `
                        -LocalPort $env:APP_PORT `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($connection) {

                        $oldPid = $connection[0].OwningProcess

                        Write-Host "Stopping existing deployed application PID: $oldPid"

                        taskkill /F /PID $oldPid

                        Start-Sleep -Seconds 3
                    }

                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    Write-Host "Starting deployed Spring Boot application..."

                    $process = Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "C:\\deploy" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden `
                        -PassThru

                    Write-Host "Deployment PID: $($process.Id)"

                    $started = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 1

                        $running = Get-NetTCPConnection `
                            -LocalPort $env:APP_PORT `
                            -State Listen `
                            -ErrorAction SilentlyContinue

                        if ($running) {

                            $started = $true

                            Write-Host "Application is running on port $env:APP_PORT."

                            break
                        }
                    }

                    if (!$started) {

                        Write-Host "ERROR: Deployed application failed to start."

                        Write-Host "===== DEPLOYMENT OUTPUT ====="

                        if (Test-Path $outputLog) {
                            Get-Content $outputLog -Tail 100
                        }

                        Write-Host "===== DEPLOYMENT ERROR ====="

                        if (Test-Path $errorLog) {
                            Get-Content $errorLog -Tail 100
                        }

                        taskkill /F /PID $process.Id 2>$null

                        exit 1
                    }

                    try {

                        $response = Invoke-WebRequest `
                            -Uri $env:APP_URL `
                            -UseBasicParsing `
                            -TimeoutSec 10

                        if ($response.StatusCode -eq 200) {

                            Write-Host "Application health check passed."
                        }
                        else {

                            Write-Host "ERROR: Health check returned $($response.StatusCode)"

                            exit 1
                        }
                    }

                    catch {

                        Write-Host "ERROR: Deployment health check failed."
                        Write-Host $_.Exception.Message

                        exit 1
                    }
                '''

                echo "=========================================="
                echo "DEPLOYMENT SUCCESSFUL"
                echo "=========================================="
                echo "Environment: ${params.DEPLOY_ENV}"
                echo "Application URL: ${env.APP_URL}"
            }
        }
    }


    // ================================================================
    // POST ACTIONS
    // ================================================================

    post {

        always {

            echo '=========================================='
            echo 'PUBLISHING TEST REPORTS'
            echo '=========================================='

            junit(
                testResults: 'target/surefire-reports/*.xml',
                allowEmptyResults: true
            )

            archiveArtifacts(
                artifacts: 'target/screenshots/*.png',
                allowEmptyArchive: true
            )

            archiveArtifacts(
                artifacts: 'target/selenium-*.log',
                allowEmptyArchive: true
            )
        }

        success {   

            echo '=========================================='
            echo 'PIPELINE SUCCESS'
            echo '=========================================='

            echo 'Week 10 Continuous Testing Pipeline completed successfully.'
            echo 'Selenium tests passed.'
            echo 'Application deployment completed.'
        }

        failure {

            echo '=========================================='
            echo 'PIPELINE FAILED'
            echo '=========================================='

            echo 'Week 10 Pipeline FAILED.'
            echo 'Check the stage where the failure occurred.'
        }

        aborted {

            echo '=========================================='
            echo 'PIPELINE ABORTED'
            echo '=========================================='
        }
    }
}