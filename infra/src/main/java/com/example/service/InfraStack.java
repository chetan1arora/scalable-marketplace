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
import software.amazon.awscdk.services.ec2.*;
import software.amazon.awscdk.services.events.targets.SqsQueue;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.rds.*;
import software.amazon.awscdk.services.sqs.Queue;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class InfraStack extends Stack {

    public InfraStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Map<String, String> customEnvVariables = new HashMap<>();

        final SqsQueue sqsQueue = SqsQueue.Builder.create(new Queue(this, "ScalableMarketplaceQueue"))
                .build();
        System.out.println(sqsQueue.getQueue().getQueueUrl());

        Table ordersTable = Table.Builder.create(this, "ScalableMarketplaceDynamoDBOrdersTable")
                        .tableName("orders")
                        .removalPolicy(RemovalPolicy.DESTROY)
                        .billingMode(BillingMode.PAY_PER_REQUEST).partitionKey(getTablePk())
                        .build();

        Table productsTable = Table.Builder.create(this, "ScalableMarketplaceDynamoDBProductsTable")
                .tableName("products")
                .removalPolicy(RemovalPolicy.DESTROY)
                .billingMode(BillingMode.PAY_PER_REQUEST).partitionKey(getTablePk())
                .build();

        final Vpc vpc = Vpc.Builder.create(this, "ScalableMarketplaceDynamoDBVPC")
                .natGateways(0)
                .build();

        final IInstanceEngine instanceEngine = DatabaseInstanceEngine.postgres(
                PostgresInstanceEngineProps.builder()
                        .version(PostgresEngineVersion.VER_13_6)
                        .build());

        final DatabaseInstance databaseInstance = DatabaseInstance.Builder.create(this, "ScalableMarketplacePostgresDBRDS")
                .vpc(vpc)
                .vpcSubnets(SubnetSelection.builder().subnetType(SubnetType.PRIVATE_ISOLATED).build())
                .instanceType(InstanceType.of(InstanceClass.BURSTABLE3, InstanceSize.NANO))
                .engine(instanceEngine)
                .instanceIdentifier("ScalableMarketplaceDynamoDBRDS")
                .removalPolicy(RemovalPolicy.DESTROY) // Comment in prod env to enable default RDS snapshot behaviour
                .build();

//        createRDSSchema(databaseInstance);

        customEnvVariables.put("SCALABLE_MARKETPLACE_QUEUE",sqsQueue.getQueue().getQueueUrl());
        customEnvVariables.put("SCALABLE_MARKETPLACE_ORDERS_TABLE",ordersTable.getTableName());
        customEnvVariables.put("SCALABLE_MARKETPLACE_PRODUCTS_TABLE",productsTable.getTableName());

        final Function inventoryHandler = Function.Builder.create(this, "ScalableMarketplaceInventoryService")
                .runtime(Runtime.JAVA_11)
                .handler("org.example.InventoryService")
                .environment(customEnvVariables)
                .memorySize(1024)
                .timeout(Duration.seconds(8))
                .functionName("inventoryService")
                .code(Code.fromAsset("../images/inventory-service.jar"))
                .build();

        final Function orderHandler = Function.Builder.create(this, "ScalableMarketplaceOrderService")
                .runtime(Runtime.JAVA_11)
                .handler("org.example.OrderService")
                .environment(customEnvVariables)
                .memorySize(1024)
                .timeout(Duration.seconds(8))
                .functionName("orderService")
                .code(Code.fromAsset("../images/order-service.jar"))
                .build();

        // Giving Lambda access to dynamoDB and RDS
        productsTable.grantReadData(inventoryHandler);
        ordersTable.grantReadWriteData(orderHandler);
        sqsQueue.getQueue().grantSendMessages(orderHandler);
        databaseInstance.grantConnect(inventoryHandler);
        databaseInstance.grantConnect(orderHandler);

        final RestApi api = RestApi.Builder.create(this, "ScalableMarketplaceRestGateway")
                .restApiName("Scalable Stack Gateway")
                .description("Testing APIs to this serverless function")
                .build();

        LambdaIntegration productListingIntegration = LambdaIntegration.Builder.create(inventoryHandler)
                .requestTemplates(Map.of("application/json","{ \"statusCode\": \"200\" }"))
                .build();

        LambdaIntegration orderCreationIntegration = LambdaIntegration.Builder.create(orderHandler)
                .requestTemplates(Map.of("application/json","{ \"statusCode\": \"200\" }"))
                .build();

        api.getRoot().addMethod("GET",productListingIntegration);
        api.getRoot().addMethod("POST",orderCreationIntegration);
    }

//    private void createRDSSchema(DatabaseInstance databaseInstance) {
//        final AmazonRDS amazonRDS = AmazonRDSClientBuilder.defaultClient();
//        amazonRDS.setEndpoint(databaseInstance.getDbInstanceEndpointAddress());
//    }

    private Attribute getTablePk() {
        return Attribute.builder().name("id").type(AttributeType.STRING).build();
    }

}
