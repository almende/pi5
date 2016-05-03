/*
 * $Id: f88f25ff0fc7623864377549ee59a20c15384704 $
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
 * {@link FlexAspect}.
 *
 * @author <a href="mailto:rick@almende.org">Rick</a>
 * @version $Id: f88f25ff0fc7623864377549ee59a20c15384704 $
 * @date $Date$
 */
public enum FlexAspect {
	/** Visual flexibility (brightness, lux) */
	VISUAL("visual"),

	/** Thermal flexibility (temperature, Celcius) */
	THERMAL("thermal"),

	/** Occupancy flexibility (crowding, people) */
	OCCUPANCY("occupancy"),

	;

	/** the JSON representation */
	private final String	jsonValue;

	/**
	 * {@link FlexAspect} constructor
	 * 
	 * @param jsonValue
	 *            the JSON representation
	 */
	private FlexAspect(final String jsonValue) {
		this.jsonValue = jsonValue;
	}

	/**
	 * For value.
	 *
	 * @param value
	 *            the value
	 * @return the flex aspect
	 */
	@JsonCreator
	public static FlexAspect forValue(final String value) {
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

}
