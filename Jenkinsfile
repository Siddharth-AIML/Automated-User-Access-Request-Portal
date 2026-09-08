stage('Start Test Server') {
    steps {
        echo '===== STARTING TEST SERVER ====='

        powershell '''
            Write-Host "=========================================="
            Write-Host "START TEST SERVER"
            Write-Host "=========================================="

            $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"
            $port = 8081

            Write-Host "JAR: $jar"
            Write-Host "Port: $port"

            if (!(Test-Path $jar)) {
                Write-Host "ERROR: JAR NOT FOUND"
                exit 1
            }

            # Check whether port is already occupied
            $existing = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue

            if ($existing) {
                Write-Host "Port $port is already in use."

                $existingPid = $existing.OwningProcess
                Write-Host "Existing PID: $existingPid"

                Stop-Process -Id $existingPid -Force -ErrorAction SilentlyContinue

                Start-Sleep -Seconds 2
            }

            Write-Host "Starting Spring Boot application..."

            $java = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"

            if (!(Test-Path $java)) {
                Write-Host "ERROR: Java not found at $java"
                exit 1
            }

            $process = Start-Process `
                -FilePath $java `
                -ArgumentList "-jar `"$jar`"" `
                -WindowStyle Hidden `
                -RedirectStandardOutput "$env:WORKSPACE\\target\\jenkins-spring.log" `
                -RedirectStandardError "$env:WORKSPACE\\target\\jenkins-spring-error.log" `
                -PassThru

            $pid = $process.Id

            Write-Host "Spring Boot PID: $pid"

            # Save PID for the Selenium/cleanup stages
            Set-Content `
                -Path "$env:WORKSPACE\\target\\test-server.pid" `
                -Value $pid

            Write-Host "Test server PID saved: $pid"
            Write-Host "Waiting for application startup..."

            $ready = $false

            for ($i = 1; $i -le 30; $i++) {

                Start-Sleep -Seconds 2

                # Check whether Java process is still alive
                $running = Get-Process -Id $pid -ErrorAction SilentlyContinue

                if (!$running) {
                    Write-Host "ERROR: Spring Boot process stopped unexpectedly."

                    if (Test-Path "$env:WORKSPACE\\target\\jenkins-spring-error.log") {
                        Get-Content "$env:WORKSPACE\\target\\jenkins-spring-error.log" -Tail 50
                    }

                    exit 1
                }

                try {

                    $response = Invoke-WebRequest `
                        -Uri "http://localhost:8081/login" `
                        -UseBasicParsing `
                        -TimeoutSec 3

                    if ($response.StatusCode -eq 200) {
                        $ready = $true
                        break
                    }

                } catch {

                    Write-Host "Waiting for application... attempt $i"
                }
            }

            if (!$ready) {

                Write-Host "ERROR: Application did not become ready."

                if (Test-Path "$env:WORKSPACE\\target\\jenkins-spring.log") {
                    Write-Host "===== APPLICATION LOG ====="
                    Get-Content "$env:WORKSPACE\\target\\jenkins-spring.log" -Tail 100
                }

                if (Test-Path "$env:WORKSPACE\\target\\jenkins-spring-error.log") {
                    Write-Host "===== APPLICATION ERROR LOG ====="
                    Get-Content "$env:WORKSPACE\\target\\jenkins-spring-error.log" -Tail 100
                }

                Stop-Process -Id $pid -Force -ErrorAction SilentlyContinue

                exit 1
            }

            Write-Host "Application detected on port $port."
            Write-Host "Login page returned HTTP 200."

            Write-Host "=========================================="
            Write-Host "TEST SERVER READY"
            Write-Host "PID: $pid"
            Write-Host "URL: http://localhost:8081/login"
            Write-Host "=========================================="

            # IMPORTANT:
            # Do NOT stop Java here.
            # Selenium needs the application to remain running.
        '''
    }
}

stage('Selenium Tests') {
    options {
        timeout(time: 4, unit: 'MINUTES')
    }

    steps {
        echo '=========================================='
        echo 'SELENIUM STAGE STARTED'
        echo '=========================================='

        bat '''
            call mvnw.cmd -Dtest=PortalSeleniumTests test
            if errorlevel 1 exit /b 1
        '''

        echo '=========================================='
        echo 'SELENIUM TESTS PASSED'
        echo '=========================================='
    }
}


post {
    always {
        echo '===== CLEANUP ====='

        powershell '''
            $pidFile = Join-Path $env:WORKSPACE "target\\test-server.pid"

            if (Test-Path $pidFile) {

                $serverPid = Get-Content $pidFile

                Write-Host "Stopping test server PID: $serverPid"

                Stop-Process `
                    -Id ([int]$serverPid) `
                    -Force `
                    -ErrorAction SilentlyContinue

                Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

                Write-Host "Test server cleanup completed."
            }
            else {
                Write-Host "No test server PID file found."
            }
        '''

        echo '===== PIPELINE FINISHED ====='
    }
}