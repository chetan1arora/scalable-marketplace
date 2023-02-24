package org.example;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.rds.AmazonRDS;
import com.amazonaws.services.rds.AmazonRDSClient;
import com.amazonaws.services.rds.AmazonRDSClientBuilder;
import com.amazonaws.services.rds.model.DBCluster;
import org.example.models.Product;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InventoryService implements RequestHandler<APIGatewayProxyRequestEvent,APIGatewayProxyResponseEvent>
{
    final AmazonRDS amazonRDS = AmazonRDSClientBuilder.defaultClient();

    final String tableName = System.getenv("SCALABLE_MARKETPLACE_PRODUCTS_TABLE");

    LambdaLogger logger;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        // Context Logger
        logger = context.getLogger();
        
        // Insert Data into dynamodb table
        List<Product> productList = listProductsByType(tableName, event.getBody());

        // Generate Response
        Map<String, String> responseHeaders = new HashMap<>();
        responseHeaders.put("Content-Type", "application/json");
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(responseHeaders);
        return response
                .withStatusCode(200)
                .withBody(productList.toString());
    }

    private List<Product> listProductsByType(String tableName, String body) {
//        DBCluster dbCluster = amazonRDS.describeDBClusters().getDBClusters().get(0);
        return null;
    }


}
