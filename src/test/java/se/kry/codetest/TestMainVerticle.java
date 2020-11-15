package se.kry.codetest;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
  }

  @Test
  @DisplayName("Start a web server on localhost responding to path /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server(Vertx vertx, VertxTestContext testContext) {
    WebClient.create(vertx)
        .get(8080, "::1", "/service")
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          testContext.completeNow();
        }));
  }

  @Test
  @DisplayName("start a web server and post a service should return OK")
  void start_http_server_and_add_service(Vertx vertx, VertxTestContext testContext) {
      WebClient.create(vertx)
              .post(8080, "::1", "/service")
              .sendJsonObject(new JsonObject()
                      .put("url","http://www.blocket.se")
                      .put("name","service1"),
                      response -> testContext.verify(() -> {
                  assertEquals(200, response.result().statusCode());
                  String body = response.result().body().toString();

                  assertEquals("OK", body);
                  testContext.completeNow();
              }));
  }

    @Test
    @DisplayName("start a web server and post a service, get request should show new service")
    void start_http_server_and_add_service_then_get_service(Vertx vertx, VertxTestContext testContext) {
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(new JsonObject()
                        .put("url","http://www.blocket.se")
                        .put("name","service1"), event -> {});

        WebClient.create(vertx)
                .get(8080,"::1","/service")
                .send(response -> testContext.verify(() -> {
                    JsonArray body = response.result().bodyAsJsonArray();
                    Boolean serviceExist = body.toString().contains("http://www.blocket.se");

                   assertTrue(serviceExist);
                    testContext.completeNow();
                }));

        assertEquals(0 ,0);
    }

    @Test
    @DisplayName("start a web server and post a service and then delete it, get request should not show new service")
    void start_http_server_and_add_service_then_delete_it(Vertx vertx, VertxTestContext testContext) throws InterruptedException {
        WebClient.create(vertx)
                .post(8080, "::1", "/service")
                .sendJsonObject(new JsonObject()
                        .put("url","http://www.blocket.se")
                        .put("name","service1"), event -> {
                });

        TimeUnit.SECONDS.sleep(1); //Waiting for service to be added
        WebClient.create(vertx)
                .delete(8080, "::1", "/service/service1")
                .send(response -> testContext.verify(() -> {
                    String body = response.result().bodyAsString();
                    assertEquals("OK", body);
                }));

        TimeUnit.SECONDS.sleep(2); //Waiting for service to be deleted
            WebClient.create(vertx)
                    .get(8080,"::1","/service")
                    .send(response -> testContext.verify(() -> {
                        JsonArray body = response.result().bodyAsJsonArray();
                        String bodyAsString = body.toString();
                        Boolean service1Exist = bodyAsString.contains("service1");
                        assertEquals(false, service1Exist);
                        testContext.completeNow();
                    }));
    }
}
