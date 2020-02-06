package com.stockmzk.product;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.stockmzk.dto.ProductDto;
import com.stockmzk.entity.ProductEntity;
import com.stockmzk.main.MainVerticle;
import com.stockmzk.route.Routes;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;

@ExtendWith(VertxExtension.class)
public class ProductTest {

	private final int port = 8082;
	private final String url = "localhost";

	private Vertx vertx;

	private WebClient webClient;

	private JDBCClient client;

	private Random random;

	private final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS Product (id INTEGER IDENTITY, name varchar(100), barCode varchar(100), available BOOLEAN, serialNumber INTEGER)";
	private final String SQL_INSERT = "INSERT INTO Product (id, name, barCode, available, serialNumber) VALUES (?, ?, ?, ?, ?)";
	private final String SQL_SELECT = "SELECT * FROM Product";
	private final String SQL_DELETE = "DELETE FROM Product";

	@BeforeEach
	public void tearUp(VertxTestContext testContext) {
		this.vertx = Vertx.vertx();
		this.random = new Random();

		JsonObject jsonConfig = new JsonObject();
		jsonConfig.put("http.port", this.port);
		jsonConfig.put("url", "jdbc:hsqldb:mem:stockmzk-test?shutdown=true");
		jsonConfig.put("driver_class", "org.hsqldb.jdbcDriver");
		jsonConfig.put("max_pool_size", 30);

		DeploymentOptions options = new DeploymentOptions();
		options.setConfig(jsonConfig);

		this.vertx.deployVerticle(new MainVerticle(), options, (completionHandler) -> {
			if (completionHandler.failed()) {
				testContext.failNow(completionHandler.cause());
				return;
			}

			this.client = JDBCClient.createShared(this.vertx, jsonConfig);
			this.client.getConnection((connectionHandler) -> {
				if (connectionHandler.failed()) {
					testContext.failNow(connectionHandler.cause());
					return;
				}
				
				SQLConnection sqlConnection = connectionHandler.result();
				sqlConnection.execute(this.SQL_DELETE, (result) -> {
					this.webClient = WebClient.create(this.vertx);
					testContext.completeNow();
				});
			});
		});
	}
	
	@AfterEach
	public void tearDown(VertxTestContext testContext) {
	  this.client.close((closeHandler) -> {
		  this.vertx.close((completionHandler) -> {
			  testContext.completeNow();
		  });
	  });
	}
	
	@Test
	@DisplayName("Should add new product")
	void shouldAddNewProduct(VertxTestContext testContext) throws Throwable {
		ProductDto product = new ProductDto();
		product.setName("name" + new Date().getTime());
		product.setBarCode(Long.toString(new Date().getTime()));
		product.setSerialNumber(this.random.nextInt(100));

		this.webClient.post(this.port, this.url, Routes.API_PRODUCTS)
				.putHeader("content-type", "application/json; charset=utf-8").sendJson(product, (responseHandler) -> {
					int statusCode = responseHandler.result().statusCode();

					testContext.verify(() -> assertThat(statusCode).isEqualTo(HttpURLConnection.HTTP_CREATED));

					this.getConnection((connectionHandler) -> {
						if (connectionHandler.failed()) {
							testContext.failNow(connectionHandler.cause());
							return;
						}

						JsonArray jsonArray = new JsonArray();
						jsonArray.add(product.getName());
						jsonArray.add(product.getBarCode());
						jsonArray.add(product.getSerialNumber());

						String sql = SQL_SELECT + " WHERE name = ? and barCode = ? and serialNumber = ?";

						SQLConnection sqlConnection = connectionHandler.result();
						sqlConnection.queryWithParams(sql, jsonArray, (resultHandler) -> {
							List<ProductEntity> listProducts = resultHandler.result().getRows().stream()
									.map(ProductEntity::new).collect(Collectors.toList());
							ProductEntity productEntity = listProducts.get(0);

							testContext.verify(() -> assertThat(productEntity.getId()).isNotZero());
							testContext
									.verify((() -> assertThat(productEntity.getName()).isEqualTo(product.getName())));
							testContext.verify(
									(() -> assertThat(productEntity.getBarCode()).isEqualTo(product.getBarCode())));
							testContext.verify(() -> assertThat(productEntity.getSerialNumber())
									.isEqualTo(product.getSerialNumber()));
							testContext.verify((() -> assertThat(productEntity.isAvailable()).isEqualTo(true)));

							testContext.completeNow();
						});
					});
				});
	}

	@Test
	@DisplayName("Should validate barCode field of product")
	void shouldValidateBarCodeField(VertxTestContext testContext) {
		ProductDto product = new ProductDto();
		product.setBarCode(Long.toString(new Date().getTime()));
		product.setSerialNumber(this.random.nextInt(100));

		this.webClient.post(this.port, this.url, Routes.API_PRODUCTS)
				.putHeader("content-type", "application/json; charset=utf-8").sendJson(product, (responseHandler) -> {
					int statusCode = responseHandler.result().statusCode();
					testContext.verify(() -> assertThat(statusCode).isEqualTo(HttpURLConnection.HTTP_INTERNAL_ERROR));

					JsonObject jsonObject = responseHandler.result().bodyAsJsonObject();
					String validationMessage = jsonObject.getString("error");
					testContext.verify(() -> assertThat(validationMessage).isEqualTo("Informe o nome."));
					testContext.completeNow();
				});
	}

	@Test
	@DisplayName("Should validate name field of product")
	void shouldValidateNameField(VertxTestContext testContext) {
		ProductDto product = new ProductDto();
		product.setSerialNumber(this.random.nextInt(100));

		this.webClient.post(this.port, this.url, Routes.API_PRODUCTS)
				.putHeader("content-type", "application/json; charset=utf-8").sendJson(product, (responseHandler) -> {
					int statusCode = responseHandler.result().statusCode();
					testContext.verify(() -> assertThat(statusCode).isEqualTo(HttpURLConnection.HTTP_INTERNAL_ERROR));

					JsonObject jsonObject = responseHandler.result().bodyAsJsonObject();
					String validationMessage = jsonObject.getString("error");
					testContext.verify(() -> assertThat(validationMessage).isEqualTo("Informe o código de barras."));
					testContext.completeNow();
				});
	}

	@Test
	@DisplayName("Should validate serialNumber field of product")
	void shouldValidateSerialNumberField(VertxTestContext testContext) {
		ProductDto product = new ProductDto();
		product.setName("name" + new Date().getTime());
		product.setBarCode(Long.toString(new Date().getTime()));

		this.webClient.post(this.port, this.url, Routes.API_PRODUCTS)
				.putHeader("content-type", "application/json; charset=utf-8").sendJson(product, (responseHandler) -> {
					int statusCode = responseHandler.result().statusCode();
					testContext.verify(() -> assertThat(statusCode).isEqualTo(HttpURLConnection.HTTP_INTERNAL_ERROR));

					JsonObject jsonObject = responseHandler.result().bodyAsJsonObject();
					String validationMessage = jsonObject.getString("error");
					testContext.verify(() -> assertThat(validationMessage).isEqualTo("Informe o número de série."));
					testContext.completeNow();
				});
	}

	@Test
	@DisplayName("Should validate if the product already exists")
	void shouldValidateExistsProduct(VertxTestContext testContext) {
		ProductDto product = new ProductDto();
		product.setId(1);
		product.setName("name" + new Date().getTime());
		product.setBarCode(Long.toString(new Date().getTime()));
		product.setSerialNumber(this.random.nextInt(100));
		product.setAvailable(true);

		this.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				testContext.failNow(connectionHandler.cause());
				return;
			}

			SQLConnection sqlConnection = connectionHandler.result();
			this.insert(product, sqlConnection, (resultHandler) -> {
				if (resultHandler.failed()) {
					testContext.failNow(resultHandler.cause());
					return;
				}

				this.webClient.post(this.port, this.url, Routes.API_PRODUCTS)
						.putHeader("content-type", "application/json; charset=utf-8")
						.sendJson(product, (responseHandler) -> {
							int statusCode = responseHandler.result().statusCode();
							testContext.verify(
									() -> assertThat(statusCode).isEqualTo(HttpURLConnection.HTTP_INTERNAL_ERROR));

							JsonObject jsonObject = responseHandler.result().bodyAsJsonObject();
							String validationMessage = jsonObject.getString("error");
							testContext.verify(() -> assertThat(validationMessage)
									.isEqualTo("Já existe um produto com este código de barras e número de série."));
							testContext.completeNow();
						});
			});
		});
	}

	@Test
	@DisplayName("Should return all products")
	void shouldReturnAllProducts(VertxTestContext testContext) {
		this.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				testContext.failNow(connectionHandler.cause());
				return;
			}

			List<ProductDto> listProducts = new ArrayList<>();
			for (int index = 1; index <= this.random.nextInt(10); index++) {
				ProductDto product = new ProductDto();
				product.setId(index);
				product.setName("name" + new Date().getTime() + index);
				product.setBarCode(Long.toString(new Date().getTime()) + index);
				product.setSerialNumber(this.random.nextInt(100) + index);
				product.setAvailable(true);
				listProducts.add(product);
			}

			SQLConnection sqlConnection = connectionHandler.result();
			this.insert(listProducts, sqlConnection, (resultHandler) -> {
				if (resultHandler.failed()) {
					testContext.failNow(resultHandler.cause());
					return;
				}

				this.webClient.get(this.port, this.url, Routes.API_PRODUCTS).send(testContext.succeeding((resp) -> {
					testContext.verify(() -> assertThat(resp.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK));

					String bodyAsString = resp.bodyAsString();
					ProductDto[] arrayProducts = Json.decodeValue(bodyAsString, ProductDto[].class);
					this.validateProduct(testContext, listProducts, arrayProducts);

					testContext.completeNow();
				}));
			});
		});
	}

	@Test
	@DisplayName("Should return all available products")
	void shouldReturnAllAvailableProducts(VertxTestContext testContext) {
		this.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				testContext.failNow(connectionHandler.cause());
				return;
			}

			List<ProductDto> listProducts = new ArrayList<>();
			for (int index = 1; index <= this.random.nextInt(10); index++) {
				ProductDto product = new ProductDto();
				product.setId(index);
				product.setName("name" + new Date().getTime() + index);
				product.setBarCode(Long.toString(new Date().getTime()) + index);
				product.setSerialNumber(this.random.nextInt(100) + index);
				product.setAvailable((index % 2 == 0) ? true : false);
				listProducts.add(product);
			}

			SQLConnection sqlConnection = connectionHandler.result();
			this.insert(listProducts, sqlConnection, (resultHandler) -> {
				if (resultHandler.failed()) {
					testContext.failNow(resultHandler.cause());
					return;
				}

				this.webClient.get(this.port, this.url, Routes.API_PRODUCTS_AVAILABLE)
						.send(testContext.succeeding((resp) -> {
							testContext
									.verify(() -> assertThat(resp.statusCode()).isEqualTo(HttpURLConnection.HTTP_OK));

							List<ProductDto> listProductsAvailable = listProducts.stream().filter((product) -> {
								return product.isAvailable();
							}).collect(Collectors.toList());

							String bodyAsString = resp.bodyAsString();
							ProductDto[] arrayProducts = Json.decodeValue(bodyAsString, ProductDto[].class);
							this.validateProduct(testContext, listProductsAvailable, arrayProducts);

							testContext.completeNow();
						}));
			});
		});
	}

	private void validateProduct(VertxTestContext testContext, List<ProductDto> listProducts,
			ProductDto[] arrayProducts) {
		testContext.verify(() -> assertThat(listProducts.size()).isEqualTo(arrayProducts.length));

		for (int index = 0; index < listProducts.size(); index++) {
			ProductDto product1 = listProducts.get(index);
			ProductDto product2 = arrayProducts[index];
			testContext.verify(() -> assertThat(product1.getId()).isEqualTo(product2.getId()));
			testContext.verify(() -> assertThat(product1.getName()).isEqualTo(product2.getName()));
			testContext.verify(() -> assertThat(product1.getBarCode()).isEqualTo(product2.getBarCode()));
			testContext.verify(() -> assertThat(product1.getSerialNumber()).isEqualTo(product2.getSerialNumber()));
			testContext.verify(() -> assertThat(product1.isAvailable()).isEqualTo(product2.isAvailable()));
		}
	}

	@Test
	@DisplayName("Should update product to not available")
	void shouldUpdateProductToNotAvailable(VertxTestContext testContext) {
		this.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				testContext.failNow(connectionHandler.cause());
				return;
			}

			ProductDto product = new ProductDto();
			product.setId(1);
			product.setName("name" + new Date().getTime());
			product.setBarCode(Long.toString(new Date().getTime()));
			product.setSerialNumber(this.random.nextInt(100));
			product.setAvailable(true);

			SQLConnection sqlConnection = connectionHandler.result();
			this.insert(product, sqlConnection, (resultHandler) -> {
				if (resultHandler.failed()) {
					testContext.failNow(resultHandler.cause());
					return;
				}

				this.webClient
						.put(this.port, this.url,
								Routes.API_PRODUCT_BY_ID.replace(":id", String.valueOf(product.getId())))
						.send(testContext.succeeding((resp) -> {
							testContext.verify(
									() -> assertThat(resp.statusCode()).isEqualTo(HttpURLConnection.HTTP_NO_CONTENT));

							JsonArray jsonArray = new JsonArray();
							jsonArray.add(product.getId());

							String sql = SQL_SELECT + " WHERE id = ?";
							sqlConnection.queryWithParams(sql, jsonArray, (result) -> {
								ProductDto[] updatedProduct = result.result().getRows().stream().map(ProductDto::new)
										.toArray(ProductDto[]::new);

								product.setAvailable(false);
								validateProduct(testContext, Arrays.asList(product), updatedProduct);

								testContext.completeNow();
							});
						}));
			});
		});
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

				next.handle(connectionHandler);
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

	private void insert(List<ProductDto> listProducts, SQLConnection connection, Handler<AsyncResult<Integer>> next) {
		List<JsonArray> listJsonArray = listProducts.stream().map((product) -> {
			JsonArray jsonArray = new JsonArray();
			jsonArray.add(product.getId());
			jsonArray.add(product.getName());
			jsonArray.add(product.getBarCode());
			jsonArray.add(product.isAvailable());
			jsonArray.add(product.getSerialNumber());
			return jsonArray;
		}).collect(Collectors.toList());

		connection.batchWithParams(SQL_INSERT, listJsonArray, (result) -> {
			if (result.failed()) {
				next.handle(Future.failedFuture(result.cause()));
				connection.close();
				return;
			}
			next.handle(Future.succeededFuture());
		});
	}

	private void insert(ProductDto product, SQLConnection connection, Handler<AsyncResult<Integer>> next) {
		this.insert(Arrays.asList(product), connection, next);
	}
}
