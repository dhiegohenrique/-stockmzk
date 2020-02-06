package com.stockmzk.entity;

import io.vertx.core.json.JsonObject;

public class ProductEntity {
	
	private int id;
	private int serialNumber;

	private String name;
	private String barCode;
	
	private boolean available;
	
	public ProductEntity () {
		
	}

	public ProductEntity(JsonObject json) {
		this.setId(json.getInteger("ID"));
		this.setSerialNumber(json.getInteger("SERIALNUMBER"));
		this.setName(json.getString("NAME"));
		this.setBarCode(json.getString("BARCODE"));
		this.setAvailable(json.getBoolean("AVAILABLE"));
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBarCode() {
		return barCode;
	}

	public void setBarCode(String origin) {
		this.barCode = origin;
	}

	public boolean isAvailable() {
		return available;
	}

	public void setAvailable(boolean available) {
		this.available = available;
	}

	public int getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(int serialNumber) {
		this.serialNumber = serialNumber;
	}
}
