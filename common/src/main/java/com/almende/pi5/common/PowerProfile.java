/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common;

import java.util.HashMap;
import java.util.Map;

import org.joda.time.DateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Class EnergyProfile.
 */
public class PowerProfile {

	private DateTime						timestamp	= new DateTime();
	/* Outside temperature */
	private double							temperature;
	private PowerTimeLine					controlMode	= new PowerTimeLine(
																timestamp);
	private Map<String, CategoryProfile>	reports		= new HashMap<String, CategoryProfile>();

	/**
	 * Instantiates a new energy profile.
	 */
	public PowerProfile() {};

	/**
	 * Instantiates a new power profile.
	 *
	 * @param timestamp
	 *            the timestamp
	 */
	public PowerProfile(final DateTime timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Gets the category report.
	 *
	 * @param category
	 *            the category
	 * @return the category report
	 */
	@JsonIgnore
	public CategoryProfile getCategoryProfile(final String category) {
		if (!reports.containsKey(category)) {
			final CategoryProfile report = new CategoryProfile();
			report.setDemand(new PowerTimeLine(timestamp));
			report.setExpectedFlexibilityMaxInWatts(new PowerTimeLine(timestamp));
			report.setExpectedFlexibilityMinInWatts(new PowerTimeLine(timestamp));
			reports.put(category, report);
		}
		return reports.get(category);
	}

	/**
	 * Calc all.
	 *
	 * @return this profile for chaining
	 */
	public PowerProfile calcAll() {
		// TODO: for loop over categories?
		CategoryProfile all = getCategoryProfile(Categories.ALL.name());
		CategoryProfile hvac = getCategoryProfile(Categories.HVAC.name());
		CategoryProfile lighting = getCategoryProfile(Categories.LIGHTING
				.name());
		CategoryProfile other = getCategoryProfile(Categories.OTHER.name());
		CategoryProfile production = getCategoryProfile(Categories.PRODUCTION
				.name());
		CategoryProfile simulated = getCategoryProfile(Categories.SIMULATED
				.name());

		all.setDemand(new PowerTimeLine(timestamp).add(hvac.getDemand())
				.add(lighting.getDemand()).add(other.getDemand())
				.add(production.getDemand()).add(simulated.getDemand())
				.compact());
		all.setExpectedFlexibilityMaxInWatts(new PowerTimeLine(timestamp)
				.add(hvac.getExpectedFlexibilityMaxInWatts())
				.add(lighting.getExpectedFlexibilityMaxInWatts())
				.add(other.getExpectedFlexibilityMaxInWatts())
				.add(production.getExpectedFlexibilityMaxInWatts())
				.add(simulated.getExpectedFlexibilityMaxInWatts()).compact());
		all.setExpectedFlexibilityMinInWatts(new PowerTimeLine(timestamp)
				.add(hvac.getExpectedFlexibilityMinInWatts())
				.add(lighting.getExpectedFlexibilityMinInWatts())
				.add(other.getExpectedFlexibilityMinInWatts())
				.add(production.getExpectedFlexibilityMinInWatts())
				.add(simulated.getExpectedFlexibilityMinInWatts()).compact());
		setCategoryReport(Categories.ALL.name(), all);
		return this;
	}

	/**
	 * Merge other PowerProfile into this one for the given time period.
	 *
	 * @param other
	 *            the other
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @return this profile for chaining
	 */
	public PowerProfile merge(final PowerProfile other, final DateTime start,
			final DateTime end) {
		for (Categories cat : Categories.values()) {
			final String category = cat.name();
			if (category.equals(Categories.ALL.name())) {
				continue;
			}
			final CategoryProfile myCat = getCategoryProfile(category);
			final CategoryProfile otherCat = other.getCategoryProfile(category);

			myCat.getDemand().merge(otherCat.getDemand(), start, end).compact();
			myCat.getExpectedFlexibilityMaxInWatts()
					.merge(otherCat.getExpectedFlexibilityMaxInWatts(), start,
							end).compact();
			myCat.getExpectedFlexibilityMinInWatts()
					.merge(otherCat.getExpectedFlexibilityMinInWatts(), start,
							end).compact();
		}
		calcAll();
		return this;
	}

	/**
	 * Add other PowerProfile to this one
	 *
	 * @param other
	 *            the other
	 * @return this profile for chaining
	 */
	public PowerProfile add(final PowerProfile other) {
		for (Categories cat : Categories.values()) {
			final String category = cat.name();
			if (category.equals(Categories.ALL.name())) {
				continue;
			}
			final CategoryProfile myCat = getCategoryProfile(category);
			final CategoryProfile otherCat = other.getCategoryProfile(category);

			myCat.getDemand().add(otherCat.getDemand()).compact();
			myCat.getExpectedFlexibilityMaxInWatts()
					.add(otherCat.getExpectedFlexibilityMaxInWatts()).compact();
			myCat.getExpectedFlexibilityMinInWatts()
					.add(otherCat.getExpectedFlexibilityMinInWatts()).compact();
		}
		calcAll();
		return this;
	}

	/**
	 * Drop history.
	 *
	 * @param start
	 *            the start
	 * @return this profile for chaining
	 */
	public PowerProfile dropHistory(final DateTime start) {
		for (Categories cat : Categories.values()) {
			final String category = cat.name();
			if (category.equals(Categories.ALL.name())) {
				continue;
			}
			final CategoryProfile myCat = getCategoryProfile(category);
			myCat.getDemand().zeroBefore(start);
			myCat.getExpectedFlexibilityMaxInWatts().zeroBefore(start);
			myCat.getExpectedFlexibilityMinInWatts().zeroBefore(start);
		}
		calcAll();
		return this;
	}

	/**
	 * Sets the category report.
	 *
	 * @param category
	 *            the category
	 * @param report
	 *            the report
	 */
	public void setCategoryReport(final String category,
			final CategoryProfile report) {
		reports.put(category, report);
	}

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
	public void setTimestamp(final DateTime timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Gets the temperature.
	 *
	 * @return the temperature
	 */
	public double getTemperature() {
		return temperature;
	}

	/**
	 * Sets the temperature.
	 *
	 * @param temperature
	 *            the new temperature
	 */
	public void setTemperature(double temperature) {
		this.temperature = temperature;
	}

	/**
	 * Gets the control mode.
	 *
	 * @return the control mode
	 */
	public PowerTimeLine getControlMode() {
		return controlMode;
	}

	/**
	 * Sets the control mode.
	 *
	 * @param controlMode
	 *            the new control mode
	 */
	public void setControlMode(PowerTimeLine controlMode) {
		this.controlMode = controlMode;
	}

	/**
	 * Gets the reports.
	 *
	 * @return the reports
	 */
	public Map<String, CategoryProfile> getReports() {
		return reports;
	}

	/**
	 * Sets the reports.
	 *
	 * @param reports
	 *            the reports
	 */
	public void setReports(Map<String, CategoryProfile> reports) {
		this.reports = reports;
	}

}
