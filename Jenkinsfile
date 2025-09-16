pipeline {
    agent any

    environment {
        // 빌드할 서비스 목록을 관리하는 변수
        BUILD_SPRING = 'false'
        BUILD_FASTAPI = 'false'
    }

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
                            echo "Build Spring App: ${buildSpring}"
                        }
                        if (trimmedFile.startsWith('AI_server/')) {
                            buildFastApi = true
                            echo "Build FastAPI App: ${buildFastApi}"
                        }
                        if (trimmedFile.startsWith('Nginx/') || trimmedFile.startsWith('MYSQL/') || trimmedFile == 'docker-compose.yml') {
                            restartInfrastructure = true
                            echo "Infrastructure restart required due to changes in: ${trimmedFile}"
                            echo "Restart Infrastructure: ${restartInfrastructure}"
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
                    echo "servicesToRebuild: ${servicesToRebuild}"

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
                // 'develop' 브랜치이고, 빌드할 서비스가 하나라도 있을 때 실행
                branch 'develop'
                anyOf {
                    expression { env.BUILD_SPRING == 'true' }
                    expression { env.BUILD_FASTAPI == 'true' }
                }
            }
            steps {
                script {
                    echo "Updating Green (test) environment from 'develop' branch."
                    // 이 스크립트는 내부적으로 docker-compose up --build -d <서비스명>을 실행
                    sh 'sh ./scripts/rebuild_green.sh'
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
                script {
                    if (env.RESTART_INFRA == 'true') {
                        // 인프라 변경이 최우선. Blue/Green 환경 전체를 최신 설정으로 재구성
                        echo "[PROD] Infrastructure change detected. Restarting all services before swapping."
                        sh 'sh ./scripts/restart_all.sh'
                    } else {
                        // 애플리케이션 코드만 변경된 경우, 스왑 후 Green이 될 서비스만 재빌드
                        echo "[PROD] Application code change detected. Rebuilding target image."
                        sh "sh ./scripts/rebuild_services.sh ${env.SERVICES_TO_REBUILD}"
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
