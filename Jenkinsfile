pipeline {

    agent any

    options {
        skipStagesAfterUnstable()
        disableConcurrentBuilds()
        timestamps()
        timeout(time: 10, unit: 'MINUTES')
    }

    parameters {
        choice(
            name: 'DEPLOY_ENV',
            choices: ['development', 'staging'],
            description: 'Select the deployment environment'
        )
    }

    environment {
        JAVA_HOME = 'C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot'
        PATH = "${JAVA_HOME}\\bin;${env.PATH}"

        APP_PORT = '8081'
        APP_URL = 'http://localhost:8081'

        TEST_SERVER_PID = ''
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building the application...'

                bat '''
                    echo ===== BUILD =====
                    mvnw.cmd clean compile -DskipTests
                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging Spring Boot application...'

                bat '''
                    echo ===== PACKAGE =====
                    mvnw.cmd package -DskipTests
                    if errorlevel 1 exit /b 1
                '''
            }
        }

        stage('Start Test Server') {
            steps {

                echo 'Starting application for Selenium testing...'

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $java = "$env:JAVA_HOME\\\\bin\\\\java.exe"
                    $jar = "$env:WORKSPACE\\\\target\\\\accessportal-0.0.1-SNAPSHOT.jar"

                    $outputLog = "$env:WORKSPACE\\\\target\\\\selenium-output.log"
                    $errorLog = "$env:WORKSPACE\\\\target\\\\selenium-error.log"
                    $pidFile = "$env:WORKSPACE\\\\target\\\\selenium-server.pid"

                    Write-Host "===== START TEST SERVER ====="

                    Write-Host "Checking port 8081..."

                    $existing = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existing) {

                        Write-Host "Port 8081 is already in use."

                        $existingPids = $existing |
                            Select-Object -ExpandProperty OwningProcess -Unique

                        foreach ($processId in $existingPids) {

                            Write-Host "Stopping existing process PID: $processId"

                            taskkill /F /PID $processId /T 2>$null
                        }

                        Start-Sleep -Seconds 2
                    }

                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

                    if (-not (Test-Path $jar)) {
                        Write-Host "ERROR: JAR file not found:"
                        Write-Host $jar
                        exit 1
                    }

                    Write-Host "Starting test application..."

                    $process = Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "$env:WORKSPACE" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden `
                        -PassThru

                    $serverPid = $process.Id

                    Write-Host "Spring Boot PID: $serverPid"

                    Set-Content `
                        -Path $pidFile `
                        -Value $serverPid

                    Write-Host "Waiting for application startup..."

                    $started = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 1

                        $runningProcess = Get-Process `
                            -Id $serverPid `
                            -ErrorAction SilentlyContinue

                        if (-not $runningProcess) {

                            Write-Host "ERROR: Spring Boot process stopped unexpectedly."

                            Write-Host "===== APPLICATION OUTPUT ====="

                            if (Test-Path $outputLog) {
                                Get-Content $outputLog -Tail 100
                            }

                            Write-Host "===== APPLICATION ERROR ====="

                            if (Test-Path $errorLog) {
                                Get-Content $errorLog -Tail 100
                            }

                            exit 1
                        }

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "http://localhost:8081/login" `
                                -UseBasicParsing `
                                -TimeoutSec 2 `
                                -ErrorAction Stop

                            if ($response.StatusCode -eq 200) {

                                $started = $true

                                Write-Host "Application is responding on /login."
                                Write-Host "Application started after approximately $i seconds."

                                break
                            }

                        } catch {

                            Write-Host "Waiting for application... $i/30"
                        }
                    }

                    if (-not $started) {

                        Write-Host "ERROR: Application did not become ready."

                        Write-Host "===== APPLICATION OUTPUT ====="

                        if (Test-Path $outputLog) {
                            Get-Content $outputLog -Tail 100
                        }

                        Write-Host "===== APPLICATION ERROR ====="

                        if (Test-Path $errorLog) {
                            Get-Content $errorLog -Tail 100
                        }

                        Write-Host "Stopping server PID $serverPid"

                        taskkill /F /PID $serverPid /T 2>$null

                        exit 1
                    }

                    Write-Host "Test application started successfully."
                    Write-Host "Test server PID: $serverPid"
                    Write-Host "Proceeding to Selenium Tests..."
                '''
            }
        }

        stage('Selenium Tests') {

            options {
                timeout(time: 3, unit: 'MINUTES')
            }

            steps {

                echo 'Running Selenium UI tests...'

                bat '''
                    echo ===== STARTING SELENIUM TESTS =====

                    mvnw.cmd -Dtest=PortalSeleniumTests test

                    if errorlevel 1 (
                        echo ===== SELENIUM TESTS FAILED =====
                        exit /b 1
                    )

                    echo ===== SELENIUM TESTS PASSED =====
                '''
            }
        }

        stage('Stop Test Server') {

            steps {

                echo 'Stopping Selenium test server...'

                powershell '''
                    $pidFile = "$env:WORKSPACE\\\\target\\\\selenium-server.pid"

                    if (Test-Path $pidFile) {

                        $serverPid = Get-Content $pidFile

                        Write-Host "Test server PID from file: $serverPid"

                        $process = Get-Process `
                            -Id $serverPid `
                            -ErrorAction SilentlyContinue

                        if ($process) {

                            Write-Host "Stopping test server PID $serverPid..."

                            taskkill /F /PID $serverPid /T 2>$null

                            Start-Sleep -Seconds 2

                            Write-Host "Test server stopped."
                        }
                        else {

                            Write-Host "Test server process already stopped."
                        }

                        Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

                    }
                    else {

                        Write-Host "No test server PID file found."
                    }
                '''
            }
        }

        stage('Deploy') {

            steps {

                echo "All Selenium tests passed."
                echo "Deploying application to ${params.DEPLOY_ENV}..."

                bat '''
                    echo ===== DEPLOYMENT =====

                    if not exist C:\\deploy (
                        mkdir C:\\deploy
                    )

                    copy /Y target\\accessportal-0.0.1-SNAPSHOT.jar C:\\deploy\\accessportal.jar

                    if errorlevel 1 exit /b 1
                '''

                powershell '''
                    $ErrorActionPreference = "Stop"

                    $java = "$env:JAVA_HOME\\\\bin\\\\java.exe"
                    $jar = "C:\\deploy\\accessportal.jar"

                    $outputLog = "C:\\deploy\\accessportal-output.log"
                    $errorLog = "C:\\deploy\\accessportal-error.log"

                    Write-Host "===== DEPLOY APPLICATION ====="

                    Write-Host "Checking port 8081..."

                    $existing = Get-NetTCPConnection `
                        -LocalPort 8081 `
                        -State Listen `
                        -ErrorAction SilentlyContinue

                    if ($existing) {

                        $existingPids = $existing |
                            Select-Object -ExpandProperty OwningProcess -Unique

                        foreach ($processId in $existingPids) {

                            Write-Host "Stopping existing process PID $processId"

                            taskkill /F /PID $processId /T 2>$null
                        }

                        Start-Sleep -Seconds 2
                    }

                    Remove-Item $outputLog -Force -ErrorAction SilentlyContinue
                    Remove-Item $errorLog -Force -ErrorAction SilentlyContinue

                    Write-Host "Starting deployed Spring Boot application..."

                    Start-Process `
                        -FilePath $java `
                        -ArgumentList "-jar `"$jar`"" `
                        -WorkingDirectory "C:\\deploy" `
                        -RedirectStandardOutput $outputLog `
                        -RedirectStandardError $errorLog `
                        -WindowStyle Hidden

                    Write-Host "Waiting for deployed application..."

                    $started = $false

                    for ($i = 1; $i -le 30; $i++) {

                        Start-Sleep -Seconds 1

                        try {

                            $response = Invoke-WebRequest `
                                -Uri "http://localhost:8081/login" `
                                -UseBasicParsing `
                                -TimeoutSec 2 `
                                -ErrorAction Stop

                            if ($response.StatusCode -eq 200) {

                                $started = $true

                                Write-Host "Deployed application is responding."
                                break
                            }

                        } catch {

                            Write-Host "Waiting for deployment... $i/30"
                        }
                    }

                    if (-not $started) {

                        Write-Host "ERROR: Deployment failed."

                        Write-Host "===== DEPLOYMENT OUTPUT ====="

                        if (Test-Path $outputLog) {
                            Get-Content $outputLog -Tail 100
                        }

                        Write-Host "===== DEPLOYMENT ERROR ====="

                        if (Test-Path $errorLog) {
                            Get-Content $errorLog -Tail 100
                        }

                        exit 1
                    }

                    Write-Host "Application deployed successfully."
                    Write-Host "Application is running on port 8081."
                '''

                echo "Environment: ${params.DEPLOY_ENV}"
                echo "Application URL: http://localhost:8081/login"
            }
        }
    }

    post {

        always {

            echo 'Publishing Selenium test reports...'

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

            echo '======================================'
            echo 'WEEK 10 PIPELINE SUCCESS'
            echo '======================================'
            echo 'Build successful.'
            echo 'Selenium tests passed.'
            echo 'Application deployed successfully.'
        }

        failure {

            echo '======================================'
            echo 'WEEK 10 PIPELINE FAILED'
            echo '======================================'
            echo 'Check the failed stage and Selenium reports.'
        }

        aborted {

            echo '======================================'
            echo 'WEEK 10 PIPELINE ABORTED'
            echo '======================================'
        }
    }
}