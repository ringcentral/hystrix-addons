package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCollapser;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherThreadPool;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Publisher for metrics that allows to register several publishers and will publish every event into all of them
 * This class is useful to avoid limitations that only 1 plugin can be registered in Hystrix.
 */
public class ComposedHystrixMetricsPublisher extends HystrixMetricsPublisher {

    /** list of registered plugins **/
    private final List<HystrixMetricsPublisher> publishers;

    public ComposedHystrixMetricsPublisher(HystrixMetricsPublisher... publishers) {
        this.publishers = Arrays.asList(publishers);
    }

    @Override
    public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey commandGroupKey, HystrixCommandMetrics metrics, HystrixCircuitBreaker circuitBreaker, HystrixCommandProperties properties) {
        Stream<HystrixMetricsPublisherCommand> stream = publishers.stream().map(p ->
                p.getMetricsPublisherForCommand(commandKey, commandGroupKey, metrics, circuitBreaker, properties));
        return () -> stream.forEach(HystrixMetricsPublisherCommand::initialize);
    }

    @Override
    public HystrixMetricsPublisherThreadPool getMetricsPublisherForThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolMetrics metrics, HystrixThreadPoolProperties properties) {
        Stream<HystrixMetricsPublisherThreadPool> stream = publishers.stream().map(p ->
                p.getMetricsPublisherForThreadPool(threadPoolKey, metrics, properties));
        return () -> stream.forEach(HystrixMetricsPublisherThreadPool::initialize);
    }

    @Override
    public HystrixMetricsPublisherCollapser getMetricsPublisherForCollapser(HystrixCollapserKey collapserKey, HystrixCollapserMetrics metrics, HystrixCollapserProperties properties) {
        Stream<HystrixMetricsPublisherCollapser> stream = publishers.stream().map(p ->
                p.getMetricsPublisherForCollapser(collapserKey, metrics, properties));
        return () -> stream.forEach(HystrixMetricsPublisherCollapser::initialize);
    }
}
