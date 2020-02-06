package com.stockmzk.route;

public final class Routes {
	
	private Routes() {
		
	}
	
	public static final String API_PRODUCTS = "/api/products";
	public static final String API_PRODUCT_BY_ID = API_PRODUCTS + "/:id";
	public static final String API_PRODUCTS_AVAILABLE = API_PRODUCTS + "/available";
}
