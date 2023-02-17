package com.example.service;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.apigateway.LambdaIntegration;
import software.amazon.awscdk.services.apigateway.RestApi;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class InfraStack extends Stack {
    public InfraStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Map<String, String> customEnvVariables = new HashMap<>();

        final SqsQueue sqsQueue = SqsQueue.Builder.create(new Queue(this, "ScalableMarketplaceQueue"))
                .build();
        System.out.println(sqsQueue.getQueue().getQueueUrl());

        Table dynamoDBTable = Table.Builder.create(this, "ScalableMarketplaceDynamoDBTable")
                        .tableName("orders")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .billingMode(BillingMode.PAY_PER_REQUEST).partitionKey(getTaxiTablePk())
                        .build();

        customEnvVariables.put("SCALABLE_MARKETPLACE_QUEUE",sqsQueue.getQueue().getQueueUrl());
        customEnvVariables.put("SCALABLE_MARKETPLACE_ORDERS_TABLE",dynamoDBTable.getTableName());

        final Function lambdaHandler = Function.Builder.create(this, "ScalableMarketplaceLambda")
                .runtime(Runtime.JAVA_11)
                .handler("org.example.ScalableLambda")
                .environment(customEnvVariables)
                .memorySize(1024)
                .timeout(Duration.seconds(8))
                .functionName("scalableLambda")
                .code(Code.fromAsset("../images/scalableLambda.jar"))
                .build();

        // Giving Lambda access to dynamoDB
        dynamoDBTable.grantReadWriteData(lambdaHandler);
        sqsQueue.getQueue().grantSendMessages(lambdaHandler);

        final RestApi api = RestApi.Builder.create(this, "ScalableMarketplaceRestGateway")
                .restApiName("Scalable Stack Gateway")
                .description("Testing APIs to this serverless function")
                .build();

        LambdaIntegration lambdaIntegration = LambdaIntegration.Builder.create(lambdaHandler)
                .requestTemplates(Map.of("application/json","{ \"statusCode\": \"200\" }"))
                .build();

        api.getRoot().addMethod("POST",lambdaIntegration);
    }

    private Attribute getTaxiTablePk() {
        return Attribute.builder().name("id").type(AttributeType.STRING).build();
    }

}
