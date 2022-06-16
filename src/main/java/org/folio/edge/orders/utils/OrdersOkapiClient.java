package org.folio.edge.orders.utils;

import static org.folio.edge.core.Constants.XML_OR_TEXT;
import static org.folio.edge.core.Constants.X_OKAPI_TOKEN;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.folio.edge.core.utils.OkapiClient;
import org.folio.rest.mappings.model.Routing;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;

public class OrdersOkapiClient extends OkapiClient {

  private static final Logger logger = LogManager.getLogger(OrdersOkapiClient.class);

  protected OrdersOkapiClient(Vertx vertx, String okapiURL, String tenant, int timeout) {
    super(vertx, okapiURL, tenant, timeout);
  }

  public OrdersOkapiClient(OkapiClient client) {
    super(client);
  }

  @Override
  protected void initDefaultHeaders() {
    super.initDefaultHeaders();
    defaultHeaders.set(HttpHeaders.ACCEPT, XML_OR_TEXT);
  }

  public void send(Routing routing, String payload, MultiMap params, MultiMap headers, Handler<HttpResponse<Buffer>> responseHandler,
                   Handler<Throwable> exceptionHandler) {

    final String method = routing.getProxyMethod() == null ? routing.getMethod() : routing.getProxyMethod();

    String resultPath = Optional.ofNullable(params)
      .map(it -> it.names().stream().reduce(routing.getProxyPath(), (acc, item) -> acc.replace(":" + item, params.get(item))))
      .orElse(routing.getProxyPath());

    switch (method) {
      case "POST":
        post(
          okapiURL + resultPath,
          tenant,
          StringUtils.isEmpty(payload) ? null : payload,
          combineHeadersWithDefaults(headers),
          responseHandler,
          exceptionHandler);
        break;
      case "GET":
        get(
          okapiURL + resultPath,
          tenant,
          combineHeadersWithDefaults(headers),
          responseHandler,
          exceptionHandler);
        break;
      case "PUT":
        put(
          okapiURL + resultPath,
          tenant,
          StringUtils.isEmpty(payload) ? null : payload,
          defaultHeaders,
          responseHandler,
          exceptionHandler);
        break;
    }
  }

  public void put(String url, String tenant, String payload, MultiMap headers, Handler<HttpResponse<Buffer>> responseHandler,
      Handler<Throwable> exceptionHandler) {

    HttpRequest<Buffer> request = client.putAbs(url);

    if (headers != null) {
      request.headers().setAll(combineHeadersWithDefaults(headers));
    } else {
      request.headers().setAll(defaultHeaders);
    }

    logger.info("PUT {} tenant: {} token: {}", () -> url, () -> tenant, () -> request.headers()
      .get(X_OKAPI_TOKEN));

    request.timeout(reqTimeout);
    if (StringUtils.isEmpty(payload)) {
      request.send()
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);

    } else {
      request.sendBuffer(Buffer.buffer(payload))
        .onSuccess(responseHandler)
        .onFailure(exceptionHandler);
    }

  }
}
