/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.lch;

import com.almende.pi5.common.agents.AggregatingAgent;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class HolisticAgent.
 */
public class HolisticAgent extends AggregatingAgent {

	/**
	 * Instantiates a new holistic agent.
	 */
	public HolisticAgent() {
		super();
	}

	/**
	 * Instantiates a new holistic agent.
	 *
	 * @param agentId
	 *            the agent id
	 * @param config
	 *            the agent config
	 */
	public HolisticAgent(final String agentId, final ObjectNode config) {
		super(agentId, config);
	}

}
