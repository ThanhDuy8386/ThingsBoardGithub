/**
 * Copyright © 2016-2026 The Thingsboard Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.thingsboard.rule.engine.transform;

/**
 * Temperature conversion modes
 */
public enum ConversionMode {
    CELSIUS_TO_FAHRENHEIT("°C", "°F"),
    FAHRENHEIT_TO_CELSIUS("°F", "°C");

    private final String inputUnit;
    private final String outputUnit;

    ConversionMode(String inputUnit, String outputUnit) {
        this.inputUnit = inputUnit;
        this.outputUnit = outputUnit;
    }

    public String getInputUnit() {
        return inputUnit;
    }

    public String getOutputUnit() {
        return outputUnit;
    }
}
