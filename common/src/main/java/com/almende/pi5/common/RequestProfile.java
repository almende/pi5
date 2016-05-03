/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common;

import org.joda.time.DateTime;

/**
 * The Class RequestProfile.
 */
public class RequestProfile {
	private DateTime		timestamp	= new DateTime();
	private PowerTimeLine	request		= null;

	/**
	 * Gets the timestamp.
	 *
	 * @return the timestamp
	 */
	public DateTime getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets the timestamp.
	 *
	 * @param timestamp
	 *            the new timestamp
	 */
	public void setTimestamp(DateTime timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Gets the request.
	 *
	 * @return the request
	 */
	public PowerTimeLine getRequest() {
		if (request == null) {
			request = new PowerTimeLine(timestamp);
		}
		return request;
	}

	/**
	 * Sets the request.
	 *
	 * @param request
	 *            the new request
	 */
	public void setRequest(PowerTimeLine request) {
		this.request = request;
	}

}
