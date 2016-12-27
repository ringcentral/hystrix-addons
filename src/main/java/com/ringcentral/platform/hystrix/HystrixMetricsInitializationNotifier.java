package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link #addListener(HystrixMetricsInitializationListener)} are not thread-safe
 */
public class HystrixMetricsInitializationNotifier extends HystrixMetricsPublisher {

    private final List<HystrixMetricsInitializationListener> listeners = new ArrayList<>();

    @Override
    public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(
            HystrixCommandKey commandKey,
            HystrixCommandGroupKey commandGroupKey,
            HystrixCommandMetrics metrics,
            HystrixCircuitBreaker circuitBreaker,
            HystrixCommandProperties properties) {
        return () -> listeners.forEach(listener -> listener.initialize(metrics));
    }

    public void addListener(HystrixMetricsInitializationListener listener) {
        listeners.add(listener);
    }

}
