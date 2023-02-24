package org.example;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.models.Order;

import java.util.HashMap;
import java.util.Map;

public class PaymentService implements RequestHandler<APIGatewayProxyRequestEvent,APIGatewayProxyResponseEvent>
{

    final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

    final String tableName = System.getenv("SCALABLE_MARKETPLACE_ORDERS_TABLE");

    final String queue = System.getenv("SCALABLE_MARKETPLACE_QUEUE");

    final AmazonSQS awsSqsClient = AmazonSQSClientBuilder.defaultClient();

    final ObjectMapper objectMapper = new ObjectMapper();

    LambdaLogger logger;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        // Context Logger
        logger = context.getLogger();

        // Log event
        logger.log("EVENT TYPE: " + event.getClass().toString());
        logger.log("EVENT BODY: " + event.getBody());

        Order order = null;
        try {
            order = objectMapper.readValue(event.getBody(), Order.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        // Insert Data into dynamodb table
        insertOrderIntoDB(tableName, event.getBody());

        // Produce event to queue
        sendSingleMessage(queue, event.getBody());

        // Generate Response
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(responseHeaders);
        return response
                .withStatusCode(200)
                .withBody("Success");
    }

    private void insertOrderIntoDB(String tableName, String body){
        dynamoDB
                .getTable(tableName)
                .putItem(Item.fromJSON(body)).getPutItemResult();
        logger.log("DynamoDB record saved successfully: "+ body);
    }

    private void sendSingleMessage(String queueUrl, String message) {
        awsSqsClient.sendMessage(queueUrl,message);
        logger.log("Message has been pushed to Queue: "+ message);
    }
}
