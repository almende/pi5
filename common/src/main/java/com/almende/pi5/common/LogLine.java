/*
 * Copyright: Almende B.V. (2015), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Class LogLine.
 */
public class LogLine {
	private long	now			= 0;
	private long	timeslot	= 0;
	private String	id			= null;
	private double	current		= 0.0;
	private double	demand		= 0.0;
	private double	expected	= 0.0;
	private double	max			= 0.0;
	private double	min			= 0.0;
	private Double	request		= null;
	private double	nextDemand	= 0.0;
	private double	nextMax		= 0.0;
	private double	nextMin		= 0.0;
	private Double	nextRequest	= null;

	/**
	 * Instantiates a new log line.
	 */
	public LogLine() {}

	/**
	 * Gets the now.
	 *
	 * @return the now
	 */
	public long getNow() {
		return now;
	}

	/**
	 * Sets the now.
	 *
	 * @param now
	 *            the new now
	 */
	public void setNow(long now) {
		this.now = now;
	}

	/**
	 * Gets the timeslot.
	 *
	 * @return the timeslot
	 */
	public long getTimeslot() {
		return timeslot;
	}

	/**
	 * Sets the timeslot.
	 *
	 * @param timeslot
	 *            the new timeslot
	 */
	public void setTimeslot(long timeslot) {
		this.timeslot = timeslot;
	}

	/**
	 * Gets the id.
	 *
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Sets the id.
	 *
	 * @param id
	 *            the new id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Gets the current.
	 *
	 * @return the current
	 */
	public double getCurrent() {
		return current;
	}

	/**
	 * Sets the current.
	 *
	 * @param current
	 *            the new current
	 */
	public void setCurrent(double current) {
		this.current = current;
	}

	/**
	 * Gets the demand.
	 *
	 * @return the demand
	 */
	public double getDemand() {
		return demand;
	}

	/**
	 * Sets the demand.
	 *
	 * @param demand
	 *            the new demand
	 */
	public void setDemand(double demand) {
		this.demand = demand;
	}

	/**
	 * Gets the expected.
	 *
	 * @return the expected
	 */
	public double getExpected() {
		return expected;
	}

	/**
	 * Sets the expected.
	 *
	 * @param expected
	 *            the new expected
	 */
	public void setExpected(double expected) {
		this.expected = expected;
	}

	/**
	 * Gets the max.
	 *
	 * @return the max
	 */
	public double getMax() {
		return max;
	}

	/**
	 * Sets the max.
	 *
	 * @param max
	 *            the new max
	 */
	public void setMax(double max) {
		this.max = max;
	}

	/**
	 * Gets the min.
	 *
	 * @return the min
	 */
	public double getMin() {
		return min;
	}

	/**
	 * Sets the min.
	 *
	 * @param min
	 *            the new min
	 */
	public void setMin(double min) {
		this.min = min;
	}

	/**
	 * Gets the request.
	 *
	 * @return the request
	 */
	public Double getRequest() {
		return request;
	}

	/**
	 * Sets the request.
	 *
	 * @param request
	 *            the new request
	 */
	public void setRequest(Double request) {
		this.request = request;
	}

	/**
	 * Gets the next demand.
	 *
	 * @return the next demand
	 */
	public double getNextDemand() {
		return nextDemand;
	}

	/**
	 * Sets the next demand.
	 *
	 * @param nextDemand
	 *            the new next demand
	 */
	public void setNextDemand(double nextDemand) {
		this.nextDemand = nextDemand;
	}

	/**
	 * Gets the next max.
	 *
	 * @return the next max
	 */
	public double getNextMax() {
		return nextMax;
	}

	/**
	 * Sets the next max.
	 *
	 * @param nextMax
	 *            the new next max
	 */
	public void setNextMax(double nextMax) {
		this.nextMax = nextMax;
	}

	/**
	 * Gets the next min.
	 *
	 * @return the next min
	 */
	public double getNextMin() {
		return nextMin;
	}

	/**
	 * Sets the next min.
	 *
	 * @param nextMin
	 *            the new next min
	 */
	public void setNextMin(double nextMin) {
		this.nextMin = nextMin;
	}

	/**
	 * Gets the next request.
	 *
	 * @return the next request
	 */
	public Double getNextRequest() {
		return nextRequest;
	}

	/**
	 * Sets the next request.
	 *
	 * @param nextRequest
	 *            the new next request
	 */
	public void setNextRequest(Double nextRequest) {
		this.nextRequest = nextRequest;
	}

	/**
	 * From profiles.
	 *
	 * @param id
	 *            the id
	 * @param current
	 *            the current
	 * @param expected
	 *            the expected
	 * @param now
	 *            the now
	 * @param currentTimeslot
	 *            the current timeslot
	 * @param contractMode
	 *            the contract mode
	 * @return the log line
	 */
	public static LogLine fromProfiles(final String id,
			final CategoryProfile current, final CategoryProfile expected,
			final DateTime now, final DateTime currentTimeslot,
			final boolean contractMode) {
		final LogLine result = new LogLine();
		result.setId(id);
		result.setNow(now.getMillis());
		result.setTimeslot(currentTimeslot.getMillis());
		result.setCurrent(current.getDemand().getValueAt(now));
		result.setDemand(current.getDemand().getIntegral(
				currentTimeslot.minusMinutes(15), currentTimeslot));
		result.setExpected(expected.getDemand().getIntegral(
				currentTimeslot.minusMinutes(15), currentTimeslot));
		result.setMax(expected.getExpectedFlexibilityMaxInWatts().getIntegral(
				currentTimeslot.minusMinutes(15), currentTimeslot));
		result.setMin(expected.getExpectedFlexibilityMinInWatts().getIntegral(
				currentTimeslot.minusMinutes(15), currentTimeslot));
		result.setNextDemand(current.getDemand().getIntegral(currentTimeslot,
				currentTimeslot.plusMinutes(15)));
		result.setNextMax(expected.getExpectedFlexibilityMaxInWatts()
				.getIntegral(currentTimeslot, currentTimeslot.plusMinutes(15)));
		result.setNextMin(expected.getExpectedFlexibilityMinInWatts()
				.getIntegral(currentTimeslot, currentTimeslot.plusMinutes(15)));
		if (contractMode) {
			result.setRequest(expected.getDemand().getIntegral(
					currentTimeslot.minusMinutes(15), currentTimeslot));
			result.setNextRequest(expected.getDemand().getIntegral(
					currentTimeslot, currentTimeslot.plusMinutes(15)));
		}
		return result;
	}

	/**
	 * Return as csv log line.
	 *
	 * @return the log line
	 */
	@JsonIgnore
	public String getCsv() {
		final StringBuilder sb = new StringBuilder();

		sb.append(id);
		sb.append(',');
		sb.append(new DateTime(now));
		sb.append(',');
		sb.append(current);
		sb.append(',');
		sb.append(demand);
		sb.append(',');
		sb.append(expected);
		sb.append(',');
		sb.append(max);
		sb.append(',');
		sb.append(min);
		sb.append(',');
		sb.append(request);
		sb.append(',');
		sb.append(nextDemand);
		sb.append(',');
		sb.append(nextMax);
		sb.append(',');
		sb.append(nextMin);
		sb.append(',');
		sb.append(nextRequest);
		sb.append(',');
		sb.append(now);
		sb.append(',');
		sb.append(timeslot);
		sb.append(',');
		sb.append(new DateTime(timeslot));
		sb.append('\n');
		return sb.toString();
	}
}
