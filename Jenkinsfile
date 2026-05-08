pipeline {
    agent any

    tools {
        maven 'Maven'
        jdk 'JDK17'
    }

    environment {
        APP_NAME        = 'java-enterprise-app'
        AWS_REGION      = 'ap-south-1'
        EB_APP_NAME     = 'my-java-app'
        S3_BUCKET       = 'elasticbeanstalk-ap-south-1-YOUR_ACCOUNT_ID'

        // Elastic Beanstalk Environment Names
        EB_ENV_DEV      = 'myapp-dev'
        EB_ENV_TEST     = 'myapp-test'
        EB_ENV_STAGING  = 'myapp-staging'
        EB_ENV_PROD     = 'myapp-prod'

        JAR_NAME        = 'enterprise-app.jar'
        VERSION         = "${BUILD_NUMBER}-${GIT_COMMIT[0..6]}"
    }

    stages {

        // ─────────────────────────────────────
        stage('📋 Checkout') {
            steps {
                echo "Branch: ${env.BRANCH_NAME}"
                echo "Build:  ${env.BUILD_NUMBER}"
                checkout scm
            }
        }

        // ─────────────────────────────────────
        stage('🔨 Build') {
            steps {
                sh 'mvn clean package -DskipTests'
                echo "✅ Build complete"
            }
            post {
                success { echo "JAR created: target/${JAR_NAME}" }
                failure { error "❌ Build failed" }
            }
        }

        // ─────────────────────────────────────
        stage('🧪 Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                }
                failure { error "❌ Tests failed — stopping pipeline" }
            }
        }

        // ─────────────────────────────────────
        stage('🚀 Deploy to DEV') {
            when {
                branch 'dev'
            }
            steps {
                deployToEB(env.EB_ENV_DEV, 'dev')
            }
        }

        // ─────────────────────────────────────
        stage('🚀 Deploy to TEST') {
            when {
                branch 'test'
            }
            steps {
                deployToEB(env.EB_ENV_TEST, 'test')
            }
        }

        // ─────────────────────────────────────
        stage('🚀 Deploy to STAGING') {
            when {
                branch 'staging'
            }
            steps {
                deployToEB(env.EB_ENV_STAGING, 'staging')
            }
        }

        // ─────────────────────────────────────
        stage('🚀 Deploy to PRODUCTION') {
            when {
                branch 'main'
            }
            steps {
                // Manual approval for PROD
                timeout(time: 10, unit: 'MINUTES') {
                    input message: '🚨 Deploy to PRODUCTION?', ok: 'Yes, Deploy!'
                }
                deployToEB(env.EB_ENV_PROD, 'production')
            }
        }
    }

    post {
        success {
            echo "✅ Pipeline completed successfully!"
            echo "Branch: ${env.BRANCH_NAME} | Build: ${env.BUILD_NUMBER}"
        }
        failure {
            echo "❌ Pipeline failed on branch: ${env.BRANCH_NAME}"
        }
        always {
            cleanWs()
        }
    }
}

// ─────────────────────────────────────────────────
// Helper function: Deploy JAR to Elastic Beanstalk
// ─────────────────────────────────────────────────
def deployToEB(String ebEnv, String envName) {
    echo "📦 Deploying to ${envName.toUpperCase()} (${ebEnv})..."

    withAWS(region: env.AWS_REGION, credentials: 'aws-credentials') {

        // Step 1: Upload JAR to S3
        def s3Key = "${env.APP_NAME}/${envName}/${env.VERSION}-${env.JAR_NAME}"
        sh """
            aws s3 cp target/${env.JAR_NAME} s3://${env.S3_BUCKET}/${s3Key}
        """
        echo "✅ JAR uploaded to S3: ${s3Key}"

        // Step 2: Create new EB Application Version
        def versionLabel = "${envName}-${env.VERSION}"
        sh """
            aws elasticbeanstalk create-application-version \
                --application-name ${env.EB_APP_NAME} \
                --version-label ${versionLabel} \
                --source-bundle S3Bucket=${env.S3_BUCKET},S3Key=${s3Key} \
                --region ${env.AWS_REGION}
        """
        echo "✅ Application version created: ${versionLabel}"

        // Step 3: Deploy to EB Environment
        sh """
            aws elasticbeanstalk update-environment \
                --environment-name ${ebEnv} \
                --version-label ${versionLabel} \
                --region ${env.AWS_REGION}
        """
        echo "✅ Deploying to ${ebEnv}..."

        // Step 4: Wait for deployment to complete
        sh """
            aws elasticbeanstalk wait environment-updated \
                --environment-names ${ebEnv} \
                --region ${env.AWS_REGION}
        """
        echo "🎉 Successfully deployed to ${envName.toUpperCase()}!"
    }
}
