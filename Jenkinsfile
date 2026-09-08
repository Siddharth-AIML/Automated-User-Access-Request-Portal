pipeline {

    agent any

    options {
        timestamps()
    }

    stages {

        stage('Checkout') {
            steps {
                echo '===== CHECKOUT ====='
                checkout scm
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


        stage('Selenium Tests') {

            options {
                timeout(time: 7, unit: 'MINUTES')
            }

            steps {

                echo '=========================================='
                echo 'SELENIUM STAGE STARTED'
                echo '=========================================='

                bat '''
                    @echo off

                    echo ==========================================
                    echo STARTING SPRING BOOT FOR SELENIUM
                    echo ==========================================

                    set "JAR=%WORKSPACE%\\target\\accessportal-0.0.1-SNAPSHOT.jar"

                    echo JAR: %JAR%

                    if not exist "%JAR%" (
                        echo ERROR: JAR NOT FOUND
                        exit /b 1
                    )


                    echo.
                    echo Checking port 8081...

                    for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do (
                        echo Killing existing PID %%P
                        taskkill /F /PID %%P >nul 2>&1
                    )


                    echo.
                    echo Starting Spring Boot application...

                    start "" /B java -jar "%JAR%" > "%WORKSPACE%\\springboot-output.log" 2> "%WORKSPACE%\\springboot-error.log"

                    echo Spring Boot launch command completed.


                    echo.
                    echo Waiting for Spring Boot...

                    set "READY="

                    for /L %%I in (1,1,30) do (

                        powershell -NoProfile -Command "try { $r=Invoke-WebRequest -Uri 'http://127.0.0.1:8081/login' -UseBasicParsing -TimeoutSec 2; if($r.StatusCode -eq 200){exit 0}else{exit 1} } catch { exit 1 }"

                        if not errorlevel 1 (
                            set "READY=YES"
                            echo.
                            echo ==========================================
                            echo APPLICATION READY
                            echo HTTP 200 FROM /login
                            echo ==========================================
                            goto SERVER_READY
                        )

                        echo Application not ready - attempt %%I/30
                        timeout /t 2 /nobreak >nul
                    )


                    echo.
                    echo ==========================================
                    echo APPLICATION FAILED TO START
                    echo ==========================================

                    echo ----- SPRING BOOT OUTPUT -----
                    if exist "%WORKSPACE%\\springboot-output.log" (
                        type "%WORKSPACE%\\springboot-output.log"
                    )

                    echo ----- SPRING BOOT ERROR -----
                    if exist "%WORKSPACE%\\springboot-error.log" (
                        type "%WORKSPACE%\\springboot-error.log"
                    )

                    goto CLEANUP_FAILED


                    :SERVER_READY

                    echo.
                    echo ==========================================
                    echo RUNNING SELENIUM TESTS
                    echo ==========================================
                    echo.

                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    set "SELENIUM_RESULT=%ERRORLEVEL%"

                    echo.
                    echo ==========================================
                    echo SELENIUM EXIT CODE: %SELENIUM_RESULT%
                    echo ==========================================


                    :CLEANUP

                    echo.
                    echo ==========================================
                    echo STOPPING TEST SERVER
                    echo ==========================================

                    for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do (
                        echo Stopping PID %%P
                        taskkill /F /PID %%P >nul 2>&1
                    )

                    timeout /t 2 /nobreak >nul

                    echo Test server stopped.


                    if defined SELENIUM_RESULT (
                        exit /b %SELENIUM_RESULT%
                    )

                    exit /b 1


                    :CLEANUP_FAILED

                    echo.
                    echo Cleaning up failed application...

                    for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do (
                        taskkill /F /PID %%P >nul 2>&1
                    )

                    exit /b 1
                '''

                echo '=========================================='
                echo 'SELENIUM STAGE FINISHED'
                echo '=========================================='
            }
        }
    }


    post {

        always {

            echo '===== FINAL CLEANUP ====='

            bat '''
                @echo off

                echo Checking for remaining process on port 8081...

                for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":8081" ^| findstr "LISTENING"') do (
                    echo Killing remaining PID %%P
                    taskkill /F /PID %%P >nul 2>&1
                )

                echo Final cleanup complete.
            '''

            archiveArtifacts artifacts: 'springboot-output.log,springboot-error.log,target/surefire-reports/**', allowEmptyArchive: true
        }
    }
}