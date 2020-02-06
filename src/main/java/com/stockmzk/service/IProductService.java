package com.stockmzk.service;

import java.util.List;

import com.stockmzk.dto.ProductDto;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface IProductService {
	
	void insert(ProductDto product, Handler<AsyncResult<Integer>> next);

	void getAll(Handler<AsyncResult<List<ProductDto>>> next, boolean isAvailable);

	void setAvailable(int id, Handler<AsyncResult<Void>> next);
}
