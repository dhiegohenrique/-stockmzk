package com.stockmzk.repository;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.stockmzk.entity.ProductEntity;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;

public class ProductRepository implements IProductRepository {

	private JDBCClient client;

	private static final AtomicInteger COUNTER = new AtomicInteger(1);

	private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS Product (id INTEGER IDENTITY, name varchar(100), barCode varchar(100), available BOOLEAN, serialNumber INTEGER)";
	private static final String SQL_INSERT = "INSERT INTO Product (id, name, barCode, available, serialNumber) VALUES ?, ?, ?, ?, ?";
	private static final String SQL_SELECT = "SELECT * FROM Product";
	private static final String SQL_ORDER_BY = " ORDER BY id";
	private static final String SQL_WHERE = " WHERE available = ?";
	private static final String SQL_UPDATE = "UPDATE Product SET available = ? WHERE ID = ?";
	private static final String SQL_COUNT_BY_BAR_CODE_SERIAL_NUMBER = "SELECT COUNT(0) FROM Product WHERE barCode = ? and serialNumber = ?";

	public ProductRepository(Vertx vertx, JsonObject config) {
		this.client = JDBCClient.createShared(vertx, config);
	}

	private void getConnection(Handler<AsyncResult<SQLConnection>> next) {
		this.client.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				next.handle(Future.failedFuture(connectionHandler.cause()));
				return;
			}

			SQLConnection sqlConnection = connectionHandler.result();
			this.createTable(sqlConnection, (createTableHander) -> {
				if (createTableHander.failed()) {
					next.handle(Future.failedFuture(connectionHandler.cause()));
					return;
				}

				next.handle(Future.succeededFuture(sqlConnection));
			});
		});
	}

	private void createTable(SQLConnection sqlConnection, Handler<AsyncResult<Void>> next) {
		sqlConnection.execute(SQL_CREATE_TABLE, (resultHandler) -> {
			if (resultHandler.failed()) {
				next.handle(Future.failedFuture(resultHandler.cause()));
				sqlConnection.close();
				return;
			}
			next.handle(Future.succeededFuture());
		});
	}

	@Override
	public void exists(ProductEntity product, Handler<AsyncResult<Boolean>> next) {
		this.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				next.handle(Future.failedFuture(connectionHandler.cause()));
				return;
			}
			
			JsonArray jsonArray = new JsonArray();
			jsonArray.add(product.getBarCode());
			jsonArray.add(product.getSerialNumber());

			SQLConnection sqlConnection = connectionHandler.result();
			sqlConnection.queryWithParams(SQL_COUNT_BY_BAR_CODE_SERIAL_NUMBER, jsonArray, (countResult) -> {
				if (countResult.failed()) {
					next.handle(Future.failedFuture(connectionHandler.cause()));
					return;
				}

				String columnName = "count";
				ResultSet resultSet = countResult.result();
				resultSet.setColumnNames(Arrays.asList(columnName));
				JsonObject jsonObject = resultSet.getRows().get(0);
				int count = jsonObject.getInteger(columnName);
				
				next.handle(Future.succeededFuture(count > 0));
			});
		});
	}

	@Override
	public void insert(ProductEntity product, Handler<AsyncResult<Integer>> next) {
		product.setAvailable(true);
		int id = product.getId();
		if (id <= 0) {
			id = COUNTER.getAndIncrement();
		}

		product.setId(id);

		this.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				next.handle(Future.failedFuture(connectionHandler.cause()));
				return;
			}

			SQLConnection sqlConnection = connectionHandler.result();
			this.insert(product, sqlConnection, (insertHandler) -> {
				if (insertHandler.failed()) {
					next.handle(Future.failedFuture(connectionHandler.cause()));
					return;
				}

				next.handle(Future.succeededFuture(product.getId()));
			});
		});
	}

	private void insert(ProductEntity product, SQLConnection connection, Handler<AsyncResult<Integer>> next) {
		JsonArray jsonArray = new JsonArray();
		jsonArray.add(product.getId());
		jsonArray.add(product.getName());
		jsonArray.add(product.getBarCode());
		jsonArray.add(product.isAvailable());
		jsonArray.add(product.getSerialNumber());

		connection.updateWithParams(SQL_INSERT, jsonArray, (result) -> {
			if (result.failed()) {
				next.handle(Future.failedFuture(result.cause()));
				connection.close();
				return;
			}
			next.handle(Future.succeededFuture());
		});
	}

	@Override
	public void getAll(Handler<AsyncResult<List<ProductEntity>>> next, boolean isAvailable) {
		this.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				next.handle(Future.failedFuture(connectionHandler.cause()));
				return;
			}

			SQLConnection sqlConnection = connectionHandler.result();
			Handler<AsyncResult<ResultSet>> resultHandler = (result) -> {
				List<ProductEntity> products = result.result().getRows().stream().map(ProductEntity::new)
						.collect(Collectors.toList());
				next.handle(Future.succeededFuture(products));
				sqlConnection.close();
			};

			String sql = SQL_SELECT;
			if (isAvailable) {
				sql += SQL_WHERE + SQL_ORDER_BY;
				sqlConnection.queryWithParams(sql, new JsonArray().add(true), resultHandler);
			} else {
				sql += SQL_ORDER_BY;
				sqlConnection.query(sql, resultHandler);
			}
		});
	}

	@Override
	public void setAvailable(int id, Handler<AsyncResult<Void>> next) {
		this.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				next.handle(Future.failedFuture(connectionHandler.cause()));
				return;
			}

			JsonArray jsonArray = new JsonArray();
			jsonArray.add(false);
			jsonArray.add(id);

			SQLConnection sqlConnection = connectionHandler.result();
			sqlConnection.updateWithParams(SQL_UPDATE, jsonArray, (result) -> {
				if (result.failed()) {
					next.handle(Future.failedFuture(result.cause()));
					sqlConnection.close();
					return;
				}

				next.handle(Future.succeededFuture());
			});
		});
	}
}
