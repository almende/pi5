/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The Class PowerTimeLine.
 */
public class PowerTimeLine {
	private DateTime				timestamp	= DateTime.now();
	private ArrayList<PowerTime>	series		= new ArrayList<PowerTime>();

	/**
	 * Instantiates a new power time line.
	 */
	public PowerTimeLine() {}

	/**
	 * Instantiates a new power time line.
	 *
	 * @param timestamp
	 *            the timestamp
	 */
	public PowerTimeLine(final DateTime timestamp) {
		this.timestamp = timestamp;
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
	public void setTimestamp(DateTime timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Gets the series.
	 *
	 * @return the series
	 */
	public ArrayList<PowerTime> getSeries() {
		return series;
	}

	/**
	 * Gets the value at.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @return the value at
	 */
	@JsonIgnore
	public Double getValueAt(DateTime timestamp) {
		long offset = new Duration(this.timestamp, timestamp).getMillis();
		PowerTime oldVal = null;
		for (PowerTime pt : series) {
			if (pt.getOffset() > offset) {
				return (oldVal == null) ? 0 : oldVal.getValue();
			}
			oldVal = pt;
		}
		return (oldVal == null) ? 0 : oldVal.getValue();
	}

	/**
	 * Adds the value at.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @param value
	 *            the value
	 * @return the power time line
	 */
	public PowerTimeLine addValueAt(final DateTime timestamp, final double value) {
		long offset = new Duration(this.timestamp, timestamp).getMillis();

		if (series.isEmpty()) {
			series.add(new PowerTime(offset, value));
		} else {
			PowerTime last = series.get(series.size() - 1);
			if (last.getOffset() < offset) {
				series.add(new PowerTime(offset, value));
			} else {
				for (int i = 0; i < series.size(); i++) {
					PowerTime pt = series.get(i);
					if (pt.getOffset() < offset) {
						continue;
					}
					if (pt.getOffset() == offset) {
						pt.setValue(value);
					} else {
						series.add(i, new PowerTime(offset, value));
					}
					break;
				}
			}
		}
		return this;
	}

	/**
	 * Make this TL discrete, returning the TL;
	 * As a side effect, removes values outside the start and end times.
	 *
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @param stepSize
	 *            the step size
	 * @return this
	 */
	@JsonIgnore
	public PowerTimeLine discrete(final DateTime start, final DateTime end,
			final Duration stepSize) {
		final PowerTimeLine newTL = new PowerTimeLine();
		newTL.timestamp = this.timestamp;
		final ArrayList<PowerTime> steps = new ArrayList<PowerTime>();

		long offset = new Duration(timestamp, start).getMillis();
		Interval interval = stepSize.toIntervalFrom(start);
		while (interval.getEnd().isBefore(end)
				|| interval.getEnd().isEqual(end)) {
			steps.add(new PowerTime(offset, 0));
			offset += interval.toDurationMillis();
			interval = stepSize.toIntervalFrom(timestamp
					.plusMillis((int) offset));
		}
		newTL.setSeries(steps);
		this.add(newTL).zeroBefore(start).zeroFrom(end);

		final Duration diff = new Duration(start, end);
		if (series.size() > (diff.getMillis() / stepSize.getMillis())) {
			int index = 0;
			long expectedOffset = new Duration(timestamp, start).getMillis()
					+ stepSize.getMillis();
			while (index < series.size() - 1) {
				PowerTime pt = series.get(index);
				ArrayList<PowerTime> temp = new ArrayList<PowerTime>();

				int nextIndex = index + 1;
				PowerTime next = series.get(nextIndex);
				while (next.getOffset() < expectedOffset) {
					temp.add(next);
					series.remove(nextIndex);
					if (nextIndex == series.size()) {
						break;
					}
					next = series.get(nextIndex);
				}
				if (temp.size() > 0) {
					temp.add(0, pt);
					double integral = getIntegral(pt.getOffset(),
							pt.getOffset() + stepSize.getMillis(), temp);
					series.set(index, new PowerTime(pt.getOffset(), integral
							/ stepSize.getMillis()));
				}
				index++;
				expectedOffset += stepSize.getMillis();
			}
		}

		return this;
	}

	/**
	 * Compact.
	 *
	 * @return the power time line
	 */
	@JsonIgnore
	public PowerTimeLine compact() {
		if (series.size() == 0) {
			return this;
		}
		int index = 0;
		while (index < series.size()) {
			PowerTime pt = series.get(index);
			while ((index < series.size() - 1)
					&& pt.getValue() == series.get(index + 1).getValue()) {
				series.remove(index + 1);
			}
			index++;
		}
		return this;
	}

	/**
	 * Gets the discrete series.
	 *
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @param stepSize
	 *            the step size
	 * @return the discrete series
	 */
	@JsonIgnore
	public ArrayList<PowerTime> getDiscreteSeries(DateTime start, DateTime end,
			Duration stepSize) {
		return this.clone().discrete(start, end, stepSize).getSeries();
	}

	/**
	 * Sets the series.
	 *
	 * @param series
	 *            the new series
	 */
	public void setSeries(ArrayList<PowerTime> series) {
		this.series = series;
	}

	/**
	 * Gets the average watts.
	 *
	 * @param fromDateTime
	 *            the from date time
	 * @param untilDateTime
	 *            the until date time
	 * @return the average watts
	 */
	@JsonIgnore
	public double getAverageWatts(final DateTime fromDateTime,
			final DateTime untilDateTime) {
		final double difference = new Duration(fromDateTime, untilDateTime)
				.getMillis()/1000.0;
		if (difference > 0) {
			return getIntegral(fromDateTime, untilDateTime) / difference;
		} else {
			return 0;
		}
	}

	private double getIntegral(final long from, final long until,
			List<PowerTime> items) {

		if (items.size() == 0) {
			return 0;
		}

		double result = 0;
		long oldOffset = from;
		double val = 0;
		for (PowerTime pt : items) {

			if (pt.getOffset() < from) {
				val = pt.getValue();
				continue;
			}
			if (pt.getOffset() > until) {
				break;
			}
			result += val * (pt.getOffset() - oldOffset);
			val = pt.getValue();
			oldOffset = pt.getOffset();
		}
		result += val * (until - oldOffset);
		return result;

	}

	/**
	 * Gets the integral.
	 *
	 * @param fromDateTime
	 *            the from date time
	 * @param untilDateTime
	 *            the until date time
	 * @return the integral
	 */
	@JsonIgnore
	public double getIntegral(final DateTime fromDateTime,
			final DateTime untilDateTime) {
		final long from = new Duration(timestamp, fromDateTime).getMillis();
		final long until = new Duration(timestamp, untilDateTime).getMillis();
		return getIntegral(from, until, this.series) / 1000.0;
	}

	/**
	 * Merge the two timelines (adding values), returning this one;.
	 *
	 * @param other
	 *            the other
	 * @return the power time line
	 */
	public PowerTimeLine add(final PowerTimeLine other) {
		return operation(new addition(), other);
	}

	/**
	 * Merge the two timelines (minus values), returning this one;.
	 *
	 * @param other
	 *            the other
	 * @return the power time line
	 */
	public PowerTimeLine minus(final PowerTimeLine other) {
		return operation(new difference(), other);
	}

	/**
	 * Merge the two timelines (multiplying values), returning this one;.
	 *
	 * @param other
	 *            the other
	 * @return the power time line
	 */
	public PowerTimeLine multi(final PowerTimeLine other) {
		return operation(new multiply(), other);
	}

	/**
	 * Merge the two timelines (maximum value), returning this one;.
	 *
	 * @param other
	 *            the other
	 * @return the power time line
	 */
	public PowerTimeLine max(final PowerTimeLine other) {
		return operation(new maximum(), other);
	}

	/**
	 * Merge the two timelines (minimum value), returning this one;.
	 *
	 * @param other
	 *            the other
	 * @return the power time line
	 */
	public PowerTimeLine min(final PowerTimeLine other) {
		return operation(new minimum(), other);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public PowerTimeLine clone() {
		final PowerTimeLine result = new PowerTimeLine();
		result.timestamp = this.timestamp;
		for (PowerTime pt : this.series) {
			result.series.add(new PowerTime(pt.getOffset(), pt.getValue()));
		}
		return result;
	}

	private PowerTimeLine operation(operator op, PowerTimeLine other) {
		if (other.series.size() == 0) {
			return this;
		}
		final long offset = new Duration(this.timestamp, other.timestamp)
				.getMillis();

		final ArrayList<PowerTime> result = new ArrayList<PowerTime>();
		if (this.series.size() == 0) {
			for (PowerTime pt : other.series) {
				result.add(new PowerTime(pt.getOffset() + offset, op.doOp(0,
						pt.getValue())));
			}
			this.series = result;
			return this;
		}

		PowerTime val_mine = null;
		PowerTime val_other = null;
		int index_mine = 0;
		int index_other = 0;
		double value_mine = 0;
		double value_other = 0;

		while (index_mine < this.series.size()
				&& index_other < other.series.size()) {
			val_mine = this.series.get(index_mine);
			val_other = other.series.get(index_other);
			switch ((int) Math.signum(val_other.getOffset() + offset
					- val_mine.getOffset())) {
				case 1:
					value_mine = val_mine.getValue();
					result.add(new PowerTime(val_mine.getOffset(), op.doOp(
							value_mine, value_other)));
					index_mine++;
					break;

				case 0:
					value_mine = val_mine.getValue();
					value_other = val_other.getValue();
					result.add(new PowerTime(val_mine.getOffset(), op.doOp(
							value_mine, value_other)));
					index_mine++;
					index_other++;
					break;
				case -1:
					value_other = val_other.getValue();
					result.add(new PowerTime(val_other.getOffset() + offset, op
							.doOp(value_mine, value_other)));
					index_other++;
					break;
			}
		}

		for (int p = index_mine; p < this.series.size(); p++) {
			PowerTime pt = this.series.get(p);
			result.add(new PowerTime(pt.getOffset(), op.doOp(pt.getValue(),
					value_other)));
		}
		for (int p = index_other; p < other.series.size(); p++) {
			PowerTime pt = other.series.get(p);
			result.add(new PowerTime(pt.getOffset() + offset, op.doOp(
					value_mine, pt.getValue())));
		}

		this.series = result;
		return this;
	}

	private interface operator {
		double doOp(final double left, final double right);
	}

	class addition implements operator {
		@Override
		public double doOp(double left, double right) {
			return left + right;
		}
	}

	class difference implements operator {

		@Override
		public double doOp(double left, double right) {
			return left - right;
		}

	}

	class multiply implements operator {

		@Override
		public double doOp(double left, double right) {
			return left * right;
		}

	}

	class maximum implements operator {

		@Override
		public double doOp(double left, double right) {
			return Math.max(left, right);
		}

	}

	class minimum implements operator {

		@Override
		public double doOp(double left, double right) {
			return Math.min(left, right);
		}

	}

	/**
	 * Clear to zero between start (inclusive) and end (exclusive).
	 *
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @return this for chaining
	 */
	public PowerTimeLine zeroBetween(final DateTime start, final DateTime end) {
		if (this.series.size() == 0) {
			return this;
		}
		final long startOffset = new Duration(this.timestamp, start)
				.getMillis();
		final long endOffset = new Duration(this.timestamp, end).getMillis();
		// get current value at end
		final double endVal = getValueAt(end);
		// remove all values with index between start incl and end incl.
		final Iterator<PowerTime> iter = this.series.iterator();
		int index = 0;
		while (iter.hasNext()) {
			final PowerTime item = iter.next();
			final long offset = item.getOffset();
			if (offset < startOffset) {
				index++;
			} else if (offset >= startOffset && offset <= endOffset) {
				iter.remove();
			} else if (offset > endOffset) {
				break;
			}
		}
		// Add zero at start
		this.series.add(index, new PowerTime(startOffset, 0));
		// add current value at end
		this.series.add(index + 1, new PowerTime(endOffset, endVal));

		return this;
	}

	/**
	 * Clear to zero starting at start (inclusive).
	 *
	 * @param start
	 *            the start
	 * @return this for chaining
	 */
	public PowerTimeLine zeroFrom(final DateTime start) {
		if (this.series.size() == 0) {
			return this;
		}
		final long startOffset = new Duration(this.timestamp, start)
				.getMillis();
		// remove all values with index after start incl
		final Iterator<PowerTime> iter = this.series.iterator();
		int index = 0;
		while (iter.hasNext()) {
			final PowerTime item = iter.next();
			final long offset = item.getOffset();
			if (offset < startOffset) {
				index++;
			} else if (offset >= startOffset) {
				iter.remove();
			}
		}
		// Add zero at start
		this.series.add(index, new PowerTime(startOffset, 0));
		return this;
	}

	/**
	 * Clear to zero before start (exclusive).
	 *
	 * @param start
	 *            the start
	 * @return this for chaining
	 */
	public PowerTimeLine zeroBefore(final DateTime start) {
		if (this.series.size() == 0) {
			return this;
		}
		final long startOffset = new Duration(this.timestamp, start)
				.getMillis();
		final double startVal = getValueAt(start);
		// remove all values with index before start incl
		final Iterator<PowerTime> iter = this.series.iterator();
		while (iter.hasNext()) {
			final PowerTime item = iter.next();
			final long offset = item.getOffset();
			if (offset <= startOffset) {
				iter.remove();
			} else {
				break;
			}
		}
		// Add value at start
		this.series.add(0, new PowerTime(startOffset, startVal));
		return this;
	}

	/**
	 * Merge other timeline into this this timeline, replacing all between start
	 * and end timestamps.
	 *
	 * @param other
	 *            the other
	 * @param start
	 *            the start
	 * @param end
	 *            the end
	 * @return this for chaining
	 */
	public PowerTimeLine merge(final PowerTimeLine other, final DateTime start,
			final DateTime end) {
		final PowerTimeLine copy = other.clone().zeroBefore(start)
				.zeroFrom(end);
		this.zeroBetween(start, end).add(copy);
		return this;
	}

	/**
	 * With timestamp.
	 *
	 * @param timestamp
	 *            the timestamp
	 * @return the power time line
	 */
	public PowerTimeLine withTimestamp(DateTime timestamp) {
		final DateTime oldTimestamp = this.timestamp;
		this.timestamp = timestamp;
		if (oldTimestamp != null && !oldTimestamp.equals(timestamp)
				&& series.size() > 0) {
			long diff = new Duration(timestamp, oldTimestamp).getMillis();
			for (PowerTime pt : series) {
				pt.setOffset(pt.getOffset() + diff);
			}
		}
		return this;
	}

	@Override
	public String toString() {
		return JOM.getInstance().valueToTree(this).toString();
	}
}
