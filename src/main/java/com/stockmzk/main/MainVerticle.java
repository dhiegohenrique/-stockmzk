package com.stockmzk.main;

import com.stockmzk.route.FailureHandler;
import com.stockmzk.route.IProductRouting;
import com.stockmzk.route.ProductRouting;
import com.stockmzk.route.Routes;
import com.stockmzk.validator.ValidatorHandler;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

public class MainVerticle extends AbstractVerticle {

	private final int port = 8080;

	private IProductRouting productRouting;

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		this.productRouting = new ProductRouting(this.vertx, this.config());

		checkDatabaseConnection((connectionHandler) -> {
			startServer((serverHandler) -> {
				completeStartup(serverHandler, startPromise);
			});
		}, startPromise);
	}

	@Override
	public JsonObject config() {
		JsonObject jsonObject = context.config();
		if (jsonObject.isEmpty()) {
			String path = "src\\main\\resources\\conf\\config.json";
			jsonObject = new JsonObject(this.vertx.fileSystem().readFileBlocking(path));
		}
		
//		path = "D:\\Usuarios\\Dhiego\\Documents\\Mozaiko\\stockmzk\\src\\main\\resources\\conf\\config.json";
		return jsonObject;
//		JsonObject jsonConfig = new JsonObject();
//		jsonConfig.put("http.port", this.port);
//		jsonConfig.put("url", "jdbc:hsqldb:mem:stockmzk?shutdown=true");
//		jsonConfig.put("driver_class", "org.hsqldb.jdbcDriver");
//		jsonConfig.put("max_pool_size", 30);
//		return jsonConfig;
	}

	private void checkDatabaseConnection(Handler<AsyncResult<SQLConnection>> next, Promise<Void> fut) {
		JDBCClient jdbcClient = JDBCClient.createShared(this.vertx, config());
		jdbcClient.getConnection((connectionHandler) -> {
			if (connectionHandler.failed()) {
				fut.fail(connectionHandler.cause());
				return;
			}

			next.handle(Future.succeededFuture(connectionHandler.result()));
		});
	}

	private void startServer(Handler<AsyncResult<HttpServer>> next) {
		final Router router = Router.router(this.vertx);
		final FailureHandler failureHandler = new FailureHandler();

		router.route(Routes.API_PRODUCTS + "*").handler(BodyHandler.create());

		router.get(Routes.API_PRODUCTS).handler(this.productRouting.getAll()).failureHandler(failureHandler);
		router.get(Routes.API_PRODUCTS_AVAILABLE).handler(this.productRouting.getAllAvailable())
				.failureHandler(failureHandler);

		router.post(Routes.API_PRODUCTS).handler(new ValidatorHandler()).handler((routingContext) -> {
			this.productRouting.insert(routingContext);
		}).failureHandler(failureHandler);

		router.put(Routes.API_PRODUCT_BY_ID).handler((routingContext) -> {
			this.productRouting.setAvailable(routingContext);
		}).failureHandler(failureHandler);

		final HttpServer httpServer = this.vertx.createHttpServer();
		httpServer.requestHandler(router);
		httpServer.listen(config().getInteger("http.port", this.port), next::handle);
	}

	private void completeStartup(AsyncResult<HttpServer> serverHandler, Promise<Void> next) {
		if (serverHandler.succeeded()) {
			next.complete();
			return;
		}

		next.fail(serverHandler.cause());
	}
}
