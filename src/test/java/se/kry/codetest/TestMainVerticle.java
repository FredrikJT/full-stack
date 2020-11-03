package se.kry.codetest;

import com.fasterxml.jackson.databind.util.JSONPObject;
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
          JsonArray body = response.result().bodyAsJsonArray();
          assertEquals(1, body.size());
          testContext.completeNow();
        }));
  }

  @Test
  @DisplayName("start a web server and post a service should return OK")
  void start_http_server_and_add_service(Vertx vertx, VertxTestContext testContext) {
      WebClient.create(vertx)
              .post(8080, "::1", "/service")
              .sendJsonObject(new JsonObject()
                      .put("url","service1"),
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
                                .put("url","service1"), event -> {});

        System.out.println("hejda");

        WebClient.create(vertx)
                .get(8080,"::1","/service")
                .send(response -> testContext.verify(() -> {
                    JsonArray body = response.result().bodyAsJsonArray();

                    JsonObject newService = (JsonObject) body.getValue(1);
                   String serviceName = (String) newService.getValue("name");
                    assertEquals("service1", serviceName);
                    testContext.completeNow();
                }));

        assertEquals(0 ,0);
    }


}
