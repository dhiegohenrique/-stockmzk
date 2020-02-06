package com.stockmzk.validator;

import com.stockmzk.dto.ProductDto;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

public class ValidatorHandler implements Handler<RoutingContext>{

	@Override
	public void handle(RoutingContext routingContext) {
		final ProductDto product = Json.decodeValue(routingContext.getBodyAsString(), ProductDto.class);
		if (this.isNullOrEmpty(product.getBarCode())) {
			routingContext.fail(new Throwable("Informe o código de barras."));
			return;
		}
		
		if (this.isNullOrEmpty(product.getName())) {
			routingContext.fail(new Throwable("Informe o nome."));
			return;
		}
		
		if (product.getSerialNumber() <= 0) {
			routingContext.fail(new Throwable("Informe o número de série."));
			return;
		}
		
		routingContext.next();
	}
	
	private boolean isNullOrEmpty(String value) {
		return value == null || value.trim().isEmpty();
	}
}
