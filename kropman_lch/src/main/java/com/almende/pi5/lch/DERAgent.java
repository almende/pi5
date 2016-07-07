/*
 * Copyright: Almende B.V. (2015), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */
package com.almende.pi5.lch;

/*
 * Copyright: Almende B.V. (2014), Rotterdam, The Netherlands
 * License: The Apache Software License, Version 2.0
 */

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.almende.eve.agent.Agent;
import com.almende.eve.protocol.jsonrpc.annotation.Access;
import com.almende.eve.protocol.jsonrpc.annotation.AccessType;
import com.almende.eve.protocol.jsonrpc.annotation.Name;
import com.almende.eve.protocol.jsonrpc.formats.Params;
import com.almende.util.URIUtil;
import com.almende.util.callback.AsyncCallback;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The Class DERAgent.
 */
@Access(AccessType.PUBLIC)
public class DERAgent extends Agent {
	private static final Logger	LOG	= Logger.getLogger(DERAgent.class.getName());

	/**
	 * Switch lamp.
	 *
	 * @param state
	 *            the state (1: on, 0: off)
	 */
	public void switchLamp(@Name("state") int state) {
		// var params = {LibraryName:'Lamp303',Parameters:{'Adres': 4221,
		// 'AanUit':1}};

		final Params params = new Params();
		final Params subparams = new Params();

		params.put("LibraryName", "Lamp303");
		subparams.put("Adres", 4221);
		subparams.put("AanUit", state);
		params.set("Parameters", subparams);

		try {
			call(URIUtil.create("ws://10.14.10.76:3000/agents/remoteGuy"),
					"ExecLibrary", params, new AsyncCallback<ObjectNode>() {

						/* (non-Javadoc)
						 * @see com.almende.util.callback.AsyncCallback#onSuccess(java.lang.Object)
						 */
						@Override
						public void onSuccess(ObjectNode result) {
							LOG.info("Received result:" + result.toString());
						}

						@Override
						public void onFailure(Exception exception) {
							LOG.log(Level.SEVERE, "Failed to call remoteGuy:",
									exception);
						}

					});
		} catch (IOException e) {
			LOG.log(Level.SEVERE, "Failed to call remoteGuy:", e);
		}
	}

}
