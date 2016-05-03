/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common;


/**
 * The Class PowerTime.
 */

public class PowerTime {
	private double	value;
	private long	offset;

	/**
	 * Instantiates a new power time.
	 */
	public PowerTime() {}

	/**
	 * Instantiates a new power time.
	 *
	 * @param offset
	 *            the offset (milliseconds!!!!!!)
	 * @param value
	 *            the value (Watt)
	 */
	public PowerTime(final long offset, final double value) {
		this.offset = offset;
		this.value = value;
	}

	/**
	 * Gets the value (Watt).
	 *
	 * @return the value (Watt)
	 */
	public double getValue() {
		return value;
	}

	/**
	 * Sets the value (Watt).
	 *
	 * @param value
	 *            the new value (Watt)
	 */
	public void setValue(double value) {
		if (Double.isNaN(value)){
			value=0;
		}
		if (Double.isInfinite(value)){
			value = Double.MAX_VALUE;
		}
		this.value = value;
	}

	/**
	 * Gets the offset (minutes).
	 *
	 * @return the offset (minutes)
	 */
	public long getOffset() {
		return offset;
	}

	/**
	 * Sets the offset (minutes).
	 *
	 * @param offset
	 *            the new offset (minutes)
	 */
	public void setOffset(long offset) {
		this.offset = offset;
	}

}