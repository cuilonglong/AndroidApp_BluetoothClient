package com.viewat.entity;

public class DeviceBean {

	public DeviceBean(String message,boolean isOpen)
	{
		this.message = message;
	}
	
	public String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
}
