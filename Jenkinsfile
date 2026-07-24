pipeline {
    agent any
    
    tools {
        maven 'Maven-3.8'
        jdk 'JDK-11'
    }
    
    environment {
        DOCKER_REGISTRY = 'vasupa32'
        IMAGE_NAME = 'nammastocks'
        DOCKER_CREDENTIALS_ID = 'docker-hub-credentials'
        WORKER_IP = '172.31.39.240'
        REPO_URL = 'https://github.com/VasundharaPatil/NammaStocks.git'
    }
    
    stages {
        stage('Checkout') {
            steps {
                git branch: 'main', url: env.REPO_URL
                script {
                    currentBuild.displayName = "NammaStocks Build #${BUILD_NUMBER}"
                }
            }
        }
        
        stage('Build Application') {
            steps {
                sh 'mvn clean package -DskipTests'
                sh 'ls -l target/*.jar'
            }
        }
        
        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                        scp target/*.jar ubuntu@${WORKER_IP}:/tmp/nammastocks.jar
                        scp Dockerfile ubuntu@${WORKER_IP}:/tmp/Dockerfile
                        ssh ubuntu@${WORKER_IP} "cd /tmp && docker build -t ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} -f Dockerfile ."
                    """
                }
            }
        }
        
        stage('Push to Docker Hub') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: DOCKER_CREDENTIALS_ID, 
                                                      usernameVariable: 'DOCKER_USER', 
                                                      passwordVariable: 'DOCKER_PASS')]) {
                        sh """
                            ssh ubuntu@${WORKER_IP} "docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}"
                            ssh ubuntu@${WORKER_IP} "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}"
                            ssh ubuntu@${WORKER_IP} "docker tag ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest"
                            ssh ubuntu@${WORKER_IP} "docker push ${DOCKER_REGISTRY}/${IMAGE_NAME}:latest"
                        """
                    }
                }
            }
        }
        
        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }
    }
    
    post {
        success {
            echo "✅ NammaStocks build successful! Image: ${DOCKER_REGISTRY}/${IMAGE_NAME}:${BUILD_NUMBER}"
        }
        failure {
            echo "❌ NammaStocks build failed!"
        }
    }
}