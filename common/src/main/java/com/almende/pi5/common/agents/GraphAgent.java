/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common.agents;

import java.net.URI;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.pi5.common.Categories;
import com.almende.pi5.common.CategoryProfile;
import com.almende.pi5.common.ControlMode;
import com.almende.pi5.common.PowerProfile;
import com.almende.pi5.common.PowerTimeLine;
import com.almende.pi5.common.RequestProfile;
import com.almende.util.callback.AsyncCallback;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The {@link GraphAgent}.
 */
public abstract class GraphAgent extends Agent {
	private static final Logger		LOG							= Logger.getLogger(GraphAgent.class
																		.getName());

	protected URI					myParentUrl					= null;
	protected URI					loggerUrl					= null;
	protected DateTime				currentTimeslot				= DateTime
																		.now();
	private int						sendOffset					= 0;
	protected int					sendInterval				= 0;

	protected PowerProfile			currentReport				= null;
	protected final ReentrantLock	currentRepLock				= new ReentrantLock();

	protected static ControlMode	DEFAULT_MODE				= ControlMode.NOMINAL;

	protected ControlMode			modus						= DEFAULT_MODE;

	private static final String		PARENT_URL_KEY				= "parentUrl";
	private static final String		LOGGER_URL_KEY				= "loggerUrl";

	private static final String		SEND_OFFSET_KEY				= "sendOffset";

	private static final int		SEND_OFFSET_DEFAULT			= 0;

	private static final String		SEND_INTERVAL_KEY			= "sendInterval";

	protected static final int		TIMESTEP					= 15;
	protected static final int		TIMESLOTLENGTH				= 15 * 60 * 1000;
	private static final int		SEND_INTERVAL_DEFAULT		= TIMESLOTLENGTH;
	private static final String		repeatUpdateTime			= "repeatUpdateTime";
	protected static final String	sendReport					= "sendReport";
	private static final String		repeatSendReportOn15		= "repeatSendReportOn15";
	private static final String		repeatSendReportOnInterval	= "repeatSendReportOnInterval";

	/**
	 * {@link GraphAgent} zero-arg constructor.
	 */
	protected GraphAgent() {
		super();
	}

	/**
	 * {@link GraphAgent} constructor.
	 *
	 * @param agentId
	 *            the agent id
	 * @param config
	 *            the agent config
	 */
	protected GraphAgent(final String agentId, final ObjectNode config) {
		super(agentId, config);
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.eve.agent.Agent#onBoot()
	 */
	@Override
	protected void onReady() {
		final ObjectNode config = getConfig();

		this.myParentUrl = config.hasNonNull(PARENT_URL_KEY) ? URI
				.create(config.get(PARENT_URL_KEY).asText()) : null;
		this.loggerUrl = config.hasNonNull(LOGGER_URL_KEY) ? URI.create(config
				.get(LOGGER_URL_KEY).asText()) : null;
		this.sendOffset = config.hasNonNull(SEND_OFFSET_KEY) ? config.get(
				SEND_OFFSET_KEY).asInt(SEND_OFFSET_DEFAULT) : 0;
		this.sendInterval = config.hasNonNull(SEND_INTERVAL_KEY) ? config.get(
				SEND_INTERVAL_KEY).asInt(SEND_INTERVAL_DEFAULT) : 0;

		repeatUpdateTime();
		repeatSendReportOn15();
		repeatSendReportOnInterval();

		schedule(sendReport, null, DateTime.now().plusMinutes(1));
		super.onReady();
	}

	/**
	 * updates the current time.
	 */
	@Access(AccessType.PUBLIC)
	public void repeatUpdateTime() {
		schedule("updateTime", null, DateTime.now());
		schedule(repeatUpdateTime, null, 60000);
	}

	/**
	 * Update time.
	 */
	@Access(AccessType.PUBLIC)
	public void updateTime() {
		DateTime now = DateTime.now();
		now = now.plus((TIMESTEP - (now.getMinuteOfHour() % TIMESTEP)) * 60000
				- (now.getSecondOfMinute() * 1000) - now.getMillisOfSecond());
		if (!this.currentTimeslot.equals(now)) {
			this.currentTimeslot = now;
			LOG.fine(getId() + ": updateTime to: " + now);
		}
	}

	/**
	 * Schedule send report on 15 minutes.
	 */
	@Access(AccessType.PUBLIC)
	public void repeatSendReportOn15() {
		LOG.fine(getId() + ": repeatSendReportOn15");
		if (this.sendOffset <= 0) {
			LOG.info("repeatSendReportOn15 stopped");
			return;
		}

		updateTime();

		DateTime next = this.currentTimeslot.plus(this.sendOffset);
		DateTime prev = this.currentTimeslot.minusMinutes(TIMESTEP).plus(
				this.sendOffset);
		if (DateTime.now().isBefore(prev)) {
			next = prev;
		}

		LOG.fine(getId() + ": repeatSendReportOn15, next run at:" + next);
		schedule(sendReport, null, DateTime.now());
		schedule(repeatSendReportOn15, null, next);
	}

	/**
	 * Schedule report on interval.
	 */
	@Access(AccessType.PUBLIC)
	public void repeatSendReportOnInterval() {
		if (this.sendInterval <= 0)
			return;

		LOG.info(getId() + ": repeatSendReportOnInterval, next run at:"
				+ DateTime.now().plus(this.sendInterval));

		updateTime();
		schedule(sendReport, null, DateTime.now());
		schedule(repeatSendReportOnInterval, null, this.sendInterval);
	}

	/**
	 * Update current report.
	 * TODO: still copy reported flexibility and expected demand?
	 *
	 * @param contract
	 *            the contract
	 */
	@Access(AccessType.PUBLIC)
	public void updateCurrentReport(final boolean contract) {
		final PowerProfile aggregate = generateReport();
		currentRepLock.lock();
		if (currentReport == null) {
			currentReport = aggregate;
		} else {
			final PowerProfile newReport = new PowerProfile(currentTimeslot);
			for (Categories cat : Categories.values()) {
				final String category = cat.name();
				if (category.equals(Categories.ALL.name())) {
					continue;
				}
				final CategoryProfile oldCat = aggregate
						.getCategoryProfile(category);
				final CategoryProfile newCat = newReport
						.getCategoryProfile(category);
				newCat.setExpectedFlexibilityMaxInWatts(oldCat
						.getExpectedFlexibilityMaxInWatts().clone()
						.withTimestamp(currentTimeslot));
				newCat.setExpectedFlexibilityMinInWatts(oldCat
						.getExpectedFlexibilityMinInWatts().clone()
						.withTimestamp(currentTimeslot));
				if (!contract) {
					newCat.setDemand(oldCat.getDemand().clone()
							.withTimestamp(currentTimeslot));
				} else {
					newCat.setDemand(currentReport.getCategoryProfile(category)
							.getDemand().clone().withTimestamp(currentTimeslot));
				}
			}
			newReport.calcAll();
			currentReport = newReport;
		}
		// currentReport.calcAll();
		currentRepLock.unlock();
	}

	/**
	 * Send report to my parent agent.
	 */
	@Access(AccessType.PUBLIC)
	public void sendReport() {
		// TODO: limit to 1 per second?!
		if (getConfig().has("skipReports")
				&& getConfig().get("skipReports").asBoolean(false)) {
			LOG.info("Skipping sendReport");
			return;
		}
		try {
			updateCurrentReport(modus.equals(ControlMode.CONTRACT));
			final Params params = new Params();
			params.set("profile", JOM.getInstance().valueToTree(currentReport));
			getSender().get().call(this.myParentUrl, "report", params,
					new AsyncCallback<Void>() {

						@Override
						public void onSuccess(Void result) {
							LOG.info(getId() + ": Reported " + getId()
									+ " flex to: " + myParentUrl + " : "
									+ params);
						}

						@Override
						public void onFailure(Exception exception) {
							LOG.log(Level.WARNING, getId()
									+ ": Failed to report flex to:"
									+ myParentUrl, exception);
						}

					});

		} catch (final Exception e) {
			LOG.log(Level.WARNING, getId()
					+ ": Failed to report flexibility to parent: "
					+ this.myParentUrl, e);
		}
	}

	/**
	 * Called periodically by {@link #sendReport()} for upstream aggregation.
	 *
	 * @return the power profile
	 */
	@Access(AccessType.PUBLIC)
	abstract public PowerProfile generateReport();

	/**
	 * Creates the proposal.
	 *
	 * @param report
	 *            the report
	 * @param request
	 *            the request
	 * @return the power profile
	 */
	@Access(AccessType.PUBLIC)
	public PowerProfile createProposal(
			final @Name("report") PowerProfile report,
			final @Name("request") RequestProfile request) {

		final PowerProfile result = new PowerProfile(request.getTimestamp());
		final PowerTimeLine pt = request.getRequest().clone()
				.zeroBefore(currentTimeslot.minus(TIMESLOTLENGTH))
				.zeroFrom(currentTimeslot.plus(TIMESLOTLENGTH));
		if (pt.getValueAt(currentTimeslot.minus(TIMESLOTLENGTH)) == 0) {
			pt.merge(report.getCategoryProfile(Categories.ALL.name())
					.getDemand(), currentTimeslot.minus(TIMESLOTLENGTH),
					currentTimeslot);
		}
		final CategoryProfile cat = result
				.getCategoryProfile(Categories.SIMULATED.name());
		cat.setDemand(pt.compact());
		cat.setExpectedFlexibilityMaxInWatts(report
				.getCategoryProfile(Categories.ALL.name())
				.getExpectedFlexibilityMaxInWatts().clone()
				.max(cat.getDemand())
				.zeroBefore(currentTimeslot.minus(TIMESLOTLENGTH))
				.zeroFrom(currentTimeslot.plus(TIMESLOTLENGTH)).compact());
		cat.setExpectedFlexibilityMinInWatts(report
				.getCategoryProfile(Categories.ALL.name())
				.getExpectedFlexibilityMinInWatts().clone()
				.min(cat.getDemand())
				.zeroBefore(currentTimeslot.minus(TIMESLOTLENGTH))
				.zeroFrom(currentTimeslot.plus(TIMESLOTLENGTH)).compact());
		result.calcAll();
		try {
			LOG.info(getId() + ": Creating proposal for request:"
					+ JOM.getInstance().writeValueAsString(request)
					+ " lead to: "
					+ JOM.getInstance().writeValueAsString(result));
		} catch (Exception e) {
			LOG.log(Level.WARNING, "?", e);
		}
		return result;
	}

	/**
	 * Request.
	 *
	 * @param request
	 *            the request
	 * @param doReply
	 *            the do reply
	 */
	@Access(AccessType.PUBLIC)
	public void request(@Name("request") RequestProfile request,
			@Optional @Name("doReply") Boolean doReply) {
		LOG.info(getId() + ": Request received at Graph level!" + request);

		if (modus.equals(ControlMode.ABSTAIN)) {
			LOG.info(getId() + ": ignoring request! Free Running.");
			sendReport();
			return;
		}
		currentRepLock.lock();
		if (currentReport == null) {
			currentReport = generateReport();
		}
		currentReport.merge(createProposal(currentReport, request),
				currentTimeslot.minus(TIMESLOTLENGTH),
				currentTimeslot.plus(TIMESLOTLENGTH)).dropHistory(
				currentTimeslot.minus(TIMESLOTLENGTH));
		currentRepLock.unlock();
		setModus(ControlMode.CONTRACT);
	}

	/**
	 * Do compare.
	 *
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @param now
	 *            the now
	 * @param aggregate
	 *            the aggregate
	 * @return the diff
	 */
	public Double getDiff(final DateTime start, final DateTime end,
			final DateTime now, final PowerProfile aggregate) {
		if (currentReport == null) {
			return 0.0;
		}

		currentRepLock.lock();
		final Double goal = currentReport
				.getCategoryProfile(Categories.ALL.name()).getDemand()
				.getIntegral(start, end);
		final Double actual = aggregate
				.getCategoryProfile(Categories.ALL.name()).getDemand()
				.getIntegral(start, end);
		currentRepLock.unlock();

		long seconds = new Duration(now, end).getStandardSeconds();
		if (seconds <= 0) {
			seconds = 1;
		}
		return (goal - actual) / seconds;
	}

	/**
	 * Gets the flex up.
	 *
	 * @param now
	 *            the now
	 * @param aggregate
	 *            the aggregate
	 * @return the flex up
	 */
	public Double getFlexUp(final DateTime now, final PowerProfile aggregate) {
		if (currentReport == null) {
			return 0.0;
		}

		currentRepLock.lock();
		final Double actual = aggregate
				.getCategoryProfile(Categories.ALL.name()).getDemand()
				.getValueAt(now);

		final Double max = aggregate.getCategoryProfile(Categories.ALL.name())
				.getExpectedFlexibilityMaxInWatts().getValueAt(now);
		currentRepLock.unlock();

		return max - actual;
	}

	/**
	 * Gets the flex down.
	 *
	 * @param now
	 *            the now
	 * @param aggregate
	 *            the aggregate
	 * @return the flex down
	 */
	public Double getFlexDown(final DateTime now, final PowerProfile aggregate) {
		if (currentReport == null) {
			return 0.0;
		}

		currentRepLock.lock();
		final Double actual = aggregate
				.getCategoryProfile(Categories.ALL.name()).getDemand()
				.getValueAt(now);
		final Double min = aggregate.getCategoryProfile(Categories.ALL.name())
				.getExpectedFlexibilityMinInWatts().getValueAt(now);
		currentRepLock.unlock();

		return min - actual;
	}

	/**
	 * Gets the goal.
	 *
	 * @param now
	 *            the now
	 * @return the goal
	 */
	protected Double getGoal(final DateTime now) {
		if (currentReport == null) {
			return 0.0;
		}
		currentRepLock.lock();
		currentReport.calcAll();
		final Double goal = currentReport
				.getCategoryProfile(Categories.ALL.name()).getDemand()
				.getValueAt(now);
		currentRepLock.unlock();
		return goal;
	}

	/**
	 * Sets the modus.
	 *
	 * @param modus
	 *            the new modus
	 */
	@Access(AccessType.PUBLIC)
	public void setModus(@Name("modus") final ControlMode modus) {
		this.modus = modus;
	}

	/**
	 * Gets the modus.
	 *
	 * @return the modus
	 */
	@Access(AccessType.PUBLIC)
	public ControlMode getModus() {
		return this.modus;
	}

}
