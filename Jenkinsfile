// Pipeline de Jenkins (HU-27, RF-25): prueba, construye las imágenes y despliega con Docker Compose.

pipeline {
    agent any

    options {
        timestamps()
        timeout(time: 60, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        disableConcurrentBuilds()
    }

    environment {
        MAVEN_OPTS = '-Xmx1g'
    }

    stages {
        stage('Backend: pruebas y cobertura') {
            steps {
                dir('backend') {
                    // verify: pruebas unitarias, pruebas con MySQL real
                    sh 'mvn -B verify'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/**/target/surefire-reports/*.xml'
                    archiveArtifacts allowEmptyArchive: true, artifacts: 'backend/**/target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Frontend: tipos y compilación') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run build'
                }
            }
        }

        stage('Imágenes Docker') {
            steps {
                withCredentials([file(credentialsId: 'orvex-env', variable: 'ENV_FILE')]) {
                    sh 'cp "$ENV_FILE" .env'
                    sh 'docker compose config -q'
                    sh 'docker compose build'
                }
            }
        }

        stage('Aprobar despliegue') {
            when { branch 'main' }
            steps {
                timeout(time: 30, unit: 'MINUTES') {
                    input message: '¿Desplegar esta versión de Orvex?', ok: 'Desplegar'
                }
            }
        }

        stage('Desplegar') {
            when { branch 'main' }
            steps {
                sh 'docker compose up -d --remove-orphans'
                // Prueba de humo: el gateway responde y el catálogo devuelve productos.
                sh '''
                    for i in $(seq 1 40); do
                      if curl -fs "http://localhost:${API_PORT:-8080}/api/productos/buscar?tamano=1" > /dev/null; then
                        echo "Orvex responde"; exit 0
                      fi
                      sleep 5
                    done
                    echo "Orvex no respondió después del despliegue"; docker compose ps; exit 1
                '''
            }
        }
    }

    post {
        failure {
            echo 'El pipeline falló: revisar las pruebas o los logs de "docker compose logs".'
        }
        always {
            sh 'rm -f .env || true'
        }
    }
}
