/*
 * $Id: 72fbf801ce8545c938ec4e5ec00dc3a662903a41 $
 * $URL$
 * Part of the EU project Inertia, see http://www.inertia-project.eu/
 * @license
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy
 * of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * Copyright (c) 2014 Almende B.V.
 */
package com.almende.pi5.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@link ControlMode} described in deliverable D5.2.2 annex G.I.VII Test 07
 *
 * @author <a href="mailto:rick@almende.org">Rick</a>
 * @version $Id: 72fbf801ce8545c938ec4e5ec00dc3a662903a41 $
 * @date $Date$
 */
public enum ControlMode {

	/** No flexible power consumption being offered to power supplier, ignoring requests. (Free Running)*/
	ABSTAIN("Abstain", false),

	/** Flexible power consumption being offered to power supplier, no steering yet */
	NOMINAL("Nominal", false),
	
	/** Room comfort flexibility is minimal (no control / off) */
	VACANT("Vacant", false),

	/** Room comfort flexibility is optimal (partial control) */
	COMFORT("Comfort", true),

	/** Room comfort flexibility is maximal (full control) */
	ECONOMY("Economy", true),

	/** Meeting current flexibility contract request profile */
	CONTRACT("Contract", true),

	/** Meeting current demand forecast request profile */
	BALANCE("Balance", true), ;

	private final boolean	control;

	/** the JSON representation */
	private final String	jsonValue;

	/**
	 * {@link ControlMode} constructor
	 * 
	 * @param jsonValue
	 *            the JSON representation
	 */
	private ControlMode(final String jsonValue, final boolean control) {
		this.control = control;
		this.jsonValue = jsonValue;
	}

	/**
	 * For value.
	 *
	 * @param value
	 *            the value
	 * @return the control mode
	 */
	@JsonCreator
	public static ControlMode forValue(final String value) {
		return value == null ? null : valueOf(value.toUpperCase());
	}

	/**
	 * To value.
	 *
	 * @return the string
	 */
	@JsonValue
	public String toValue() {
		return this.jsonValue;
	}

	/**
	 * Checks for control.
	 *
	 * @return {@code true} iff this mode has control over DER & comfort,
	 *         {@code false} otherwise
	 */
	public boolean hasControl() {
		return this.control;
	}

}
