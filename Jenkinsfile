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
        stage('Detect Changes') {
            steps {
                script {
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
                // 각 파일 경로의 앞뒤 공백을 제거하고 비교 (핵심 수정)
                def trimmedFile = it.trim()
                echo "File: ${trimmedFile}"

                if (trimmedFile.startsWith('BE/')) {
                    env.BUILD_SPRING = 'true'
                }
                if (trimmedFile.startsWith('AI_server/')) {
                    env.BUILD_FASTAPI = 'true'
                }
            }
            echo "==========================="
                    
                    echo "Build Spring App: ${env.BUILD_SPRING}"
                    echo "Build FastAPI App: ${env.BUILD_FASTAPI}"
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
        // STAGE 3: 프로덕션 배포 (main 브랜치 전용)
        // =================================================================
        stage('Deploy to Production') {
            when {
                // 'main' 브랜치이고, 빌드할 서비스가 하나라도 있을 때 실행
                branch 'inf/S13P21A602-170'
                anyOf {
                    expression { env.BUILD_SPRING == 'true' }
                    expression { env.BUILD_FASTAPI == 'true' }
                }
            }
            stages {
                // main 브랜치 내의 하위 단계들
                stage('Rebuild Target Image') {
                    steps {
                        echo "Rebuilding image for production deployment."
                        // Blue-Green 스왑 후 Green이 될 대상을 미리 빌드
                        sh 'sh ./scripts/rebuild_green.sh' 
                    }
                }
                stage('Approval') {
                    steps {
                        // 운영 배포 전 최종 승인 대기
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
            // 성공 알림 (Slack, 이메일 등)
        }
        failure {
            echo 'Pipeline failed.'
            // 실패 알림
        }
    }
}
