/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.component.log;

import java.util.Locale;
import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.LoggingLevel;
import org.apache.camel.spi.CamelLogger;
import org.apache.camel.spi.ExchangeFormatter;
import org.apache.camel.spi.Metadata;
import org.apache.camel.support.DefaultComponent;
import org.apache.camel.support.processor.DefaultExchangeFormatter;
import org.slf4j.Logger;

/**
 * The <a href="http://camel.apache.org/log.html">Log Component</a>
 * is for logging message exchanges via the underlying logging mechanism.
 */
@org.apache.camel.spi.annotations.Component("log")
public class LogComponent extends DefaultComponent {

    @Metadata(label = "advanced")
    private ExchangeFormatter exchangeFormatter;

    public LogComponent() {
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        LoggingLevel level = getLoggingLevel(parameters);
        Logger providedLogger = getLogger(parameters);

        if (providedLogger == null) {
            // try to look up the logger in registry
            Map<String, Logger> availableLoggers = getCamelContext().getRegistry().findByTypeWithName(Logger.class);
            if (availableLoggers.size() == 1) {
                providedLogger = availableLoggers.values().iterator().next();
                log.info("Using custom Logger: {}", providedLogger);
            } else if (availableLoggers.size() > 1) {
                log.info("More than one {} instance found in the registry. Falling back to creating logger from URI {}.", Logger.class.getName(), uri);
            }
        }

        // first, try to pick up the ExchangeFormatter from the registry
        ExchangeFormatter localFormatter = getCamelContext().getRegistry().lookupByNameAndType("logFormatter", ExchangeFormatter.class);
        if (localFormatter != null) {
            setProperties(localFormatter, parameters);
        } else if (exchangeFormatter != null) {
            // do not set properties, the exchangeFormatter is explicitly set, therefore the
            // user would have set its properties explicitly too
            localFormatter = exchangeFormatter;
        }

        LogEndpoint endpoint = new LogEndpoint(uri, this);
        endpoint.setLevel(level.name());
        endpoint.setLocalFormatter(localFormatter);
        if (providedLogger == null) {
            endpoint.setLoggerName(remaining);
        } else {
            endpoint.setProvidedLogger(providedLogger);
        }
        setProperties(endpoint, parameters);

        return endpoint;
    }

    /**
     * Gets the logging level, will default to use INFO if no level parameter provided.
     */
    protected LoggingLevel getLoggingLevel(Map<String, Object> parameters) {
        String levelText = getAndRemoveParameter(parameters, "level", String.class, "INFO");
        return LoggingLevel.valueOf(levelText.toUpperCase(Locale.ENGLISH));
    }

    /**
     * Gets optional {@link Logger} instance from parameters. If non-null, the provided instance will be used as
     * {@link Logger} in {@link CamelLogger}
     *
     * @param parameters the parameters
     * @return the Logger object from the parameter
     */
    protected Logger getLogger(Map<String, Object> parameters) {
        return getAndRemoveOrResolveReferenceParameter(parameters, "logger", Logger.class);
    }

    public ExchangeFormatter getExchangeFormatter() {
        return exchangeFormatter;
    }

    /**
     * Sets a custom {@link ExchangeFormatter} to convert the Exchange to a String suitable for logging.
     * <p />
     * If not specified, we default to {@link DefaultExchangeFormatter}.
     */
    public void setExchangeFormatter(ExchangeFormatter exchangeFormatter) {
        this.exchangeFormatter = exchangeFormatter;
    }

}
