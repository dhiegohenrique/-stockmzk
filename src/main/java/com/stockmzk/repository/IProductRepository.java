package com.stockmzk.repository;

import java.util.List;

import com.stockmzk.entity.ProductEntity;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface IProductRepository {

	void insert(ProductEntity product, Handler<AsyncResult<Integer>> next);

	void getAll(Handler<AsyncResult<List<ProductEntity>>> next, boolean isAvailable);

	void setAvailable(int id, Handler<AsyncResult<Void>> next);

	void exists(ProductEntity product, Handler<AsyncResult<Boolean>> next);
}