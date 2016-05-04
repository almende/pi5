package com.almende.pi5.ach;
/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */


import com.almende.pi5.common.agents.AggregatingAgent;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class {@link MVAgent}.
 */
public class MVAgent extends AggregatingAgent {
	// private static final Logger LOG =
	// Logger.getLogger(MVAgent.class.getName());

	/**
	 * Instantiates a new {@link MVAgent}.
	 */
	public MVAgent() {
		super();
	}

	/**
	 * Instantiates a new {@link MVAgent}.
	 *
	 * @param id
	 *            the id
	 * @param config
	 *            the config
	 */
	public MVAgent(String id, ObjectNode config) {
		super(id, config);
	}

}
