package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.time.LocalDateTime;
import java.util.List;

public class MainVerticle extends AbstractVerticle {

  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);

    checkAndCreateServiceTable();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    vertx.setPeriodic(1000 * 5, timerId -> poller.pollServices(vertx, connector));
    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            System.out.println("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });
  }

  private void checkAndCreateServiceTable(){
    Future<ResultSet> queryResultServiceTableExist = connector.query("SELECT 1 FROM Services WHERE 1=0");
    queryResultServiceTableExist.setHandler(result -> {
      if (result.succeeded()) {
        System.out.println("Service table exist");
      } else if (result.cause().equals(null) || result.cause().getMessage().contains("no such table")) {
        System.out.println("Creating service table...");
        createServiceTable();
      } else {
        System.out.println("Something went wrong...");
      }
    });
  }

  private void createServiceTable(){
    Future<ResultSet> queryResultCreateServiceTable = connector.query("CREATE TABLE Services (name string, url string, status string, date string)");
    queryResultCreateServiceTable.setHandler(result -> {
      if (result.succeeded()) {
        System.out.println("Service table created");
      } else {
        System.out.println("Failed to create service table.");
      }
    });
  }

  private void addServiceToDB(String name, String url, String date){
    Future<ResultSet> queryResultInsertService = connector.query("INSERT INTO Services VALUES (?, ?, ?, ?)", new JsonArray().add(name).add(url).add("UNKNOWN").add(date));
    queryResultInsertService.setHandler(result -> {
      if (result.succeeded()){
        System.out.println("Success: Added service with name:" + name + " and url " + url);
      } else {
        System.out.println("Fail: Could not add service with name:" + name + " and url " + url + result);
      }
    });
  }

  private void removeServiceFromDB(String name){
    Future<ResultSet> queryResultRemoveService = connector.query("DELETE FROM Services WHERE name=?", new JsonArray().add(name));
    queryResultRemoveService.setHandler(result -> {
      if (result.succeeded()){
        System.out.println("Success: Removed service with name:" + name);
      } else {
        System.out.println("Fail: Could not remove service with name:" + name + result);
      }
    });
  }

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());
    router.get("/service").handler(req -> {
      Future<ResultSet> queryResultGetServices = connector.query("SELECT DISTINCT * FROM Services");
      queryResultGetServices.setHandler(result -> {
        if (result.succeeded()){
          List<JsonArray> resultArray = result.result().getResults();
          req.response()
                  .putHeader("content-type", "application/json")
                  .end(resultArray.toString());
        } else {
          req.response()
                  .putHeader("content-type", "application/json")
                  .end("failed");
          System.out.println("Fail: Could not fetch services from DB. " + result);
        }
      });
    });

    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      addServiceToDB(jsonBody.getString("name"), jsonBody.getString("url"), LocalDateTime.now().toString());
      req.response()
          .putHeader("content-type", "text/plain")
          .end("OK");
    });

    router.delete("/service/:id").handler(routingContext -> {
      String id = routingContext.request().getParam("id");
      removeServiceFromDB(id);
      routingContext.response()
              .putHeader("content-type", "text/plain")
              .end("OK");
    });
  }
}



