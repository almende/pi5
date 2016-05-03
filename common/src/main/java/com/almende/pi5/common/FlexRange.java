/*
 * $Id: 9583cd3c0067a45fb400bc8f9bbfd16fdae0d7fd $
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
 * {@link FlexRange} 0/0 = No lux flexibility, 1/1 minor flex up and down, 2/2
 * major flex up and down, 3/3 full flex up and down.
 *
 * @author <a href="mailto:rick@almende.org">Rick</a>
 * @version $Id: 9583cd3c0067a45fb400bc8f9bbfd16fdae0d7fd $
 * @date $Date$
 */
public enum FlexRange {
	/** No flexibility range (lux, temp, ...) */
	NONE("none"),

	/** Minor flexibility range (lux, temp, ...) */
	MINOR("minor"),

	/** Major flexibility range (lux, temp, ...) */
	MAJOR("major"),

	/** Full flexibility range (lux, temp, ...) */
	FULL("full"),

	;

	/** the JSON representation */
	private final String	jsonValue;

	/**
	 * {@link FlexRange} constructor
	 * 
	 * @param jsonValue
	 *            the JSON representation
	 */
	private FlexRange(final String jsonValue) {
		this.jsonValue = jsonValue;
	}

	/**
	 * For value.
	 *
	 * @param value
	 *            the value
	 * @return the flex range
	 */
	@JsonCreator
	public static FlexRange forValue(final String value) {
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
	 * Min.
	 *
	 * @param a
	 *            the a
	 * @param others
	 *            the others
	 * @return the flex range
	 */
	public static FlexRange min(final FlexRange a, final FlexRange... others) {
		FlexRange result = a;
		if (others != null)
			for (FlexRange other : others)
				if (result == null || other.ordinal() < result.ordinal())
					result = other;
		return result;
	}

	/**
	 * Max.
	 *
	 * @param a
	 *            the a
	 * @param others
	 *            the others
	 * @return the flex range
	 */
	public static FlexRange max(final FlexRange a, final FlexRange... others) {
		FlexRange result = a;
		if (others != null)
			for (FlexRange other : others)
				if (result == null || other.ordinal() > result.ordinal())
					result = other;
		return result;
	}

}
