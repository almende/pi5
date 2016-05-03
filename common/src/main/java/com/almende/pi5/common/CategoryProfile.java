package com.almende.pi5.common;

/**
 * The Class categoryReport.
 */
public class CategoryProfile {
	private PowerTimeLine	demand							= new PowerTimeLine();
	private PowerTimeLine	expectedFlexibilityMaxInWatts	= new PowerTimeLine();
	private PowerTimeLine	expectedFlexibilityMinInWatts	= new PowerTimeLine();

	/**
	 * Instantiates a new category report.
	 */
	public CategoryProfile() {}

	/**
	 * Gets the demand.
	 *
	 * @return the demand
	 */
	public PowerTimeLine getDemand() {
		return demand;
	}

	/**
	 * Sets the demand.
	 *
	 * @param demand
	 *            the new demand
	 */
	public void setDemand(PowerTimeLine demand) {
		this.demand = demand;
	}

	/**
	 * Gets the expected flexibility max in watts.
	 *
	 * @return the expected flexibility max in watts
	 */
	public PowerTimeLine getExpectedFlexibilityMaxInWatts() {
		return expectedFlexibilityMaxInWatts;
	}

	/**
	 * Sets the expected flexibility max in watts.
	 *
	 * @param expectedFlexibilityMaxInWatts
	 *            the new expected flexibility max in watts
	 */
	public void setExpectedFlexibilityMaxInWatts(
			final PowerTimeLine expectedFlexibilityMaxInWatts) {
		this.expectedFlexibilityMaxInWatts = expectedFlexibilityMaxInWatts;
	}

	/**
	 * Gets the expected flexibility min in watts.
	 *
	 * @return the expected flexibility min in watts
	 */
	public PowerTimeLine getExpectedFlexibilityMinInWatts() {
		return expectedFlexibilityMinInWatts;
	}

	/**
	 * Sets the expected flexibility min in watts.
	 *
	 * @param expectedFlexibilityMinInWatts
	 *            the new expected flexibility min in watts
	 */
	public void setExpectedFlexibilityMinInWatts(
			final PowerTimeLine expectedFlexibilityMinInWatts) {
		this.expectedFlexibilityMinInWatts = expectedFlexibilityMinInWatts;
	}
}
