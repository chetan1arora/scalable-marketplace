package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpStatus;
import org.example.models.InventoryRequest;
import org.example.models.Item;
import org.example.models.ItemDao;
import org.jdbi.v3.core.Jdbi;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryService implements RequestHandler<APIGatewayProxyRequestEvent,APIGatewayProxyResponseEvent>
{
    final String dbHostName = System.getenv("SCALABLE_MARKETPLACE_POSTGRES_HOST");

    final String dbUserName = System.getenv("SCALABLE_MARKETPLACE_POSTGRES_USERNAME");

    final String dbPassword = System.getenv("SCALABLE_MARKETPLACE_POSTGRES_PASSWORD");

    final ObjectMapper objectMapper = new ObjectMapper();

    LambdaLogger logger;

    ItemDao itemDao;

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
        InventoryRequest inventoryRequest = mapBodyToObject(event.getBody());

        logger.log("INFO::Setting up DB connection..");
        createDBConnection();

        String responseBody;
        switch(requestAction.toUpperCase()){
            case "LIST":
                // Insert Data into dynamodb table
                responseBody = listInventoryByType(inventoryRequest);
                break;
            case "UPDATE":
                responseBody = updateInventory(inventoryRequest);
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

    private void createDBConnection(){
        // Fetch DB Connection
        Jdbi jdbi = Jdbi.create(dbHostName, dbUserName, dbPassword);
        jdbi.installPlugins();
        itemDao = jdbi.onDemand(ItemDao.class);
    }

    private String listInventoryByType(InventoryRequest inventoryRequest) {
        List<Item> itemList = itemDao.findAllByType(inventoryRequest.getType());
        logger.log("item list: {}"+itemList.toString());
        String result;
        try {
            result = objectMapper.writeValueAsString(itemList);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return result;
    }

    private String updateInventory(InventoryRequest inventoryRequest) {
        Item item = itemDao.findById(inventoryRequest.getId());
        logger.log("INFO::Item record fetched from DB: "+item.getId());
        if(inventoryRequest.getQuantity() > item.getQuantity()){
            logger.log("INFO::Not enough inventory(stock) to fulfil quantity: "+item.getId());
            return "Not enough inventory(stock) to fulfil quantity";
        }
        item.setQuantity(item.getQuantity()- inventoryRequest.getQuantity());
        itemDao.updateQuantity(item);
        logger.log("INFO::Item quantity updated successfully: "+item.getId());
        return "Inventory update successful";
    }

    private InventoryRequest mapBodyToObject(String body){
        InventoryRequest inventoryRequest;
        try {
            inventoryRequest = objectMapper.readValue(body, InventoryRequest.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return inventoryRequest;
    }

}
