package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

import java.util.List;

public class BackgroundPoller {

  public void pollServices(Vertx vertx, DBConnector connector) {
    WebClient client = WebClient.create(vertx);

    Future<ResultSet> queryResultGetServices = connector.query("SELECT DISTINCT url FROM Services");
    queryResultGetServices.setHandler(result -> {
        if (result.succeeded()){
            System.out.println("Success: Fetched service urls" + result.result().getResults());
            List<JsonArray> urlsArray = result.result().getResults();
            urlsArray.forEach(urlArray -> {
                String httpUrl = urlArray.getString(0);
                String[] splitUrl = httpUrl.split("http://");
                String cleanedUrl = splitUrl[1];

                client.get(80, cleanedUrl, "")
                        .send(ar -> {
                            if (ar.succeeded()) {
                                HttpResponse<Buffer> response = ar.result();

                                System.out.println("Success: Polling url " + cleanedUrl + " received response with status code " + response.statusCode());
                                updateServiceStatus(connector, httpUrl, "OK");
                            } else {
                                System.out.println("Fail: Polling url " + cleanedUrl + " went wrong " + ar.cause().getMessage());
                                updateServiceStatus(connector, httpUrl, "FAIL");
                            }
                        });
            });
        } else {
            System.out.println("Fail: Could not fetch service urls " + result);
        }
    });
  }

  private void updateServiceStatus(DBConnector connector, String url, String status){
    Future<ResultSet> queryResultInsertService = connector.query("UPDATE Services SET status=? WHERE url=?", new JsonArray().add(status).add(url));
    queryResultInsertService.setHandler(result -> {
        if (result.succeeded()){
            System.out.println("Success: Updated status to: " + status + " for service with url " + url);
        } else {
            System.out.println("Fail: Could not update status for service with url: " + url + result);
        }
    });
  }
}
