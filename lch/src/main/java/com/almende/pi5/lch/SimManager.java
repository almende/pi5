package com.almende.pi5.lch;

/*
 * Copyright: Almende B.V. (2015), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.agent.AgentBuilder;
import com.almende.eve.agent.AgentConfig;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.util.jackson.JOM;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class SimManager.
 */
@Access(AccessType.PUBLIC)
public class SimManager extends Agent {
	private static final Logger			LOG			= Logger.getLogger(SimManager.class
															.getName());
	private Map<String, DERSimAgent>	DERAgents	= new HashMap<String, DERSimAgent>();
	private Map<String, HolisticAgent>	holAgents	= new HashMap<String, HolisticAgent>();
	private ArrayNode					timespread	= null;
	private ArrayNode					flexspread	= null;

	/**
	 * Gets the holistic agents.
	 *
	 * @return the holistic agents
	 */
	public Set<String> getHolisticAgents() {
		return holAgents.keySet();
	}

	/**
	 * Gets the DER agents.
	 *
	 * @return the DER agents
	 */
	public Set<String> getDERAgents() {
		return DERAgents.keySet();
	}

	private void createAgent(String agentId, AgentConfig config) {
		config.setId(agentId);
		LOG.warning("creating DER sim: " + agentId + " : " + config);
		final DERSimAgent agent = (DERSimAgent) new AgentBuilder().withConfig(
				config).build();
		DERAgents.put(agentId, agent);
	}

	@Override
	protected void onReady() {
		final ObjectNode config = getConfig();
		if (config.has("timeCsv")) {
			try {
				final InputStream is = new FileInputStream(new File(config.get(
						"timeCsv").asText()));
				if (is != null) {
					readTimeSpread(convertStreamToString(is));
				}
			} catch (FileNotFoundException e) {
				LOG.log(Level.WARNING, "Failed to read TimeSpread data.", e);
			}
		}
		if (config.has("flexCsv")) {
			try {
				final InputStream is = new FileInputStream(new File(config.get(
						"flexCsv").asText()));
				if (is != null) {
					readFlexSpread(convertStreamToString(is));
				}
			} catch (FileNotFoundException e) {
				LOG.log(Level.WARNING, "Failed to read FlexSpread data.", e);
			}
		}
		if (config.has("dersCsv")) {
			try {
				final InputStream is = new FileInputStream(new File(config.get(
						"dersCsv").asText()));
				if (is != null) {
					readDers(convertStreamToString(is));
				}
			} catch (FileNotFoundException e) {
				LOG.log(Level.WARNING, "Failed to read Ders data.", e);
			}
		}
	}

	/**
	 * Read ders.
	 *
	 * @param list
	 *            the list
	 */
	public void readDers(@Name("csv") String list) {
		final String[] lines = list.split("\\| ?");
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				continue;
			}
			final String[] fields = line.split(",");
			final String label = fields[0].replace('\n', ' ').trim();

			LOG.warning("Filling building:" + label);

			final AgentConfig holConfig = AgentConfig
					.decorate((ObjectNode) getConfig().get("holConfig"));
			final String holId = label + "_holistic";
			LOG.warning("Creating Holistic agent in sim: " + holId + " : "
					+ holConfig);
			holConfig.setId(holId);
			final HolisticAgent holAgent = (HolisticAgent) new AgentBuilder()
					.withConfig(holConfig).build();
			holAgents.put(holId, holAgent);

			// lights:
			for (int i = 0; i < Integer.valueOf(fields[2]); i++) {

				final AgentConfig derConfig = AgentConfig
						.decorate((ObjectNode) getConfig().get("derConfig"));
				final String agentId = label + "_light_" + i;

				derConfig.put("category", "LIGHTING");
				derConfig.put("maxConsumption", fields[3]);
				derConfig.put("maxFlex", flexspread.get(0).get("max").asText());
				derConfig.put("minFlex", flexspread.get(0).get("min").asText());
				derConfig.set("timespread", timespread.get(0));
				derConfig.put("parentUrl", "local:" + holId);
				createAgent(agentId, derConfig);
			}
			// HVACs:
			for (int i = 0; i < Integer.valueOf(fields[6]); i++) {
				final AgentConfig derConfig = AgentConfig
						.decorate((ObjectNode) getConfig().get("derConfig"));
				final String agentId = label + "_hvac_" + i;

				derConfig.put("category", "HVAC");
				derConfig.put("maxConsumption", fields[7]);
				derConfig.put("maxFlex", flexspread.get(1).get("max").asText());
				derConfig.put("minFlex", flexspread.get(1).get("min").asText());
				derConfig.set("timespread", timespread.get(1));
				derConfig.put("parentUrl", "local:" + holId);
				createAgent(agentId, derConfig);
			}
			// Others:
			for (int i = 0; i < Integer.valueOf(fields[10]); i++) {
				final AgentConfig derConfig = AgentConfig
						.decorate((ObjectNode) getConfig().get("derConfig"));
				final String agentId = label + "_other_" + i;

				derConfig.put("category", "OTHER");
				derConfig.put("maxConsumption", fields[11]);
				derConfig.put("maxFlex", flexspread.get(2).get("max").asText());
				derConfig.put("minFlex", flexspread.get(2).get("min").asText());
				derConfig.set("timespread", timespread.get(2));
				derConfig.put("parentUrl", "local:" + holId);
				createAgent(agentId, derConfig);
			}

		}
	}

	/**
	 * Read time spread.
	 *
	 * @param list
	 *            the list
	 */
	public void readTimeSpread(@Name("csv") String list) {
		// Init list:
		timespread = JOM.createArrayNode();
		final ArrayNode lightSpread = JOM.createArrayNode();
		final ArrayNode hvacSpread = JOM.createArrayNode();
		final ArrayNode otherSpread = JOM.createArrayNode();
		timespread.add(lightSpread);
		timespread.add(hvacSpread);
		timespread.add(otherSpread);

		// parse input
		final String[] lines = list.split("\\| ?");
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				continue;
			}
			final String[] fields = line.split(",");
			final String label = fields[0].replace('\n', ' ').trim();
			LOG.warning("Reading timeline:" + label);
			final double timeOfDay = Double.valueOf(label) * 3600;
			final ObjectNode light = JOM.createObjectNode();
			light.put("secondsOfDay", timeOfDay);
			light.put("usagePercentage", fields[1]);
			lightSpread.add(light);

			final ObjectNode hvac = JOM.createObjectNode();
			hvac.put("secondsOfDay", timeOfDay);
			hvac.put("usagePercentage", fields[2]);
			hvacSpread.add(hvac);

			final ObjectNode other = JOM.createObjectNode();
			other.put("secondsOfDay", timeOfDay);
			other.put("usagePercentage", fields[3]);
			otherSpread.add(other);
		}
	}

	/**
	 * Read flex spread.
	 *
	 * @param list
	 *            the list
	 */
	public void readFlexSpread(@Name("csv") String list) {
		flexspread = JOM.createArrayNode();
		final ObjectNode lightSpread = JOM.createObjectNode();
		final ObjectNode hvacSpread = JOM.createObjectNode();
		final ObjectNode otherSpread = JOM.createObjectNode();
		flexspread.add(lightSpread);
		flexspread.add(hvacSpread);
		flexspread.add(otherSpread);

		// parse input
		final String[] lines = list.split("\\| ?");
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				continue;
			}
			final String[] fields = line.split(",");
			final String label = fields[0].replace('\n', ' ').trim();
			LOG.warning("Reading flexline:" + label);
			lightSpread.put(label, fields[1]);
			hvacSpread.put(label, fields[2]);
			otherSpread.put(label, fields[3]);
		}
	}

	private static String convertStreamToString(InputStream is) {
		final Scanner s = new Scanner(is, "UTF-8");
		s.useDelimiter("\\A");
		final String result = s.hasNext() ? s.next() : "";
		s.close();
		return result;
	}
}
