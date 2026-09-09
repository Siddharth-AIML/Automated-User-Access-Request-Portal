pipeline {

    agent any

    stages {

        stage('Selenium Tests') {

            options {
                timeout(time: 5, unit: 'MINUTES')
            }

            steps {

                echo '=========================================='
                echo 'SELENIUM TEST STAGE STARTED'
                echo '=========================================='

                bat '''
                    echo ===== SELENIUM TEST START =====
                    echo.

                    echo Checking application on port 8081...
                    curl.exe -s -o NUL -w "HTTP STATUS: %%{http_code}\\n" http://localhost:8081/login

                    echo.
                    echo Running Selenium tests...
                    call mvnw.cmd -Dtest=PortalSeleniumTests test

                    if errorlevel 1 (
                        echo.
                        echo ===== SELENIUM TEST FAILED =====
                        exit /b 1
                    )

                    echo.
                    echo ===== SELENIUM TEST PASSED =====
                '''

                echo '=========================================='
                echo 'SELENIUM TEST STAGE FINISHED'
                echo '=========================================='
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