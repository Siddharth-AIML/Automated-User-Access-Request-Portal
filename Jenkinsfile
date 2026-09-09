pipeline {

    agent any

    stages {

        stage('Checkout') {
            steps {
                echo '=========================================='
                echo 'CHECKOUT START'
                echo '=========================================='

                checkout scm

                echo '=========================================='
                echo 'CHECKOUT SUCCESS'
                echo '=========================================='
            }
        }

        stage('Test BAT Commands') {
            steps {

                echo '=========================================='
                echo 'BAT TEST START'
                echo '=========================================='

                bat '''
                    echo ===== CMD START =====

                    echo Current Directory:
                    cd

                    echo.
                    echo Workspace:
                    echo %WORKSPACE%

                    echo.
                    echo Java Version:
                    java -version

                    echo.
                    echo Maven Wrapper Version:
                    call mvnw.cmd -version

                    echo.
                    echo Listing Workspace:
                    dir

                    echo ===== CMD TEST FINISHED =====
                '''

                echo '=========================================='
                echo 'BAT TEST SUCCESS'
                echo '=========================================='
            }
        }

        stage('Test Maven Build') {
            steps {

                echo '=========================================='
                echo 'MAVEN BUILD TEST START'
                echo '=========================================='

                bat '''
                    echo ===== MAVEN BUILD START =====

                    call mvnw.cmd clean package -DskipTests

                    echo ===== MAVEN BUILD FINISHED =====
                '''

                echo '=========================================='
                echo 'MAVEN BUILD TEST SUCCESS'
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