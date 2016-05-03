/*
 * $Id: 97ccac5f2e86225a9c1381441f691b5a7dbae305 $
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

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * {@link FlexDirection}.
 *
 * @author <a href="mailto:rick@almende.org">Rick</a>
 * @version $Id: 97ccac5f2e86225a9c1381441f691b5a7dbae305 $
 * @date $Date$
 */
public enum FlexDirection {
	/** Upward flexibility (lux, temp, ...) */
	UPWARD("up"),

	/** Downward flexibility (lux, temp, ...) */
	DOWNWARD("down"),

	;

	/** the JSON representation */
	private final String	jsonValue;

	/**
	 * {@link FlexDirection} constructor
	 * 
	 * @param jsonValue
	 *            the JSON representation
	 */
	private FlexDirection(final String jsonValue) {
		this.jsonValue = jsonValue;
	}

	/**
	 * For value.
	 *
	 * @param value
	 *            the value
	 * @return the flex direction
	 */
	@JsonCreator
	public static FlexDirection forValue(final String value) {
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
	 * Map of.
	 *
	 * @param up
	 *            the up
	 * @param down
	 *            the down
	 * @return the map
	 */
	public static Map<FlexDirection, FlexRange> mapOf(final FlexRange up,
			final FlexRange down) {
		final Map<FlexDirection, FlexRange> result = new EnumMap<>(
				FlexDirection.class);
		result.put(FlexDirection.DOWNWARD, down);
		result.put(FlexDirection.UPWARD, up);
		return result;
	}

	/** new Integer[] { 0, 0 } */
	public static final Map<FlexDirection, FlexRange>	NOFLEX		= Collections
																			.unmodifiableMap(mapOf(
																					FlexRange.NONE,
																					FlexRange.NONE));

	/** new Integer[] { 3, 3 } */
	public static final Map<FlexDirection, FlexRange>	FULLFLEX	= Collections
																			.unmodifiableMap(mapOf(
																					FlexRange.FULL,
																					FlexRange.FULL));

}
