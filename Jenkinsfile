// ============================================
// DEV / TEST / STAGING / MAIN BRANCHES
// Sirf Pipeline Logic hai yahan
// Koi parameters nahi hain is file mein
// Config jenkins branch se load hoti hai
// ============================================

def config = [:]

pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk   'JDK17'
    }

    stages {

        // ─────────────────────────────────────────
        // Step 1: Jenkins branch se config load karo
        // ─────────────────────────────────────────
        stage('Load Config') {
            steps {
                script {
                    // jenkins branch checkout karo
                    checkout([
                        $class           : 'GitSCM',
                        branches         : [[name: 'refs/heads/jenkins']],
                        userRemoteConfigs : scm.userRemoteConfigs
                    ])

                    // Jenkinsfile (parameters) load karo
                    config = evaluate(readFile('Jenkinsfile'))

                    // Apni actual branch wapas checkout karo
                    checkout scm

                    echo "✅ Config loaded from jenkins branch"
                    echo "📌 Branch     : ${env.BRANCH_NAME}"
                    echo "🌍 Deploy Env : ${config.BRANCH_DEPLOY_MAP[env.BRANCH_NAME] ?: 'dev'}"
                    echo "☁️  EB Env     : ${config.BRANCH_ENV_MAP[env.BRANCH_NAME] ?: 'Java-app-dev-env'}"
                }
            }
        }

        // ─────────────────────────────────────────
        // Step 2: Environment variables set karo
        // ─────────────────────────────────────────
        stage('Set Environment') {
            steps {
                script {
                    def branch = env.BRANCH_NAME

                    env.APP_NAME    = config.APP_NAME
                    env.JAR_NAME    = config.JAR_NAME
                    env.AWS_REGION  = config.AWS_REGION
                    env.AWS_CREDS   = config.AWS_CREDS
                    env.EB_APP_NAME = config.EB_APP_NAME
                    env.S3_BUCKET   = config.S3_BUCKET
                    env.EB_ENV_NAME = config.BRANCH_ENV_MAP[branch]    ?: 'Java-app-dev-env'
                    env.DEPLOY_ENV  = config.BRANCH_DEPLOY_MAP[branch] ?: 'dev'

                    echo "🚀 Deploying to: ${env.DEPLOY_ENV.toUpperCase()}"
                }
            }
        }

        // ─────────────────────────────────────────
        // Step 3: Build
        // ─────────────────────────────────────────
        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
                echo "✅ Build complete"
            }
        }

        // ─────────────────────────────────────────
        // Step 4: Test
        // ─────────────────────────────────────────
        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        // ─────────────────────────────────────────
        // Step 5: JAR S3 pe Upload karo
        // ─────────────────────────────────────────
        stage('Upload to S3') {
            steps {
                script {
                    def s3Key = "${env.APP_NAME}/${env.DEPLOY_ENV}/build-${env.BUILD_NUMBER}-${env.JAR_NAME}"
                    env.S3_KEY = s3Key

                    withAWS(region: env.AWS_REGION, credentials: env.AWS_CREDS) {
                        sh "aws s3 cp target/${env.JAR_NAME} s3://${env.S3_BUCKET}/${s3Key}"
                    }
                    echo "✅ Uploaded to S3: ${s3Key}"
                }
            }
        }

        // ─────────────────────────────────────────
        // Step 6: Dev / Test / Staging Deploy
        // ─────────────────────────────────────────
        stage('Deploy') {
            when { not { branch 'main' } }
            steps {
                script { deployToEB() }
            }
        }

        // ─────────────────────────────────────────
        // Step 7: Production - Manual Approval
        // ─────────────────────────────────────────
        stage('Production Approval') {
            when { branch 'main' }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    input message: "🚨 Deploy to PRODUCTION?", ok: 'Yes, Deploy!'
                }
            }
        }

        // ─────────────────────────────────────────
        // Step 8: Production Deploy
        // ─────────────────────────────────────────
        stage('Deploy to PROD') {
            when { branch 'main' }
            steps {
                script { deployToEB() }
            }
        }
    }

    post {
        success { echo "✅ Deployed to ${env.DEPLOY_ENV?.toUpperCase()} successfully!" }
        failure { echo "❌ Failed on branch: ${env.BRANCH_NAME}" }
        always  { cleanWs() }
    }
}

// ─────────────────────────────────────────────
// Deploy function - Elastic Beanstalk pe deploy
// ─────────────────────────────────────────────
def deployToEB() {
    def versionLabel = "${env.DEPLOY_ENV}-build-${env.BUILD_NUMBER}"

    withAWS(region: env.AWS_REGION, credentials: env.AWS_CREDS) {
        sh """
            aws elasticbeanstalk create-application-version \
                --application-name ${env.EB_APP_NAME} \
                --version-label ${versionLabel} \
                --source-bundle S3Bucket=${env.S3_BUCKET},S3Key=${env.S3_KEY} \
                --region ${env.AWS_REGION}

            aws elasticbeanstalk update-environment \
                --environment-name ${env.EB_ENV_NAME} \
                --version-label ${versionLabel} \
                --region ${env.AWS_REGION}

            aws elasticbeanstalk wait environment-updated \
                --environment-names ${env.EB_ENV_NAME} \
                --region ${env.AWS_REGION}
        """
        echo "🎉 Deployed to ${env.DEPLOY_ENV.toUpperCase()}!"
    }
}
