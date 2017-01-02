package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Listener that notifies all the listeners about initialization of new command
 *
 * {@link #addListener(HystrixMetricsInitializationListener)} are not thread-safe
 */
public class HystrixMetricsInitializationNotifier extends HystrixMetricsPublisher {

    private static final Logger log = LoggerFactory.getLogger(HystrixMetricsInitializationNotifier.class);
    private final List<HystrixMetricsInitializationListener> listeners = new ArrayList<>();

    @Override
    public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(
            HystrixCommandKey commandKey,
            HystrixCommandGroupKey commandGroupKey,
            HystrixCommandMetrics metrics,
            HystrixCircuitBreaker circuitBreaker,
            HystrixCommandProperties properties) {
        log.trace("Notify {} listeners for command {} and group {}", listeners.size(), commandKey.name(), commandGroupKey.name());
        return () -> listeners.forEach(listener -> listener.initialize(metrics));
    }

    public void addListener(HystrixMetricsInitializationListener listener) {
        listeners.add(listener);
    }

}
