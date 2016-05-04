package com.almende.pi5.lch;
/*
 * Copyright: Almende B.V. (2015), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */


/**
 * The Class Timestep.
 */
public class Timestep {
	private long	secondsOfDay	= 0;
	private double	usagePercentage	= 0;

	/**
	 * Instantiates a new timestep.
	 */
	public Timestep() {}

	/**
	 * Gets the seconds of day.
	 *
	 * @return the seconds of day
	 */
	public long getSecondsOfDay() {
		return secondsOfDay;
	}

	/**
	 * Sets the seconds of day.
	 *
	 * @param secondsOfDay
	 *            the new seconds of day
	 */
	public void setSecondsOfDay(long secondsOfDay) {
		this.secondsOfDay = secondsOfDay;
	}

	/**
	 * Gets the usage percentage.
	 *
	 * @return the usage percentage
	 */
	public double getUsagePercentage() {
		return usagePercentage;
	}

	/**
	 * Sets the usage percentage.
	 *
	 * @param usagePercentage
	 *            the new usage percentage
	 */
	public void setUsagePercentage(double usagePercentage) {
		this.usagePercentage = usagePercentage;
	}
}
