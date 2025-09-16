pipeline {
    agent any

    stages {        
        // =================================================================
        // STAGE 1: 변경 사항 감지 (모든 브랜치에서 실행)
        // =================================================================
        stage('Analyze Changes') {
            steps {
                script {
                    // 빌드 플래그 초기화
                    def buildSpring = false
                    def buildFastApi = false
                    // 인프라 전체를 재시작할지 결정하는 플래그
                    def restartInfrastructure = false
                    echo "Checking for changes in branch: ${env.BRANCH_NAME}"
            
                    def changedFilesScript = ""
                    // env.CHANGE_TARGET 변수가 존재하고 비어있지 않은지 Groovy 레벨에서 먼저 확인
                    if (env.CHANGE_TARGET != null && !env.CHANGE_TARGET.isEmpty()) {
                        echo "Comparing with previous build target: ${env.CHANGE_TARGET}"
                        changedFilesScript = "git diff --name-only ${env.CHANGE_TARGET} HEAD"
                    } else {
                        echo "First build for this branch. Listing all files."
                        changedFilesScript = "git ls-files"
                    }

                    def changedFiles = sh(script: changedFilesScript, returnStdout: true).trim().split('\n')
            
                    echo "=== List of changed files ==="
                    changedFiles.each {
                        def trimmedFile = it.trim()
                        echo "File: ${trimmedFile}"

                        if (trimmedFile.startsWith('BE/')) {
                            buildSpring = true
                        }
                        if (trimmedFile.startsWith('AI_server/')) {
                            buildFastApi = true
                        }
                        if (trimmedFile.startsWith('Nginx/') || trimmedFile.startsWith('MYSQL/') || trimmedFile == 'docker-compose.yml') {
                            restartInfrastructure = true
                        }
                    }
                    echo "==========================="

                    def servicesToRebuild = []
                    if (buildSpring) {
                        servicesToRebuild.add('spring-app-green')
                    }
                    if (buildFastApi) {
                        servicesToRebuild.add('fastapi-app')
                    }
            
                    // 다음 스테이지에서 사용할 수 있도록 env 변수에 최종 할당
                    env.SERVICES_TO_REBUILD = servicesToRebuild.join(' ')
                    env.RESTART_INFRA = restartInfrastructure.toString()

                    echo "Services to rebuild: ${env.SERVICES_TO_REBUILD}"
                    echo "Restart infrastructure needed: ${env.RESTART_INFRA}"
                }
            }
        }

        // =================================================================
        // STAGE 2: Green 환경 업데이트 (develop 브랜치 전용)
        // =================================================================
        stage('Update Green Environment') {
            when {
                branch 'develop'
                // 빌드할 앱 코드가 있거나, 인프라 변경이 있을 때 실행
                anyOf {
                    expression { env.SERVICES_TO_REBUILD != null && !env.SERVICES_TO_REBUILD.isEmpty() }
                    expression { env.RESTART_INFRA == 'true' }
                }
            }
        
            steps {
                withCredentials([
                    string(credentialsId: 'db-root-password', variable: 'MYSQL_ROOT_PASSWORD'),
                    string(credentialsId: 'db-name', variable: 'MYSQL_DATABASE'),
                    string(credentialsId: 'db-user', variable: 'MYSQL_USER'),
                    string(credentialsId: 'db-password', variable: 'MYSQL_PASSWORD'),
                    string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                    string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY'),
                    string(credentialsId: 'aws-region', variable: 'AWS_REGION'),
                    string(credentialsId: 'aws-s3-bucket', variable: 'SPRING_CLOUD_AWS_S3_BUCKET'),
                    string(credentialsId: 'aws-s3-cdn-url', variable: 'SPRING_CLOUD_AWS_S3_CDN_URL'),
                    string(credentialsId: 'jwt-secret', variable: 'SPRING_JWT_SECRET')
                ]){
                script {
                    sh """
                        echo "MYSQL_ROOT_PASSWORD=${env.MYSQL_ROOT_PASSWORD}" > .env
                        echo "MYSQL_DATABASE=${env.MYSQL_DATABASE}" >> .env
                        echo "MYSQL_USER=${env.MYSQL_USER}" >> .env
                        echo "MYSQL_PASSWORD=${env.MYSQL_PASSWORD}" >> .env
                        echo "AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY_ID}" >> .env
                        echo "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_ACCESS_KEY}" >> .env
                        echo "AWS_REGION=${env.AWS_REGION}" >> .env
                        echo "AWS_S3_BUCKET=${env.SPRING_CLOUD_AWS_S3_BUCKET}" >> .env
                        echo "AWS_S3_CDN_URL=${env.SPRING_CLOUD_AWS_S3_CDN_URL}" >> .env
                        echo "SPRING_JWT_SECRET='${env.SPRING_JWT_SECRET}'" >> .env
                    """
                    if (env.RESTART_INFRA == 'true') {
                        // 인프라 변경이 최우선. 전체 재시작
                        echo "Infrastructure change detected. Restarting all services."
                        sh 'sh ./scripts/restart_all.sh'
                    } else {
                        // 애플리케이션 코드만 변경된 경우
                        echo "Application code change detected. Rebuilding specific services."
                        sh "sh ./scripts/rebuild_green.sh ${env.SERVICES_TO_REBUILD}"
                    }
                    sh 'cp build/docs/openapi.json /var/www/html/api-docs/'
                }
                }
            }
        }
        // =================================================================
        // STAGE 3: 프로덕션 배포 (master 브랜치 전용)
        // =================================================================
        stage('Deploy to Production') {
            when {
                branch 'inf/S13P21A602-170'
                // 빌드할 앱 코드가 있거나, 인프라 변경이 있을 때 실행
                anyOf {
                    expression { env.SERVICES_TO_REBUILD != null && !env.SERVICES_TO_REBUILD.isEmpty() }
                    expression { env.RESTART_INFRA == 'true' }
                }
            }

            stages {
                // 하위 스테이지들
                stage('Prepare for Deployment') {
                    steps {
                        
                        withCredentials([

                        string(credentialsId: 'db-root-password', variable: 'MYSQL_ROOT_PASSWORD'),
                        string(credentialsId: 'db-name', variable: 'MYSQL_DATABASE'),
                        string(credentialsId: 'db-user', variable: 'MYSQL_USER'),
                        string(credentialsId: 'db-password', variable: 'MYSQL_PASSWORD'),
                        string(credentialsId: 'aws-access-key-id', variable: 'AWS_ACCESS_KEY_ID'),
                        string(credentialsId: 'aws-secret-access-key', variable: 'AWS_SECRET_ACCESS_KEY'),
                        string(credentialsId: 'aws-region', variable: 'AWS_REGION'),
                        string(credentialsId: 'aws-s3-bucket', variable: 'SPRING_CLOUD_AWS_S3_BUCKET'),
                        string(credentialsId: 'aws-s3-cdn-url', variable: 'SPRING_CLOUD_AWS_S3_CDN_URL'),
                        string(credentialsId: 'jwt-secret', variable: 'SPRING_JWT_SECRET')
                        ]) {
                            script {
                                sh """
                                    echo "MYSQL_ROOT_PASSWORD=${env.MYSQL_ROOT_PASSWORD}" > .env
                                    echo "MYSQL_DATABASE=${env.MYSQL_DATABASE}" >> .env
                                    echo "MYSQL_USER=${env.MYSQL_USER}" >> .env
                                    echo "MYSQL_PASSWORD=${env.MYSQL_PASSWORD}" >> .env
                                    echo "AWS_ACCESS_KEY_ID=${env.AWS_ACCESS_KEY_ID}" >> .env
                                    echo "AWS_SECRET_ACCESS_KEY=${env.AWS_SECRET_ACCESS_KEY}" >> .env
                                    echo "AWS_REGION=${env.AWS_REGION}" >> .env
                                    echo "AWS_S3_BUCKET=${env.SPRING_CLOUD_AWS_S3_BUCKET}" >> .env
                                    echo "AWS_S3_CDN_URL=${env.SPRING_CLOUD_AWS_S3_CDN_URL}" >> .env
                                    echo "SPRING_JWT_SECRET='${env.SPRING_JWT_SECRET}'" >> .env
                                """
                                if (env.RESTART_INFRA == 'true') {
                                    // 인프라 변경이 최우선. Blue/Green 환경 전체를 최신 설정으로 재구성
                                    echo "[PROD] Infrastructure change detected. Restarting all services before swapping."
                                    sh 'sh ./scripts/restart_all.sh'
                                } else {
                                    // 애플리케이션 코드만 변경된 경우, 스왑 후 Green이 될 서비스만 재빌드
                                    echo "[PROD] Application code change detected. Rebuilding target image."
                                    sh "sh ./scripts/rebuild_green.sh ${env.SERVICES_TO_REBUILD}"
                                }
                                sh 'cp build/docs/openapi.json /var/www/html/api-docs/'
                            }
                        }
                    }
                }
        
                stage('Approval') {
                    steps {
                        timeout(time: 30, unit: 'MINUTES') {
                            input message: "Ready to swap Blue-Green for production? This will make the latest changes live."
                        }
                    }
                }
        
                stage('Swap Blue-Green') {
                    steps {
                        echo "Swapping Blue-Green roles..."
                        sh 'sh ./scripts/swap_blue_green.sh'
                    }
                }
            }
        }
    }
    // =================================================================
    // POST ACTIONS: 파이프라인 실행 후 작업
    // =================================================================
    post {
        always {
            echo "Cleaning up workspace..."
            deleteDir()
        }
        success {
            echo 'Pipeline finished successfully.'
            // 성공 알림
        }
        failure {
            echo 'Pipeline failed.'
            // 실패 알림
        }
    }
}
