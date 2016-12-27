package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCollapser;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherCommand;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisherThreadPool;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class ComposedHystrixMetricsPublisher extends HystrixMetricsPublisher {

    private final List<HystrixMetricsPublisher> publishers;

    public ComposedHystrixMetricsPublisher(HystrixMetricsPublisher... publishers) {
        this.publishers = Arrays.asList(publishers);
    }

    @Override
    public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey commandGroupKey, HystrixCommandMetrics metrics, HystrixCircuitBreaker circuitBreaker, HystrixCommandProperties properties) {
        HystrixMetricsPublisherCommand[] array = publishers.stream().map(p ->
                p.getMetricsPublisherForCommand(commandKey, commandGroupKey, metrics, circuitBreaker, properties)).toArray(HystrixMetricsPublisherCommand[]::new);
        return () -> Stream.of(array).forEach(HystrixMetricsPublisherCommand::initialize);
    }

    @Override
    public HystrixMetricsPublisherThreadPool getMetricsPublisherForThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolMetrics metrics, HystrixThreadPoolProperties properties) {
        HystrixMetricsPublisherThreadPool[] array = publishers.stream().map(p ->
                p.getMetricsPublisherForThreadPool(threadPoolKey, metrics, properties)).toArray(HystrixMetricsPublisherThreadPool[]::new);
        return () -> Stream.of(array).forEach(HystrixMetricsPublisherThreadPool::initialize);
    }

    @Override
    public HystrixMetricsPublisherCollapser getMetricsPublisherForCollapser(HystrixCollapserKey collapserKey, HystrixCollapserMetrics metrics, HystrixCollapserProperties properties) {
        HystrixMetricsPublisherCollapser[] array = publishers.stream().map(p ->
                p.getMetricsPublisherForCollapser(collapserKey, metrics, properties)).toArray(HystrixMetricsPublisherCollapser[]::new);
        return () -> Stream.of(array).forEach(HystrixMetricsPublisherCollapser::initialize);
    }
}
