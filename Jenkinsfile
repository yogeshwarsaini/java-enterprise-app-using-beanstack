// ============================================
// JENKINS BRANCH - Sirf Parameters hain yahan
// ============================================

return [
    // AWS Config
    AWS_REGION  : 'ap-south-1',
    AWS_CREDS   : 'aws-credentials',
    EB_APP_NAME : 'my-java-app',
    S3_BUCKET   : 'elasticbeanstalk-ap-south-1-678804053714',

    // App Config
    APP_NAME : 'java-enterprise-app',
    JAR_NAME : 'enterprise-app.jar',

    // Branch → Elastic Beanstalk Environment Mapping
    BRANCH_ENV_MAP : [
        'main'    : 'Java-app-prod-env',
        'staging' : 'Java-app-staging-env',
        'test'    : 'Java-app-test-env',
        'dev'     : 'Java-app-dev-env'
    ],

    // Branch → Deploy Environment Name Mapping
    BRANCH_DEPLOY_MAP : [
        'main'    : 'production',
        'staging' : 'staging',
        'test'    : 'test',
        'dev'     : 'dev'
    ]
]
