stage('Start Test Server') {
    steps {
        echo '===== STARTING TEST SERVER ====='

        powershell '''
            Write-Host "=========================================="
            Write-Host "START TEST SERVER"
            Write-Host "=========================================="

            $jar = Join-Path $env:WORKSPACE "target\\accessportal-0.0.1-SNAPSHOT.jar"
            $port = 8081

            $java = "C:\\Program Files\\Eclipse Adoptium\\jdk-21.0.12.8-hotspot\\bin\\java.exe"

            Write-Host "JAR: $jar"
            Write-Host "Java: $java"
            Write-Host "Port: $port"

            if (!(Test-Path $jar)) {
                Write-Host "ERROR: JAR NOT FOUND"
                exit 1
            }

            if (!(Test-Path $java)) {
                Write-Host "ERROR: JAVA NOT FOUND"
                exit 1
            }

            # --------------------------------------------------
            # Kill anything already using port 8081
            # --------------------------------------------------

            Write-Host "Checking port $port..."

            $existingConnection = Get-NetTCPConnection `
                -LocalPort $port `
                -State Listen `
                -ErrorAction SilentlyContinue

            if ($existingConnection) {

                $existingServerPid = $existingConnection[0].OwningProcess

                Write-Host "Port $port is already in use."
                Write-Host "Stopping existing PID: $existingServerPid"

                taskkill /F /PID $existingServerPid 2>$null

                Start-Sleep -Seconds 2
            }

            # --------------------------------------------------
            # Prepare logs
            # --------------------------------------------------

            $stdout = Join-Path $env:WORKSPACE "target\\jenkins-spring.log"
            $stderr = Join-Path $env:WORKSPACE "target\\jenkins-spring-error.log"

            Remove-Item $stdout -Force -ErrorAction SilentlyContinue
            Remove-Item $stderr -Force -ErrorAction SilentlyContinue

            Write-Host "Starting Spring Boot application..."

            # --------------------------------------------------
            # IMPORTANT:
            # Launch Java through CMD START so Jenkins does
            # not wait for the Java process.
            # --------------------------------------------------

            $javaCommand = "`"$java`" -jar `"$jar`" > `"$stdout`" 2> `"$stderr`""

            $cmdArguments = "/c start `"SpringBootTestServer`" /b cmd /c `"$javaCommand`""

            Start-Process `
                -FilePath "cmd.exe" `
                -ArgumentList $cmdArguments `
                -WindowStyle Hidden

            Write-Host "Java launch command completed."
            Write-Host "Waiting for application startup..."

            # --------------------------------------------------
            # Wait for port 8081
            # --------------------------------------------------

            $ready = $false
            $serverPid = $null

            for ($attempt = 1; $attempt -le 30; $attempt++) {

                Start-Sleep -Seconds 2

                $connection = Get-NetTCPConnection `
                    -LocalPort $port `
                    -State Listen `
                    -ErrorAction SilentlyContinue

                if ($connection) {

                    $serverPid = $connection[0].OwningProcess

                    Write-Host "Application detected on port $port."
                    Write-Host "Spring Boot PID: $serverPid"

                    try {

                        $response = Invoke-WebRequest `
                            -Uri "http://localhost:8081/login" `
                            -UseBasicParsing `
                            -TimeoutSec 5

                        if ($response.StatusCode -eq 200) {

                            Write-Host "Login page returned HTTP 200."

                            $ready = $true
                            break
                        }

                    }
                    catch {

                        Write-Host "Port is open but application is not ready yet..."
                    }

                }
                else {

                    Write-Host "Waiting for application... attempt $attempt"
                }
            }

            if (!$ready) {

                Write-Host "=========================================="
                Write-Host "ERROR: APPLICATION DID NOT BECOME READY"
                Write-Host "=========================================="

                if (Test-Path $stdout) {
                    Write-Host "===== APPLICATION OUTPUT ====="
                    Get-Content $stdout -Tail 100
                }

                if (Test-Path $stderr) {
                    Write-Host "===== APPLICATION ERROR ====="
                    Get-Content $stderr -Tail 100
                }

                exit 1
            }

            # --------------------------------------------------
            # Save actual server PID
            # --------------------------------------------------

            $pidFile = Join-Path $env:WORKSPACE "target\\test-server.pid"

            Set-Content `
                -Path $pidFile `
                -Value $serverPid

            Write-Host "Test server PID saved: $serverPid"

            Write-Host "=========================================="
            Write-Host "TEST SERVER READY"
            Write-Host "PID: $serverPid"
            Write-Host "URL: http://localhost:8081/login"
            Write-Host "=========================================="

            Write-Host "Returning control to Jenkins..."
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
            echo ===== BEFORE SELENIUM =====
            call mvnw.cmd -Dtest=PortalSeleniumTests test

            if errorlevel 1 (
                echo ===== SELENIUM TESTS FAILED =====
                exit /b 1
            )

            echo ===== AFTER SELENIUM =====
        '''

        echo '=========================================='
        echo 'SELENIUM TESTS PASSED'
        echo '=========================================='
    }
}


post {
    always {

        echo '=========================================='
        echo 'CLEANUP TEST SERVER'
        echo '=========================================='

        powershell '''
            $pidFile = Join-Path $env:WORKSPACE "target\\test-server.pid"

            if (Test-Path $pidFile) {

                $serverPid = [int](Get-Content $pidFile)

                Write-Host "Stopping Spring Boot PID: $serverPid"

                taskkill /F /PID $serverPid 2>$null

                Remove-Item $pidFile -Force -ErrorAction SilentlyContinue

                Write-Host "Test server stopped."
            }
            else {

                Write-Host "No test server PID file found."
            }

            # Safety check
            $connection = Get-NetTCPConnection `
                -LocalPort 8081 `
                -State Listen `
                -ErrorAction SilentlyContinue

            if ($connection) {

                $remainingPid = $connection[0].OwningProcess

                Write-Host "Port 8081 still occupied by PID: $remainingPid"
                Write-Host "Stopping remaining process..."

                taskkill /F /PID $remainingPid 2>$null
            }
            else {

                Write-Host "Port 8081 is free."
            }
        '''

        echo '===== PIPELINE FINISHED ====='
    }
}