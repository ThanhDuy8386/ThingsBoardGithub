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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.thingsboard.common.util.JacksonUtil;
import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;

/**
 * Custom Rule Node: Temperature Converter
 * 
 * Converts temperature between Celsius and Fahrenheit
 * 
 * @author Your Name
 */
@Slf4j
@RuleNode(
        type = ComponentType.TRANSFORMATION,
        name = "temperature converter",
        configClazz = TbTemperatureConverterNodeConfiguration.class,
        nodeDescription = "Convert temperature between Celsius and Fahrenheit",
        nodeDetails = """
                This node converts temperature values between Celsius and Fahrenheit.
                
                <b>Configuration:</b>
                <ul>
                  <li><b>Input Field:</b> The field name containing the temperature value (e.g., 'temperature', 'temp').</li>
                  <li><b>Output Field:</b> The field name for the converted value (e.g., 'temperatureFahrenheit').</li>
                  <li><b>Conversion Mode:</b> 
                    <ul>
                      <li><code>CELSIUS_TO_FAHRENHEIT</code> - Convert °C to °F using formula: °F = (°C × 9/5) + 32</li>
                      <li><code>FAHRENHEIT_TO_CELSIUS</code> - Convert °F to °C using formula: °C = (°F - 32) × 5/9</li>
                    </ul>
                  </li>
                  <li><b>Round Decimals:</b> Number of decimal places (0-5). Default is 2.</li>
                </ul>
                
                <b>Example:</b><br/>
                Input message: <code>{"temperature": 25.5}</code><br/>
                Configuration: inputField='temperature', outputField='tempFahrenheit', mode='CELSIUS_TO_FAHRENHEIT'<br/>
                Output message: <code>{"temperature": 25.5, "tempFahrenheit": 77.9}</code>
                
                <br/><br/>
                <b>Output connections:</b> <code>Success</code>, <code>Failure</code>
                """,
        uiResources = {"static/rulenode/custom-nodes-config.js"},
        configDirective = "tbTransformationNodeTemperatureConverterConfig",
        icon = "thermostat",
        docUrl = "https://thingsboard.io/docs/user-guide/rule-engine-2-0/transformation-nodes/#temperature-converter"
)
public class TbTemperatureConverterNode implements TbNode {

    private TbTemperatureConverterNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, TbTemperatureConverterNodeConfiguration.class);
        
        // Validate configuration
        if (config.getInputField() == null || config.getInputField().trim().isEmpty()) {
            throw new TbNodeException("Input field name cannot be empty!", true);
        }
        if (config.getOutputField() == null || config.getOutputField().trim().isEmpty()) {
            throw new TbNodeException("Output field name cannot be empty!", true);
        }
        if (config.getRoundDecimals() < 0 || config.getRoundDecimals() > 5) {
            throw new TbNodeException("Round decimals must be between 0 and 5!", true);
        }
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        try {
            // Parse message payload
            ObjectNode msgData = (ObjectNode) JacksonUtil.toJsonNode(msg.getData());
            
            // Get input value
            JsonNode inputNode = msgData.get(config.getInputField());
            if (inputNode == null || !inputNode.isNumber()) {
                ctx.tellFailure(msg, new TbNodeException(
                    "Input field '" + config.getInputField() + "' not found or not a number!"));
                return;
            }
            
            double inputValue = inputNode.asDouble();
            
            // Convert temperature
            double outputValue = convertTemperature(inputValue, config.getConversionMode());
            
            // Round to specified decimal places
            outputValue = roundValue(outputValue, config.getRoundDecimals());
            
            // Add converted value to message
            msgData.put(config.getOutputField(), outputValue);
            
            // Create new message with updated data
            TbMsg outMsg = ctx.transformMsg(msg, msg.getType(), msg.getOriginator(), 
                                           msg.getMetaData(), JacksonUtil.toString(msgData));
            
            // Log if debug enabled
            if (log.isDebugEnabled()) {
                log.debug("Temperature conversion: {} {} = {} {}",
                        inputValue, 
                        config.getConversionMode().getInputUnit(),
                        outputValue,
                        config.getConversionMode().getOutputUnit());
            }
            
            ctx.tellSuccess(outMsg);
            
        } catch (Exception e) {
            log.error("Failed to convert temperature", e);
            ctx.tellFailure(msg, e);
        }
    }

    /**
     * Convert temperature based on mode
     */
    private double convertTemperature(double value, ConversionMode mode) {
        return switch (mode) {
            case CELSIUS_TO_FAHRENHEIT -> celsiusToFahrenheit(value);
            case FAHRENHEIT_TO_CELSIUS -> fahrenheitToCelsius(value);
        };
    }

    /**
     * Convert Celsius to Fahrenheit: °F = (°C × 9/5) + 32
     */
    private double celsiusToFahrenheit(double celsius) {
        return (celsius * 9.0 / 5.0) + 32.0;
    }

    /**
     * Convert Fahrenheit to Celsius: °C = (°F - 32) × 5/9
     */
    private double fahrenheitToCelsius(double fahrenheit) {
        return (fahrenheit - 32.0) * 5.0 / 9.0;
    }

    /**
     * Round value to specified decimal places
     */
    private double roundValue(double value, int decimals) {
        if (decimals == 0) {
            return Math.round(value);
        }
        double scale = Math.pow(10, decimals);
        return Math.round(value * scale) / scale;
    }

    @Override
    public void destroy() {
        // Cleanup if needed
    }
}
