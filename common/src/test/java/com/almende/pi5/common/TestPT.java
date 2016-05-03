/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.common;

import java.util.ArrayList;

import junit.framework.TestCase;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * The Class TestPT.
 */
public class TestPT extends TestCase {

	/**
	 * Test PowerTimeLine logic
	 *
	 * @throws JsonProcessingException
	 *             the json processing exception
	 */
	@Test
	public void testPT() throws JsonProcessingException {
		final PowerTimeLine test0 = new PowerTimeLine();
		test0.setTimestamp(DateTime.parse("2016-01-01T00:00:00Z"));

		final PowerTimeLine test1 = new PowerTimeLine();
		test1.setTimestamp(test0.getTimestamp());
		final ArrayList<PowerTime> series1 = new ArrayList<PowerTime>();
		series1.add(new PowerTime(0, 10));
		series1.add(new PowerTime(1000, 20));
		series1.add(new PowerTime(10000, 40));
		series1.add(new PowerTime(33000, 20));
		series1.add(new PowerTime(41000, 10));
		test1.setSeries(series1);

		final PowerTimeLine test2 = new PowerTimeLine();
		test2.setTimestamp(test0.getTimestamp());
		final ArrayList<PowerTime> series2 = new ArrayList<PowerTime>();
		series2.add(new PowerTime(0, 15));
		series2.add(new PowerTime(3000, 20));
		series2.add(new PowerTime(10000, 10));
		series2.add(new PowerTime(35000, 30));
		series2.add(new PowerTime(40000, 20));
		test2.setSeries(series2);

		final PowerTimeLine test3 = new PowerTimeLine();
		test3.setTimestamp(test0.getTimestamp().plusMillis(10000));
		final ArrayList<PowerTime> series3 = new ArrayList<PowerTime>();
		series3.add(new PowerTime(0, 15));
		series3.add(new PowerTime(3000, 20));
		series3.add(new PowerTime(10000, 10));
		series3.add(new PowerTime(25000, 30));
		series3.add(new PowerTime(40000, 20));
		test3.setSeries(series3);

		assertEquals("{\"timestamp\":1451606400000,\"series\":[]}", JOM
				.getInstance().writeValueAsString(test0));
		assertEquals(
				"{\"timestamp\":1451606400000,\"series\":[{\"value\":10.0,\"offset\":0},{\"value\":20.0,\"offset\":1000},{\"value\":40.0,\"offset\":10000},{\"value\":20.0,\"offset\":33000},{\"value\":10.0,\"offset\":41000}]}",
				JOM.getInstance().writeValueAsString(test1));
		assertEquals(JOM.getInstance().writeValueAsString(test1), JOM
				.getInstance().writeValueAsString(test0.add(test1)));
		assertEquals(
				"{\"timestamp\":1451606400000,\"series\":[{\"value\":25.0,\"offset\":0},{\"value\":35.0,\"offset\":1000},{\"value\":40.0,\"offset\":3000},{\"value\":50.0,\"offset\":10000},{\"value\":30.0,\"offset\":33000},{\"value\":50.0,\"offset\":35000},{\"value\":40.0,\"offset\":40000},{\"value\":30.0,\"offset\":41000}]}",
				JOM.getInstance().writeValueAsString(test1.add(test2)));
		assertEquals(JOM.getInstance().writeValueAsString(test1), JOM
				.getInstance().writeValueAsString(test1.compact()));
		assertEquals(1785.0, test3.getIntegral(
				test0.getTimestamp().minus(100000),
				test0.getTimestamp().plus(100000)));
		assertEquals(8.925, test3.getAverageWatts(
				test0.getTimestamp().minus(100000),
				test0.getTimestamp().plus(100000)));

		final PowerTimeLine test4 = new PowerTimeLine();
		final ArrayList<PowerTime> series4 = new ArrayList<PowerTime>();
		series4.add(new PowerTime(0, 15));
		test4.setSeries(series4);

		assertEquals(
				"[{\"value\":0.0,\"offset\":-10000},{\"value\":0.0,\"offset\":-9000},{\"value\":0.0,\"offset\":-8000},{\"value\":0.0,\"offset\":-7000},{\"value\":0.0,\"offset\":-6000},{\"value\":0.0,\"offset\":-5000},{\"value\":0.0,\"offset\":-4000},{\"value\":0.0,\"offset\":-3000},{\"value\":0.0,\"offset\":-2000},{\"value\":0.0,\"offset\":-1000},{\"value\":15.0,\"offset\":0},{\"value\":15.0,\"offset\":1000},{\"value\":15.0,\"offset\":2000},{\"value\":15.0,\"offset\":3000},{\"value\":15.0,\"offset\":4000},{\"value\":15.0,\"offset\":5000},{\"value\":15.0,\"offset\":6000},{\"value\":15.0,\"offset\":7000},{\"value\":15.0,\"offset\":8000},{\"value\":15.0,\"offset\":9000},{\"value\":0.0,\"offset\":10000}]",
				JOM.getInstance()
						.writeValueAsString(
								test4.getDiscreteSeries(
										DateTime.now().minus(10000), DateTime
												.now().plus(10000),
										new Duration(1000))));

		final DateTime now = DateTime.now();

		final PowerTimeLine test5 = new PowerTimeLine();
		final ArrayList<PowerTime> series5 = new ArrayList<PowerTime>();
		series5.add(new PowerTime(500, 15));
		test5.setSeries(series5);
		test5.discrete(now.minus(10000), now.plus(10000), new Duration(1000));

		assertEquals(
				"{\"timestamp\":"
						+ now.getMillis()
						+ ",\"series\":[{\"value\":0.0,\"offset\":-10000},{\"value\":7.5,\"offset\":0},{\"value\":15.0,\"offset\":1000},{\"value\":0.0,\"offset\":10000}]}",
				JOM.getInstance().writeValueAsString(test5.compact()));

		assertEquals(0.0, test5.getValueAt(now.minus(10001)));
		assertEquals(0.0, test5.getValueAt(now.minus(10000)));
		assertEquals(7.5, test5.getValueAt(now));
		assertEquals(15.0, test5.getValueAt(now.plus(1001)));
		assertEquals(0.0, test5.getValueAt(now.plus(10000)));
	}
}
