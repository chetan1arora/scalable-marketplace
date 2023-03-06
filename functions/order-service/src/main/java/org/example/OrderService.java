package org.example;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.elasticache.AmazonElastiCache;
import com.amazonaws.services.elasticache.AmazonElastiCacheClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.example.models.Cart;
import org.example.models.OrderRequest;

import java.util.HashMap;
import java.util.Map;

public class OrderService implements RequestHandler<APIGatewayProxyRequestEvent,APIGatewayProxyResponseEvent>
{

    final DynamoDB dynamoDB = new DynamoDB(AmazonDynamoDBClientBuilder.defaultClient());

    final AmazonElastiCache amazonElastiCache = AmazonElastiCacheClientBuilder.defaultClient();

    final String tableName = System.getenv("SCALABLE_MARKETPLACE_ORDERS_TABLE");

    final String paymentQueue = System.getenv("SCALABLE_MARKETPLACE_PAYMENT_QUEUE");

    final String deliveryQueue = System.getenv("SCALABLE_MARKETPLACE_ORDER_QUEUE");

    final AmazonSQS awsSqsClient = AmazonSQSClientBuilder.defaultClient();

    final ObjectMapper objectMapper = new ObjectMapper();

    LambdaLogger logger;

    final Map<String, String> responseHeaders = new HashMap<>(){{
        put("Content-Type", "application/json");
    }};

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        // Context Logger
        logger = context.getLogger();

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(responseHeaders);

        String requestAction = event.getQueryStringParameters().get("ACTION");
        if(requestAction == null){
            return response
                    .withStatusCode(HttpStatus.SC_BAD_REQUEST)
                    .withBody("ACTION query parameter not found");
        }

        logger.log("INFO::Mapping event to request object..");
        OrderRequest orderRequest = mapBodyToObject(event.getBody());

        logger.log("INFO::Setting up DB connection..");
        createDBConnection();

        String responseBody;
        switch(requestAction.toUpperCase()){
            case "INITIALIZE_ORDER":
                responseBody = initiateOrder(orderRequest);
                break;
            case "FINALIZE_ORDER":
                responseBody = finalizeOrder(orderRequest);
                break;
            default:
                return response
                        .withStatusCode(HttpStatus.SC_BAD_REQUEST)
                        .withBody("ACTION query parameter invalid");
        }
        return response
                .withStatusCode(HttpStatus.SC_OK)
                .withBody(responseBody);
    }

    private String initiateOrder(OrderRequest orderRequest) {
        Cart cart = orderRequest.getCart();

        // Check inventory for given resource
        // Create orderId in Redis and take inventory lock
        cart.getCartItems().forEach((productId,quantity) -> {
            
        });
        logger.log("INFO::Took inventory lock on items");

        cart.getTotalAmount(); // Send to payment processor
        sendSingleMessage(paymentQueue, orderRequest.toString()); // Send order to Payment service
        return "Order created successfully";
    }

    private String finalizeOrder(OrderRequest orderRequest){
        Cart cart = orderRequest.getCart();
        takeInventoryLock(cart.getCartItems());
        sendSingleMessage(deliveryQueue, orderRequest.toString()); // Send successful order to Delivery service
        return "Order Placed successfully";
    }

    private OrderRequest mapBodyToObject(String body) {
        OrderRequest orderRequest;
        try {
            orderRequest = objectMapper.readValue(body, OrderRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return orderRequest;
    }

    private void createDBConnection(){
//        Fetch DB Connection
//        Jdbi jdbi = Jdbi.create(dbHostName, dbUserName, dbPassword);
//        jdbi.installPlugins();
//        itemDao = jdbi.onDemand(ItemDao.class);
    }

    private void insertOrderIntoDB(String body){
        dynamoDB
                .getTable(tableName)
                .putItem(Item.fromJSON(body)).getPutItemResult();
        logger.log("DynamoDB record saved successfully: "+ body);
    }

    private void sendSingleMessage(String queueUrl, String message) {
        awsSqsClient.sendMessage(queueUrl,message);
        logger.log("Message has been pushed to Queue: "+ message);
    }

    private void takeInventoryLock(Map<Integer, Integer> cartItems){
//        Check cache if all product exists
//           Or fetch it from API and put it in cache
//        cacheData = fetchRedis;
//        if(cacheData is empty)
//            fetchInventoryService(Auto update cache)
//            cacheData = fetchRedis;
//            if(cacheData is empty)
//                error
    }
    private void checkAndUpdateProductInventory(Cart cart){

        // Check if inventory is present
//        cart.getCartItems().forEach(Map.ent);

        // Check
//        dynamoDB
//                .getTable(tableName)
//                .putItem(Item.fromJSON(body)).getPutItemResult();
        logger.log("DynamoDB record saved successfully: "+ cart);
    }


}
