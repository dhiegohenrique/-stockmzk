package com.stockmzk.route;

import com.stockmzk.main.MainVerticle;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class FailureHandler implements Handler<RoutingContext> {

	private final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

	@Override
	public void handle(final RoutingContext routingContext) {
		LOGGER.error("ERROR: " + routingContext.failure());
		String message = routingContext.failure().getMessage();
		if (message != null) {
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("error", message);
			message = Json.encodePrettily(jsonObject);
		}
		
		routingContext.response()
				.setStatusCode(500).end(message);
	}
}