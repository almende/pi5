/*
 * Copyright: Almende B.V. (2015), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.lch;

/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.pi5.common.Categories;
import com.almende.pi5.common.ControlMode;
import com.almende.pi5.common.PowerProfile;
import com.almende.pi5.common.PowerTime;
import com.almende.pi5.common.PowerTimeLine;
import com.almende.pi5.common.agents.GraphAgent;
import com.almende.util.TypeUtil;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class DERAgent.
 */
public class DERSimAgent extends GraphAgent {
	private static final Logger	LOG					= Logger.getLogger(DERSimAgent.class
															.getName());
	private Categories			category			= Categories.SIMULATED;
	private double				maxConsumption		= 0;
	private double				maxFlex				= 100;
	private double				minFlex				= 0;
	private long				forecastHorizon		= 13 * 60 * 60 * 1000;
	private List<Timestep>		timespread			= new ArrayList<Timestep>();

	private double				trickOffset			= 0;
	private double				generationOffset	= 0;
	private double				randomOffset		= 0;
	private double				currentRandomOffset	= 0;

	private boolean				doUserEvents		= false;

	/**
	 * Instantiates a new DER agent.
	 *
	 * @param id
	 *            the id
	 * @param config
	 *            the config
	 */
	public DERSimAgent(String id, ObjectNode config) {
		super(id, config);
		onReady();
	}

	/**
	 * Instantiates a new DER sim agent.
	 */
	public DERSimAgent() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.inertia.lch.DERAgent#onReady()
	 */
	public void onReady() {
		final ObjectNode config = getConfig();
		if (config.has("category")) {
			category = Categories.valueOf(config.get("category").asText());
		}
		if (config.has("maxConsumption")) {
			maxConsumption = config.get("maxConsumption").asInt();
		}
		if (config.has("maxFlex")) {
			maxFlex = config.get("maxFlex").asDouble();
		}
		if (config.has("minFlex")) {
			minFlex = config.get("minFlex").asDouble();
		}
		if (config.has("timespread")) {
			timespread = new TypeUtil<ArrayList<Timestep>>() {}.inject(config
					.get("timespread"));
		}
		if (config.has("forecastHorizon")) {
			forecastHorizon = config.get("forecastHorizon").asLong();
		}
		LOG.warning("onReady called:" + config);
		repeatRandomOffset();

		super.onReady();
	}

	/**
	 * Repeat user events.
	 */
	@Access(AccessType.PUBLIC)
	public void repeatUserEvents() {
		if (doUserEvents) {
			doUserEvent();
			schedule(
					"repeatUserEvents",
					null,
					DateTime.now().plusSeconds(
							new Double(Math.random() * 30).intValue()));
		}
	}

	/**
	 * Change user events.
	 *
	 * @param events
	 *            the events
	 */
	@Access(AccessType.PUBLIC)
	public void doUserEvents(@Name("events") boolean events) {
		LOG.warning(getId() + ": " + (events ? "starting" : "stopping")
				+ " user events...");
		doUserEvents = events;
		if (doUserEvents) {
			repeatUserEvents();
		}
	}

	/**
	 * Repeat random offset.
	 */
	@Access(AccessType.PUBLIC)
	public void repeatRandomOffset() {
		currentRandomOffset = (0.5 - Math.random()) * randomOffset;
		schedule(
				"repeatRandomOffset",
				null,
				DateTime.now().plusSeconds(
						new Double(Math.random() * 60).intValue()));
	}

	/**
	 * Do user event.
	 */
	@Access(AccessType.PUBLIC)
	public void doUserEvent() {
		if (Math.random() > 0.8) {
			final Double oldVal = trickOffset;
			final Double available = maxConsumption - minFlex;
			final Double target = maxConsumption - Math.pow(Math.random(), 2)
					* available;

			trickOffset = target - getGoalConsumption(DateTime.now());
			LOG.warning("New trickOffset:" + trickOffset + " from:" + oldVal);
			sendReport();
		}
	}

	/**
	 * Gets the max consumption.
	 *
	 * @return the max consumption
	 */
	public double getMaxConsumption() {
		return maxConsumption;
	}

	/**
	 * Sets the max consumption.
	 *
	 * @param maxConsumption
	 *            the new max consumption
	 */
	public void setMaxConsumption(double maxConsumption) {
		this.maxConsumption = maxConsumption;
	}

	/**
	 * Gets the max flex.
	 *
	 * @return the max flex
	 */
	public double getMaxFlex() {
		return maxFlex;
	}

	/**
	 * Sets the max flex.
	 *
	 * @param maxFlex
	 *            the new max flex
	 */
	public void setMaxFlex(double maxFlex) {
		this.maxFlex = maxFlex;
	}

	/**
	 * Gets the min flex.
	 *
	 * @return the min flex
	 */
	public double getMinFlex() {
		return minFlex;
	}

	/**
	 * Sets the min flex.
	 *
	 * @param minFlex
	 *            the new min flex
	 */
	public void setMinFlex(double minFlex) {
		this.minFlex = minFlex;
	}

	/**
	 * Gets the timespread.
	 *
	 * @return the timespread
	 */
	public List<Timestep> getTimespread() {
		return timespread;
	}

	/**
	 * Sets the timespread.
	 *
	 * @param timespread
	 *            the new timespread
	 */
	public void setTimespread(List<Timestep> timespread) {
		this.timespread = timespread;
	}

	/**
	 * Sets the category.
	 *
	 * @param category
	 *            the new category
	 */
	public void setCategory(Categories category) {
		this.category = category;
	}

	/**
	 * Gets the category.
	 *
	 * @return the category
	 */
	public Categories getCategory() {
		return category;
	}

	/**
	 * Gets the trick offset.
	 *
	 * @return the trick offset
	 */
	@Access(AccessType.PUBLIC)
	public double getTrickOffset() {
		return trickOffset;
	}

	/**
	 * Sets the trick offset.
	 *
	 * @param trickOffset
	 *            the new trick offset
	 */
	@Access(AccessType.PUBLIC)
	public void setTrickOffset(@Name("offset") double trickOffset) {
		this.trickOffset = trickOffset;
	}

	/**
	 * Gets the random offset.
	 *
	 * @return the random offset
	 */
	@Access(AccessType.PUBLIC)
	public double getRandomOffset() {
		return randomOffset;
	}

	/**
	 * Sets the random offset.
	 *
	 * @param randomOffset
	 *            the new random offset
	 */
	@Access(AccessType.PUBLIC)
	public void setRandomOffset(@Name("offset") double randomOffset) {
		this.randomOffset = randomOffset;
	}

	/**
	 * Gets the trick offset.
	 *
	 * @return the trick offset
	 */
	@Access(AccessType.PUBLIC)
	public double getGenerationOffset() {
		return generationOffset;
	}

	/**
	 * Gets the trick offset.
	 *
	 * @param generationOffset
	 *            the new generation offset
	 */
	@Access(AccessType.PUBLIC)
	public void setGenerationOffset(@Name("offset") double generationOffset) {
		this.generationOffset = generationOffset;
	}

	private double getTimePercentage(DateTime when) {
		final int currentTime = when.getMillisOfDay() / 1000;
		double res = 0;
		for (Timestep ts : timespread) {
			res = ts.getUsagePercentage();
			if (ts.getSecondsOfDay() > currentTime) {
				break;
			}
		}
		return res;
	}

	/**
	 * Gets the current consumption.
	 *
	 * @return the current consumption
	 */
	private Double getConsumption(final DateTime time) {
		final long distance = Math.abs(new Duration(currentTimeslot, time)
				.getMillis());

		double offsetFactor = 1.0;
		if (distance > TIMESLOTLENGTH) {
			offsetFactor = TIMESLOTLENGTH / distance;
		}

		return Math.max(
				0,
				Math.min(maxConsumption, getGoalConsumption(time)
						+ (trickOffset + currentRandomOffset) * offsetFactor));
	}

	private Double getGoalConsumption(final DateTime time) {
		Double val = getGoal(time);
		if (ControlMode.CONTRACT.equals(getModus()) && val > 0) {
			return val;
		} else {
			return getBaseConsumption(time);
		}
	}

	private Double getBaseConsumption(final DateTime time) {
		return ((maxConsumption * getTimePercentage(time)) / 100);
	}

	private double getUpPercentage(final double baseConsumption) {
		final double meanPct = (getMaxFlex() + getMinFlex()) / 2;
		final double curPct = 100 * baseConsumption / maxConsumption;
		if (curPct - meanPct > 15) {
			return 1;
		}
		if (curPct - meanPct > 10) {
			return 5;
		}
		if (curPct - meanPct > 5) {
			return 10;
		}
		return 15;
	}

	private double getDownPercentage(final double baseConsumption) {
		final double meanPct = (getMaxFlex() + getMinFlex()) / 2;
		final double curPct = 100 * baseConsumption / maxConsumption;
		if (meanPct - curPct > 15) {
			return 1;
		}
		if (meanPct - curPct > 10) {
			return 5;
		}
		if (meanPct - curPct > 5) {
			return 10;
		}
		return 15;
	}

	private Double getMaxFlexibility(final double consumption,
			final double baseConsumption) {
		return Math.max(consumption, Math.min(
				(maxConsumption * getMaxFlex() / 100),
				((100 + getUpPercentage(baseConsumption)) / 100)
						* baseConsumption));
	}

	private Double getMinFlexibility(final double consumption,
			final double baseConsumption) {
		return Math
				.min(consumption,
						Math.max(
								(maxConsumption * getMinFlex() / 100)
										- generationOffset,
								(((100 - getDownPercentage(baseConsumption)) / 100) * baseConsumption))
								- generationOffset);

	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.inertia.lch.DERAgent#generateReport()
	 */
	@Override
	public PowerProfile generateReport() {
		final PowerProfile profile = new PowerProfile(currentTimeslot);
		final PowerTimeLine demand = new PowerTimeLine(currentTimeslot);
		final PowerTimeLine max = new PowerTimeLine(currentTimeslot);
		final PowerTimeLine min = new PowerTimeLine(currentTimeslot);
		final String cat = getCategory().name();
		final DateTime now = DateTime.now();

		// Get current report and add the new info/ remove past
		int offset = -TIMESLOTLENGTH;
		long endOffset = new Duration(currentTimeslot,
				currentTimeslot.plus(forecastHorizon)).getMillis();
		while (offset < endOffset) {
			final Double consumption = getConsumption(currentTimeslot
					.plus(offset));
			demand.getSeries().add(new PowerTime(offset, consumption));
			max.getSeries().add(
					new PowerTime(offset, getMaxFlexibility(consumption,
							getBaseConsumption(currentTimeslot.plus(offset)))));
			min.getSeries().add(
					new PowerTime(offset, getMinFlexibility(consumption,
							getBaseConsumption(currentTimeslot.plus(offset)))));
			offset += TIMESLOTLENGTH;
		}
		if (currentReport != null) {
			demand.merge(currentReport.getCategoryProfile(cat).getDemand(),
					currentTimeslot.minus(TIMESLOTLENGTH), now);
			max.merge(currentReport.getCategoryProfile(cat)
					.getExpectedFlexibilityMaxInWatts(), currentTimeslot
					.minus(TIMESLOTLENGTH), now);
			min.merge(currentReport.getCategoryProfile(cat)
					.getExpectedFlexibilityMinInWatts(), currentTimeslot
					.minus(TIMESLOTLENGTH), now);
			LOG.warning("Adding historical data:"
					+ currentTimeslot.minus(TIMESLOTLENGTH) + " -> " + now);
		}

		profile.getCategoryProfile(cat).setDemand(demand.compact());
		if (this.modus.equals(ControlMode.ABSTAIN)) {
			// No flexibility available if not participating.
			profile.getCategoryProfile(cat).setExpectedFlexibilityMaxInWatts(
					demand.compact());
			profile.getCategoryProfile(cat).setExpectedFlexibilityMinInWatts(
					demand.compact());
		} else {
			profile.getCategoryProfile(cat).setExpectedFlexibilityMaxInWatts(
					max.compact());
			profile.getCategoryProfile(cat).setExpectedFlexibilityMinInWatts(
					min.compact());
		}
		return profile;
	}
}
