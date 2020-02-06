package com.stockmzk.route;

import java.net.HttpURLConnection;
import java.util.List;

import com.stockmzk.dto.ProductDto;
import com.stockmzk.service.IProductService;
import com.stockmzk.service.ProductService;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public final class ProductRouting implements IProductRouting {

	private IProductService productService;

	public ProductRouting(Vertx vertx, JsonObject config) {
		this.productService = new ProductService(vertx, config);
	}

	@Override
	public Handler<RoutingContext> getAll() {
		return (routingContext) -> {
			this.productService.getAll((result) -> {
				List<ProductDto> listProducts = result.result();
				routingContext.response().end(Json.encodePrettily(listProducts));
			}, false);
		};
	}

	@Override
	public Handler<RoutingContext> getAllAvailable() {
		return (routingContext) -> {
			this.productService.getAll((result) -> {
				if (result.failed()) {
					routingContext.fail(result.cause());
					return;
				}
				
				List<ProductDto> listProducts = result.result();
				routingContext.response().end(Json.encodePrettily(listProducts));
			}, true);
		};
	}

	@Override
	public void insert(RoutingContext routingContext) {
		final ProductDto product = Json.decodeValue(routingContext.getBodyAsString(), ProductDto.class);
		this.productService.insert(product, (result) -> {
			if (result.failed()) {
				routingContext.fail(result.cause());
				return;
			}
			
			JsonObject jsonObject = new JsonObject();
			jsonObject.put("id", result.result());

			routingContext.response().setStatusCode(HttpURLConnection.HTTP_CREATED).end(Json.encodePrettily(jsonObject));
		});
	}

	@Override
	public void setAvailable(RoutingContext routingContext) {
		String id = routingContext.request().getParam("id");
		this.productService.setAvailable(Integer.parseInt(id), (result) -> {
			if (result.failed()) {
				routingContext.fail(result.cause());
				return;
			}
			
			routingContext.response().setStatusCode(HttpURLConnection.HTTP_NO_CONTENT).end();
		});
	}
}
