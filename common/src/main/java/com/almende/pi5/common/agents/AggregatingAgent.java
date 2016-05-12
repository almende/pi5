/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common.agents;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.joda.time.DateTime;

import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.annotation.Sender;
import com.almende.eve.protocol.jsonrpc.formats.JSONRequest;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.pi5.common.Categories;
import com.almende.pi5.common.CategoryProfile;
import com.almende.pi5.common.ControlMode;
import com.almende.pi5.common.LogLine;
import com.almende.pi5.common.PowerProfile;
import com.almende.pi5.common.PowerTimeLine;
import com.almende.pi5.common.RequestProfile;
import com.almende.util.URIUtil;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class AggregatingAgent.
 */
public abstract class AggregatingAgent extends GraphAgent {
	private static final Logger				LOG				= Logger.getLogger(AggregatingAgent.class
																	.getName());

	private Set<ReportWrap>					reports			= new HashSet<ReportWrap>();
	private final ReentrantReadWriteLock	reportsLock		= new ReentrantReadWriteLock();
	private final ReentrantLock				steeringLock	= new ReentrantLock();
	private double							steerlimit		= 100;

	class ReportWrap implements Comparable<ReportWrap> {
		private String			owner			= null;

		private PowerProfile	report			= null;
		private DateTime		lastAccessed	= null;

		public ReportWrap() {}

		public ReportWrap(final String owner, final PowerProfile report) {
			this.owner = owner;
			this.setReport(report);
			lastAccessed = DateTime.now();
		}

		public int hashCode() {
			return owner.hashCode();
		}

		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			if (o == this) {
				return true;
			}
			if (!(o instanceof ReportWrap)) {
				return false;
			}
			final ReportWrap other = (ReportWrap) o;
			return owner.equals(other.owner);
		}

		@Override
		public int compareTo(ReportWrap o) {
			if (this.equals(o)) {
				return 0;
			}
			return lastAccessed.compareTo(o.lastAccessed);
		}

		public String getOwner() {
			return owner;
		}

		public void setOwner(String owner) {
			this.owner = owner;
		}

		public PowerProfile getReport() {
			lastAccessed = DateTime.now();
			return report;
		}

		@JsonIgnore
		public PowerProfile getReportPeek() {
			return report;
		}

		public void setReport(PowerProfile report) {
			this.report = report;
		}
	}

	/**
	 * Instantiates a new aggregating agent.
	 */
	public AggregatingAgent() {
		super();
	}

	/**
	 * Instantiates a new aggregating agent.
	 *
	 * @param agentId
	 *            the agent id
	 * @param config
	 *            the config
	 */
	public AggregatingAgent(String agentId, ObjectNode config) {
		super(agentId, config);
	}

	@Override
	protected void onReady() {
		this.steerlimit = getConfig().hasNonNull("steerLimit") ? getConfig()
				.get("steerLimit").asDouble() : 100;
		repeatSteer();
		super.onReady();
	}

	/**
	 * Gets the current profile.
	 *
	 * @return the current profile
	 */
	@JsonIgnore
	@Access(AccessType.PUBLIC)
	public CategoryProfile getCurrentProfile() {
		return generateReport().getCategoryProfile(Categories.ALL.name());
	}
	
	/**
	 * Reset.
	 */
	@Access(AccessType.PUBLIC)
	public void reset(){
		reportsLock.writeLock().lock();
		reports.clear();
		reportsLock.writeLock().unlock();
	}

	@Override
	public PowerProfile generateReport() {
		if (reports == null) {
			return null;
		}

		reportsLock.readLock().lock();
		final PowerProfile profile = new PowerProfile(currentTimeslot);
		for (final ReportWrap wrap : reports) {
			final PowerProfile report = wrap.getReportPeek();
			for (Categories cat : Categories.values()) {
				final String category = cat.name();
				if (category.equals(Categories.ALL.name())) {
					continue;
				}
				profile.getCategoryProfile(category).getDemand()
						.add(report.getCategoryProfile(category).getDemand())
						.compact();
				profile.getCategoryProfile(category)
						.getExpectedFlexibilityMaxInWatts()
						.add(report.getCategoryProfile(category)
								.getExpectedFlexibilityMaxInWatts()).compact();
				profile.getCategoryProfile(category)
						.getExpectedFlexibilityMinInWatts()
						.add(report.getCategoryProfile(category)
								.getExpectedFlexibilityMinInWatts()).compact();
			}
		}
		reportsLock.readLock().unlock();
		profile.calcAll();
		return profile;
	}

	/**
	 * Repeat steer.
	 */
	public void repeatSteer() {
		steer();

		DateTime next = this.currentTimeslot.plusMinutes(TIMESTEP - 1)
				.plusSeconds(25);
		DateTime prev = next.minusMinutes(TIMESTEP);
		if (DateTime.now().isBefore(prev)) {
			next = prev;
		}

		schedule(new JSONRequest("repeatSteer", null), next);
	}

	/**
	 * Called (ad hoc or periodically) by downstream agents for later
	 * aggregation by this agent
	 *
	 * @param profile
	 *            the profile to cache
	 * @param senderUrl
	 *            the profile's sender url
	 * @throws JsonProcessingException
	 *             the json processing exception
	 */
	@Access(AccessType.PUBLIC)
	public void report(final @Name("profile") PowerProfile profile,
			final @Sender URI senderUrl) throws JsonProcessingException {
		LOG.warning(getId() + "Receiving report from: " + senderUrl + " : "
				+ JOM.getInstance().valueToTree(profile).toString());
		reportsLock.writeLock().lock();
		final ReportWrap wrap = new ReportWrap(senderUrl.toString(), profile);
		this.reports.remove(wrap);
		this.reports.add(wrap);
		reportsLock.writeLock().unlock();

		steer();
		sendLog();
	}

	@Access(AccessType.PUBLIC)
	@Override
	public void request(@Name("request") RequestProfile request,
			@Optional @Name("doReply") Boolean doReply) {
		steeringLock.lock();
		super.request(request, doReply);
		steeringLock.unlock();
		steer();
		sendLog();
		if (doReply != null && doReply) {
			sendReport();
		}
	}

	/**
	 * Steer.
	 * Run on each incoming report, incoming request.
	 */
	private void steer() {
		if (!ControlMode.CONTRACT.equals(modus)
				&& !ControlMode.BALANCE.equals(modus)) {
			if (!getId().startsWith("bus_")) {
				sendReport();
			}
			return;
		}
		if (!steeringLock.tryLock()) {
			if (steeringLock.hasQueuedThreads()) {
				return;
			}
			steeringLock.lock();
		}
		final DateTime now = DateTime.now();
		if (now.isAfter(currentTimeslot)) {
			updateTime();
		}

		final PowerProfile aggregate = generateReport();

		Double current_diff = getDiff(currentTimeslot.minus(TIMESLOTLENGTH),
				currentTimeslot, now, aggregate);
		Double next_diff = getDiff(currentTimeslot,
				currentTimeslot.plus(TIMESLOTLENGTH), currentTimeslot,
				aggregate);

		if (current_diff.isNaN()) {
			current_diff = 0.0;
		}
		if (next_diff.isNaN()) {
			next_diff = 0.0;
		}

		if (Math.abs(current_diff) < steerlimit
				&& Math.abs(next_diff) < steerlimit) {
			LOG.warning(getId() + ": Below steerlimit of " + steerlimit
					+ " Watts (" + current_diff + " - " + next_diff + "):"
					+ now);
			steeringLock.unlock();
			return;
		}

		// Current timeslot:
		double current_percentage;
		if (current_diff > 0) {
			current_percentage = (current_diff / getFlexUp(now, aggregate)) * 2;
			LOG.log(Level.INFO, getId()
					+ ": request: Currently more power usage needed:"
					+ current_diff + " Watt (" + (current_percentage * 100)
					+ "%):" + currentTimeslot.minus(TIMESLOTLENGTH) + "("
					+ currentTimeslot.minus(TIMESLOTLENGTH).getMillis() + ")");
		} else if (current_diff == 0) {
			current_percentage = 0;
		} else {
			current_percentage = (current_diff / getFlexDown(now, aggregate)) * 2;
			LOG.log(Level.INFO, getId()
					+ ": request: Currently less power usage needed:"
					+ (-current_diff) + " Watt (" + (current_percentage * 100)
					+ "%):" + currentTimeslot.minus(TIMESLOTLENGTH) + "("
					+ currentTimeslot.minus(TIMESLOTLENGTH).getMillis() + ")");
		}
		if (current_percentage > 1) {
			current_percentage = 1;
		}
		if (current_percentage < 0.01) {
			current_percentage = 0.01;
		}

		// Next timeslot:
		double next_percentage;
		if (next_diff > 0) {
			next_percentage = (next_diff / getFlexUp(currentTimeslot, aggregate)) * 2;
			LOG.log(Level.INFO, getId()
					+ ": request: Next timeslot more power usage needed:"
					+ next_diff + " Watt (" + (next_percentage * 100) + "%):"
					+ currentTimeslot + "(" + currentTimeslot.getMillis() + ")");
		} else if (next_diff == 0) {
			next_percentage = 0;
		} else {
			next_percentage = (next_diff / getFlexDown(currentTimeslot,
					aggregate)) * 2;
			LOG.log(Level.INFO,
					getId()
							+ ": request: Next timeslot less power usage needed:"
							+ (-next_diff) + " Watt ("
							+ (next_percentage * 100) + "%):" + currentTimeslot
							+ "(" + currentTimeslot.getMillis() + ")");
		}
		if (next_percentage > 1) {
			next_percentage = 1;
		}
		if (next_percentage < 0.01) {
			next_percentage = 0.01;
		}

		final List<ReportWrap> list = new ArrayList<ReportWrap>(reports.size());
		reportsLock.readLock().lock();
		list.addAll(reports);
		reportsLock.readLock().unlock();

		Collections.sort(list);
		for (ReportWrap wrap : list) {
			if (Math.abs(current_diff) < 0.01 * steerlimit
					&& Math.abs(next_diff) < 0.01 * steerlimit) {
				LOG.warning(getId() + ": done steering:" + current_diff + " - "
						+ next_diff);
				break;
			}
			LOG.warning(getId() + ": still to go:" + current_diff + " - "
					+ next_diff + " checking " + wrap.getOwner());

			String agentUrl = wrap.getOwner();
			PowerProfile report = wrap.getReport();
			report.calcAll();

			// Timeline based:
			PowerTimeLine diffLine = new PowerTimeLine(
					currentReport.getTimestamp());

			CategoryProfile rep = report.getCategoryProfile(Categories.ALL
					.name());
			double influence = 0;
			if (Math.abs(current_diff) >= 0) {
				if (current_diff > 0) {
					final double val = Math.min(current_diff,
							getFlexUp(now, report) * current_percentage);
					current_diff -= val;
					diffLine.addValueAt(now, val);
					influence += val;
				} else {
					final double val = Math.max(current_diff,
							getFlexDown(now, report) * current_percentage);
					current_diff -= val;
					diffLine.addValueAt(now, val);
					influence -= val;
				}
			}
			diffLine.zeroFrom(currentTimeslot);
			if (Math.abs(next_diff) >= 0) {
				if (next_diff > 0) {
					final double val = Math.min(next_diff,
							getFlexUp(currentTimeslot, report)
									* next_percentage);
					next_diff -= val;
					diffLine.addValueAt(currentTimeslot, val);
					influence += val;
				} else {
					final double val = Math.max(next_diff,
							getFlexDown(currentTimeslot, report)
									* next_percentage);
					next_diff -= val;
					diffLine.addValueAt(currentTimeslot, val);
					influence -= val;
				}
			}
			diffLine.zeroFrom(currentTimeslot.plus(TIMESLOTLENGTH));

			if (influence <= 0) {
				LOG.warning(getId()
						+ ": Not sending zero influence request to:" + agentUrl);
				continue;
			}

			LOG.warning(getId() + " diffLine:"
					+ JOM.getInstance().valueToTree(diffLine));
			LOG.warning(getId() + " demand:"
					+ JOM.getInstance().valueToTree(rep.getDemand()));

			RequestProfile subRequest = new RequestProfile();
			subRequest.setTimestamp(currentReport.getTimestamp());
			subRequest.getRequest().add(rep.getDemand()).add(diffLine);

			ObjectNode params = JOM.createObjectNode();
			params.set("request", JOM.getInstance().valueToTree(subRequest));
			try {
				LOG.warning(getId() + ": Sending request to Child:" + agentUrl
						+ " -> " + params);

				reportsLock.writeLock().lock();
				wrap.getReport()
						.merge(createProposal(report, subRequest),
								currentTimeslot.minus(TIMESLOTLENGTH),
								currentTimeslot.plus(TIMESLOTLENGTH))
						.dropHistory(currentTimeslot.minus(TIMESLOTLENGTH));
				reportsLock.writeLock().unlock();
				getSender().get().call(URI.create(agentUrl), "request", params);
			} catch (IOException e) {
				LOG.log(Level.WARNING, getId()
						+ ": Couldn't send profile onwards to:" + agentUrl, e);
			}
		}
		if (Math.abs(current_diff) > steerlimit
				|| Math.abs(next_diff) > steerlimit) {
			LOG.warning(getId()
					+ ": Reached steering limits, resetting goal and reporting max.");
			updateCurrentReport(false);
			sendReport();
		}
		sendLog();
		steeringLock.unlock();
	}

	/**
	 * Describe status.
	 *
	 * @return the status
	 */
	@JsonIgnore
	@Access(AccessType.PUBLIC)
	public String describe() {
		final StringBuilder sb = new StringBuilder();
		final PowerProfile aggregate = generateReport();
		final DateTime now = DateTime.now();
		sb.append(getId());
		sb.append(" ");
		sb.append(DateTime.now());
		sb.append("|");
		sb.append(currentTimeslot);
		sb.append(" ");
		sb.append(getModus());
		sb.append('\n');
		sb.append("T0 c:");
		sb.append(aggregate.getCategoryProfile(Categories.ALL.name())
				.getDemand().getValueAt(now));
		sb.append(" a:");
		sb.append(aggregate
				.getCategoryProfile(Categories.ALL.name())
				.getDemand()
				.getIntegral(currentTimeslot.minus(TIMESLOTLENGTH),
						currentTimeslot)
				/ (TIMESLOTLENGTH / 1000));
		sb.append(" g:");
		sb.append(getGoal(now));
		sb.append(" d:(");
		sb.append(getDiff(currentTimeslot.minus(TIMESLOTLENGTH),
				currentTimeslot, now, aggregate));
		sb.append(")");
		sb.append('\n');
		sb.append("T1 c:");
		sb.append(aggregate.getCategoryProfile(Categories.ALL.name())
				.getDemand().getValueAt(currentTimeslot));
		sb.append(" a:");
		sb.append(aggregate
				.getCategoryProfile(Categories.ALL.name())
				.getDemand()
				.getIntegral(currentTimeslot,
						currentTimeslot.plus(TIMESLOTLENGTH))
				/ (TIMESLOTLENGTH / 1000));
		sb.append(" g:");
		sb.append(getGoal(currentTimeslot));
		sb.append(" d:(");
		sb.append(getDiff(currentTimeslot,
				currentTimeslot.plus(TIMESLOTLENGTH), currentTimeslot,
				aggregate));
		sb.append(")");
		sb.append('\n');
		sb.append(getLogLine(aggregate));
		sb.append('\n');
		sb.append("(nof reports:" + reports.size() + ")");
		return sb.toString();
	}

	/**
	 * Send log.
	 */
	@JsonIgnore
	@Access(AccessType.PUBLIC)
	public void sendLog() {
		final DateTime now = DateTime.now();
		if (now.isAfter(currentTimeslot)) {
			updateTime();
		}
		final CategoryProfile current = generateReport().getCategoryProfile(
				Categories.ALL.name());
		CategoryProfile expected = current;
		if (getModus().equals(ControlMode.CONTRACT)) {
			expected = currentReport.getCategoryProfile(Categories.ALL.name());
		}
		final LogLine ll = LogLine.fromProfiles(getId(), current, expected,
				now, currentTimeslot, getModus().equals(ControlMode.CONTRACT));
		final Params params = new Params();
		params.add("logline", ll);
		try {
			call(loggerUrl, "log", params);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't log!", e);
		}
	}

	/**
	 * Gets the log line.
	 *
	 * @param aggregate
	 *            the aggregate
	 * @return the log line
	 */
	@JsonIgnore
	@Access(AccessType.PUBLIC)
	public String getLogLine(final PowerProfile aggregate) {
		final DateTime now = DateTime.now();
		final CategoryProfile current = aggregate
				.getCategoryProfile(Categories.ALL.name());
		CategoryProfile expected = current;
		if (getModus().equals(ControlMode.CONTRACT)) {
			expected = currentReport.getCategoryProfile(Categories.ALL.name());
		}
		final LogLine ll = LogLine.fromProfiles(getId(), current, expected,
				now, currentTimeslot, getModus().equals(ControlMode.CONTRACT));
		return ll.getCsv();
	}

	/**
	 * Sets the modus.
	 *
	 * @param modus
	 *            the new modus
	 */
	@Access(AccessType.PUBLIC)
	public void setModus(@Name("modus") final ControlMode modus) {
		super.setModus(modus);
		sendLog();
	}

	/**
	 * Sets the modus.
	 *
	 * @param modus
	 *            the new modus
	 */
	@Access(AccessType.PUBLIC)
	public void setAllModus(@Name("modus") final ControlMode modus) {
		setModus(modus);

		final Params params = new Params();
		params.add("modus", modus);

		final List<ReportWrap> list = new ArrayList<ReportWrap>(reports.size());
		reportsLock.readLock().lock();
		list.addAll(reports);
		reportsLock.readLock().unlock();
		for (ReportWrap wrap : list) {
			try {
				call(URIUtil.create(wrap.owner), "setModus", params);
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Couldn't forward setModus", e);
			}
		}
	}

}
