pipeline {
    agent any

    environment {
        APP_NAME    = 'java-enterprise-app'
        AWS_REGION  = 'ap-south-1'
        EB_APP_NAME = 'my-java-app'
        S3_BUCKET   = 'elasticbeanstalk-ap-south-1-678804053714'
        JAR_NAME    = 'enterprise-app.jar'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
                echo "Branch: ${env.BRANCH_NAME}"
            }
        }

        stage('Set Environment') {
            steps {
                script {
                    if (env.BRANCH_NAME == 'main') {
                        env.DEPLOY_ENV  = 'production'
                        env.EB_ENV_NAME = 'myapp-prod'
                    } else if (env.BRANCH_NAME == 'staging') {
                        env.DEPLOY_ENV  = 'staging'
                        env.EB_ENV_NAME = 'myapp-staging'
                    } else if (env.BRANCH_NAME == 'test') {
                        env.DEPLOY_ENV  = 'test'
                        env.EB_ENV_NAME = 'myapp-test'
                    } else {
                        env.DEPLOY_ENV  = 'dev'
                        env.EB_ENV_NAME = 'myapp-dev'
                    }
                    echo "Deploying to: ${env.DEPLOY_ENV.toUpperCase()}"
                }
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean package -DskipTests'
                echo "Build complete"
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Upload to S3') {
            steps {
                script {
                    def s3Key = "${env.APP_NAME}/${env.DEPLOY_ENV}/build-${env.BUILD_NUMBER}-${env.JAR_NAME}"
                    env.S3_KEY = s3Key
                    withAWS(region: env.AWS_REGION, credentials: 'aws-credentials') {
                        sh "aws s3 cp target/${env.JAR_NAME} s3://${env.S3_BUCKET}/${s3Key}"
                    }
                    echo "Uploaded: ${s3Key}"
                }
            }
        }

        stage('Deploy') {
            when { not { branch 'main' } }
            steps { script { deployToEB() } }
        }

        stage('Production Approval') {
            when { branch 'main' }
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    input message: "Deploy to PRODUCTION?", ok: 'Yes, Deploy!'
                }
            }
        }

        stage('Deploy to PROD') {
            when { branch 'main' }
            steps { script { deployToEB() } }
        }
    }

    post {
        success { echo "Successfully deployed to ${env.DEPLOY_ENV?.toUpperCase()}" }
        failure { echo "Failed on branch: ${env.BRANCH_NAME}" }
        always  { cleanWs() }
    }
}

def deployToEB() {
    def versionLabel = "${env.DEPLOY_ENV}-build-${env.BUILD_NUMBER}"
    withAWS(region: env.AWS_REGION, credentials: 'aws-credentials') {
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
        echo "Deployed to ${env.DEPLOY_ENV.toUpperCase()}!"
    }
}