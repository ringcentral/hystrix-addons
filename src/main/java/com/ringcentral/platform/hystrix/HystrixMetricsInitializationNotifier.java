package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * Listener that notifies all the listeners about initialization of new command
 */
public class HystrixMetricsInitializationNotifier extends HystrixMetricsPublisher {

    private static final Logger log = LoggerFactory.getLogger(HystrixMetricsInitializationNotifier.class);
    private final List<HystrixMetricsInitializationListener> listeners = new CopyOnWriteArrayList<>();

    @Override
    public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(
            HystrixCommandKey commandKey,
            HystrixCommandGroupKey commandGroupKey,
            HystrixCommandMetrics metrics,
            HystrixCircuitBreaker circuitBreaker,
            HystrixCommandProperties properties) {
        log.debug("Notify {} listeners for command {} and group {}", listeners.size(),
                safe(commandKey, HystrixKey::name, "null"),
                safe(commandGroupKey, HystrixKey::name, "null"));
        return () -> listeners.forEach(listener -> listener.initialize(metrics));
    }

    public void addListener(HystrixMetricsInitializationListener listener) {
        log.debug("Adding listener to HystrixMetricsInitializationNotifier");
        listeners.add(listener);
    }

    @SuppressWarnings("SameParameterValue")
    private <T, U> U safe(T object, Function<T, U> f, U defaultValue) {
        return Optional.ofNullable(object).map(f).orElse(defaultValue);
    }
}
