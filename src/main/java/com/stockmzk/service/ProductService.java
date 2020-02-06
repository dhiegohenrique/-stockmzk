package com.stockmzk.service;

import java.lang.reflect.Type;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.modelmapper.TypeToken;

import com.stockmzk.dto.ProductDto;
import com.stockmzk.entity.ProductEntity;
import com.stockmzk.repository.IProductRepository;
import com.stockmzk.repository.ProductRepository;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ProductService implements IProductService {
	
	private IProductRepository productRepository;
	
	private ModelMapper modelMapper;
	
	public ProductService(Vertx vertx, JsonObject config) {
		this.productRepository = new ProductRepository(vertx, config);
		this.modelMapper = new ModelMapper();
	}

	@Override
	public void insert(ProductDto product, Handler<AsyncResult<Integer>> next) {
		this.productRepository.exists(this.getProductEntity(product), (result) -> {
			if (result.failed()) {
				next.handle(Future.failedFuture(result.cause()));
				return;
			}
			
			if (result.result()) {
				next.handle(Future.failedFuture("Já existe um produto com este código de barras e número de série."));
				return;
			}
			
			this.productRepository.insert(this.getProductEntity(product), next);
		});
		
	}

	@Override
	public void getAll(Handler<AsyncResult<List<ProductDto>>> next, boolean isAvailable) {
		this.productRepository.getAll((result) -> {
			if (result.failed()) {
				next.handle(Future.failedFuture(result.cause()));
				return;
			}
			
			List<ProductEntity> listProductEntity = result.result();
			Type listType = new TypeToken<List<ProductEntity>>() {}.getType();
			List<ProductDto> listProductDto = this.modelMapper.map(listProductEntity, listType);
			next.handle(Future.succeededFuture(listProductDto));
		}, isAvailable);
	}

	@Override
	public void setAvailable(int id, Handler<AsyncResult<Void>> next) {
		this.productRepository.setAvailable(id, next);
	}
	
	private ProductEntity getProductEntity(ProductDto product) {
		return this.modelMapper.map(product, ProductEntity.class);
	}
}