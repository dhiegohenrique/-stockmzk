package com.stockmzk.route;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

public interface IProductRouting {

	Handler<RoutingContext> getAll();
	
	Handler<RoutingContext> getAllAvailable();
	
	void insert(RoutingContext routingContext);
	
	void setAvailable(RoutingContext routingContext);
}
