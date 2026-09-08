pipeline {
    agent any

    stages {

        stage('Test PowerShell Return') {
            steps {

                echo '===== BEFORE POWERSHELL ====='

                powershell '''
                    Write-Host "===== POWERSHELL START ====="
                    Write-Host "Hello from Jenkins PowerShell"
                    Write-Host "===== POWERSHELL END ====="
                '''

                echo '===== AFTER POWERSHELL ====='
            }
        }

        stage('Test Next Stage') {
            steps {
                echo '===== NEXT STAGE REACHED ====='
            }
        }
    }
}