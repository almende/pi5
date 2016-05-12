/*
 * Copyright: Almende B.V. (2015), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.ach;

/*
 * Copyright: Almende B.V. (2015), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.annotation.Optional;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.pi5.common.ControlMode;
import com.almende.util.TypeUtil;
import com.almende.util.URIUtil;

/**
 * The Class ScenarioAgent.
 */
@Access(AccessType.PUBLIC)
public class ScenarioAgent extends Agent {
	private static final Logger						LOG			= Logger.getLogger(ScenarioAgent.class
																		.getName());
	private final static Map<String, String>		SIMURIS		= new HashMap<String, String>();
	private final static Map<String, URI>			BUSURIS		= new HashMap<String, URI>();
	private final static Map<String, Set<String>>	HOLISTICS	= new HashMap<String, Set<String>>();
	private final static Map<String, Set<String>>	DERS		= new HashMap<String, Set<String>>();
	private final static URI						LOGGER		= URIUtil
																		.create("local:logger");

	static {
		SIMURIS.put("bus_6", "http://localhost:8084/agents/");
		SIMURIS.put("bus_5", "http://localhost:8085/agents/");
		SIMURIS.put("bus_11", "http://localhost:8086/agents/");
		SIMURIS.put("bus_3", "http://localhost:8087/agents/");
		BUSURIS.put("bus_3", URIUtil.create("local:bus_3"));
		BUSURIS.put("bus_5", URIUtil.create("local:bus_5"));
		BUSURIS.put("bus_6", URIUtil.create("local:bus_6"));
		BUSURIS.put("bus_11", URIUtil.create("local:bus_11"));
	}

	/*
	 * (non-Javadoc)
	 * @see com.almende.eve.agent.AgentCore#onReady()
	 */
	public void onReady() {

	}

	/**
	 * Load holistics.
	 */
	public void loadHolistics() {
		for (Entry<String, String> entry : SIMURIS.entrySet()) {
			final String baseUrl = entry.getValue();
			final URI address = URIUtil.create(baseUrl + "mgr");
			try {
				final Set<String> simNames = callSync(address,
						"getHolisticAgents", null,
						new TypeUtil<Set<String>>() {});
				HOLISTICS.put(entry.getKey(), simNames);
			} catch (IOException e) {
				LOG.log(Level.WARNING, "Couldn't get Holistic agents for: "
						+ entry.getKey(), e);
			}
		}
	}

	/**
	 * Gets the holistics.
	 *
	 * @param bus
	 *            the bus
	 * @return the holistics
	 */
	public Set<String> getHolistics(@Name("bus") final String bus) {
		if (HOLISTICS.isEmpty()) {
			loadHolistics();
		}
		return HOLISTICS.get(bus);
	}

	/**
	 * Gets the holistic uris.
	 *
	 * @param bus
	 *            the bus
	 * @return the holistic uris
	 */
	public List<URI> getHolisticUris(@Name("bus") final String bus) {
		final Set<String> holistics = getHolistics(bus);
		final List<URI> result = new ArrayList<URI>(holistics.size());
		final String baseUri = SIMURIS.get(bus);
		for (String hol : holistics) {
			result.add(URIUtil.create(baseUri + hol));
		}
		return result;
	}

	/**
	 * Load ders.
	 */
	public void loadDers() {
		for (Entry<String, String> entry : SIMURIS.entrySet()) {
			final String baseUrl = entry.getValue();
			final URI address = URIUtil.create(baseUrl + "mgr");
			try {
				final Set<String> simNames = callSync(address, "getDERAgents",
						null, new TypeUtil<Set<String>>() {});
				DERS.put(entry.getKey(), simNames);
			} catch (IOException e) {
				LOG.log(Level.WARNING,
						"Couldn't get Der agents for: " + entry.getKey(), e);
			}
		}
	}

	/**
	 * Gets the ders.
	 *
	 * @param bus
	 *            the bus
	 * @return the ders
	 */
	public Set<String> getDers(@Name("bus") final String bus) {
		if (DERS.isEmpty()) {
			loadDers();
		}
		return DERS.get(bus);
	}

	/**
	 * Gets the der uris.
	 *
	 * @param bus
	 *            the bus
	 * @return the der uris
	 */
	public List<URI> getDerUris(@Name("bus") final String bus) {
		final Set<String> ders = getDers(bus);
		final List<URI> result = new ArrayList<URI>(ders.size());
		final String baseUri = SIMURIS.get(bus);
		for (String der : ders) {
			result.add(URIUtil.create(baseUri + der));
		}
		return result;
	}

	/**
	 * Gets the all uris.
	 *
	 * @return the all uris
	 */
	public List<URI> getAllUris() {
		List<URI> result = new ArrayList<URI>();
		result.addAll(BUSURIS.values());
		for (String bus : SIMURIS.keySet()) {
			result.addAll(getHolisticUris(bus));
			result.addAll(getDerUris(bus));
		}
		return result;
	}

	private void applyToAll(final String method, final Params params,
			final boolean incBusses, final boolean incHols,
			final boolean incDers, final String busFilter) {
		if (incBusses) {
			for (Entry<String, URI> bus : BUSURIS.entrySet()) {
				if (busFilter == null || bus.getKey().startsWith(busFilter)) {
					try {
						call(bus.getValue(), method, params);
					} catch (IOException e) {
						LOG.log(Level.WARNING, "Failed to run:'" + method
								+ "' on:" + bus, e);
					}
				}
			}
		}
		for (String bus : SIMURIS.keySet()) {
			if (busFilter == null || bus.startsWith(busFilter)) {
				if (incHols) {
					for (URI hol : getHolisticUris(bus)) {
						try {
							call(hol, method, params);
						} catch (IOException e) {
							LOG.log(Level.WARNING, "Failed to run:'" + method
									+ "' on:" + hol, e);
						}
					}
				}
				if (incDers) {
					for (URI der : getDerUris(bus)) {
						try {
							call(der, method, params);
						} catch (IOException e) {
							LOG.log(Level.WARNING, "Failed to run:'" + method
									+ "' on:" + der, e);
						}
					}
				}
			}
		}
	}

	/**
	 * call sendReport on all busses.
	 *
	 * @param bus
	 *            the bus
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void sendReport(@Optional @Name("bus") final String bus)
			throws IOException {
		applyToAll("sendReport", null, false, false, true, bus);
		applyToAll("sendReport", null, false, true, false, bus);
		applyToAll("sendReport", null, true, false, false, bus);
	}

	/**
	 * Sets the modus on all agents.
	 *
	 * @param modus
	 *            the new modus
	 * @param bus
	 *            the bus
	 */
	public void setModus(@Name("modus") final ControlMode modus,
			@Optional @Name("bus") final String bus) {
		final Params params = new Params();
		params.add("modus", modus);
		applyToAll("setModus", params, true, true, true, bus);
	}

	/**
	 * Repeat user events in all DERs!.
	 *
	 * @param bus
	 *            the bus
	 */
	public void repeatUserEvents(@Optional @Name("bus") final String bus) {
		final Params params = new Params();
		params.add("events", true);
		applyToAll("doUserEvents", params, false, false, true, bus);
	}

	/**
	 * Sets the random offset in all DERs!.
	 *
	 * @param randomOffset
	 *            the new random offset
	 * @param bus
	 *            the bus
	 */
	public void setRandomOffset(@Name("offset") double randomOffset,
			@Optional @Name("bus") final String bus) {
		final Params params = new Params();
		params.add("offset", randomOffset);
		applyToAll("setRandomOffset", params, false, false, true, bus);
	}

	/**
	 * Reset demo.
	 */
	public void resetDemo() {
		Params params = new Params();
		params.add("modus", ControlMode.NOMINAL.toString());
		applyToAll("setModus", params, true, true, true, null);
		applyToAll("reset", null, true, true, false, null);
		
		params = new Params();
		params.add("events", false);
		applyToAll("doUserEvents", params, false, false, true, null);

		params = new Params();
		params.add("offset", 0);
		applyToAll("setRandomOffset", params, false, false, true, null);
		applyToAll("setTrickOffset", params, false, false, true, null);
		applyToAll("setGenerationOffset", params, false, false, true, null);

		try {
			call(LOGGER, "clear", null);
		} catch (IOException e) {
			LOG.log(Level.WARNING, "Couldn't clear logger", e);
		}

	}
}
