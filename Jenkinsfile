pipeline {

    agent any

    stages {

        stage('Selenium Tests') {

            options {
                timeout(time: 5, unit: 'MINUTES')
            }

            steps {

                echo '=========================================='
                echo 'SELENIUM TESTS STARTED'
                echo '=========================================='

                bat '''
                    echo ===== SELENIUM COMMAND START =====
                    call mvnw.cmd -Dtest=PortalSeleniumTests test
                    echo ===== SELENIUM COMMAND END =====
                '''

                echo '=========================================='
                echo 'SELENIUM TESTS FINISHED'
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