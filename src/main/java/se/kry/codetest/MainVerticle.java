package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private HashMap<String, String> services = new HashMap<>();
  //TODO use this
  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);

    checkAndCreateServiceTable();

    addSavedUrlsToHashServices();

    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    services.put("https://www.kry.se", "UNKNOWN");
    vertx.setPeriodic(1000 * 1, timerId -> poller.pollServices(services));
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
      } else if (result.cause().equals(null)) {
        System.out.println("Creating service table...");
        createServiceTable();
      } else {
        System.out.println("Something happened");
      }
    });
  }

  private void createServiceTable(){
    Future<ResultSet> queryResultCreateServiceTable = connector.query("CREATE TABLE Services (name string, url string)");
    queryResultCreateServiceTable.setHandler(result -> {
      if (result.succeeded()) {
        System.out.println("Service table created");
      } else {
        System.out.println("Failed to create service table.");
      }
    });
  };

  private void addServiceToDB(String name, String url){
    Future<ResultSet> queryResultInsertService = connector.query("INSERT INTO Services VALUES (?, ?)", new JsonArray().add(name).add(url));
    queryResultInsertService.setHandler(result -> {
      if (result.succeeded()){
        System.out.println("Success: Added service with name:" + name + " and url " + url);
      } else {
        System.out.println("Fail: Could not add service with name:" + name + " and url " + url + result);
      }
    });
  }

  private void removeServiceFromDB(String name){
    Future<ResultSet> queryResultRemoveService = connector.query("DELETE FROM Services WHERE name='?'", new JsonArray().add(name));
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
      List<JsonObject> jsonServices = services
          .entrySet()
          .stream()
          .map(service ->
              new JsonObject()
                      //TODO: Put name from DB
                  .put("url", service.getKey())
                  .put("status", service.getValue()))
          .collect(Collectors.toList());
      req.response()
          .putHeader("content-type", "application/json")
          .end(new JsonArray(jsonServices).encode());
    });
    router.post("/service").handler(req -> {
      JsonObject jsonBody = req.getBodyAsJson();
      addService(jsonBody.getString("name"), jsonBody.getString("url"));
      req.response()
          .putHeader("content-type", "text/plain")
          .end("OK");
    });
    router.delete("/service/:id").handler(routingContext -> {
      String id = routingContext.request().getParam("id");
      deleteService(id);
      routingContext.response()
              .putHeader("content-type", "text/plain")
              .end("OK");
    });
  }

  private void addService(String name, String url){
    services.put(url, "UNKNOWN");
    addServiceToDB(name, url);
  }

  private void deleteService(String id){
    System.out.println("deleting service: " + id);
    services.remove(id);
    removeServiceFromDB(id);
  }

  private void addSavedUrlsToHashServices(){
    Future<ResultSet> queryResultSelectServiceTable = connector.query("SELECT url FROM Services");
    queryResultSelectServiceTable.setHandler(result -> {
      if (result.succeeded()) {
        System.out.println("Service table exist");

        ResultSet results = result.result();
        List<JsonArray> resultList = results.getResults();
        resultList.forEach(urlJson -> {
          String url = urlJson.getString(0);
          services.put(url, "UNKNOWN");
        });

      } else if (result.cause().equals(null)) {
        System.out.println("Failed to add saved urls to services ...");
      } else {
        System.out.println("Something happened");
      }
    });
  }

}



