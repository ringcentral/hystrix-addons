package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.exception.HystrixBadRequestException;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("WeakerAccess")
public class Command extends HystrixCommand<Integer> {

    private static final AtomicInteger uniqueId = new AtomicInteger(0);
    private final String arg;
    private final HystrixEventType executionResult;
    private final int executionLatency;
    private final HystrixEventType fallbackExecutionResult;
    private final int fallbackExecutionLatency;

    private Command(Setter setter, HystrixEventType executionResult, int executionLatency, String arg,
                    HystrixEventType fallbackExecutionResult, int fallbackExecutionLatency) {
        super(setter);
        this.executionResult = executionResult;
        this.executionLatency = executionLatency;
        this.fallbackExecutionResult = fallbackExecutionResult;
        this.fallbackExecutionLatency = fallbackExecutionLatency;
        this.arg = arg;
    }

    public static Command from(HystrixCommandGroupKey groupKey, HystrixCommandKey key, HystrixEventType desiredEventType, int latency) {
        return from(groupKey, key, desiredEventType, latency, HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
    }

    public static Command from(HystrixCommandGroupKey groupKey, HystrixCommandKey key, HystrixEventType desiredEventType, int latency,
                        HystrixCommandProperties.ExecutionIsolationStrategy isolationStrategy) {
        return from(groupKey, key, desiredEventType, latency, isolationStrategy, HystrixEventType.FALLBACK_SUCCESS, 0);
    }

    public static Command from(HystrixCommandGroupKey groupKey, HystrixCommandKey key, HystrixEventType desiredEventType, int latency,
                        HystrixCommandProperties.ExecutionIsolationStrategy isolationStrategy,
                        HystrixEventType desiredFallbackEventType, @SuppressWarnings("SameParameterValue") int fallbackLatency) {
        Setter setter = Setter.withGroupKey(groupKey)
                .andCommandKey(key)
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionTimeoutInMilliseconds(600)
                        .withExecutionIsolationStrategy(isolationStrategy)
                        .withCircuitBreakerEnabled(true)
                        .withCircuitBreakerRequestVolumeThreshold(3)
                        .withMetricsHealthSnapshotIntervalInMilliseconds(100)
                        .withMetricsRollingStatisticalWindowInMilliseconds(1000)
                        .withMetricsRollingStatisticalWindowBuckets(10)
                        .withRequestCacheEnabled(true)
                        .withRequestLogEnabled(true)
                        .withFallbackIsolationSemaphoreMaxConcurrentRequests(5))
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(groupKey.name()))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withCoreSize(10)
                        .withMaxQueueSize(-1));

        String uniqueArg;

        switch (desiredEventType) {
            case SUCCESS:
                uniqueArg = uniqueId.incrementAndGet() + "";
                return new Command(setter, HystrixEventType.SUCCESS, latency, uniqueArg, desiredFallbackEventType, 0);
            case FAILURE:
                uniqueArg = uniqueId.incrementAndGet() + "";
                return new Command(setter, HystrixEventType.FAILURE, latency, uniqueArg, desiredFallbackEventType, fallbackLatency);
            case TIMEOUT:
                uniqueArg = uniqueId.incrementAndGet() + "";
                return new Command(setter, HystrixEventType.SUCCESS, 700, uniqueArg, desiredFallbackEventType, fallbackLatency);
            case BAD_REQUEST:
                uniqueArg = uniqueId.incrementAndGet() + "";
                return new Command(setter, HystrixEventType.BAD_REQUEST, latency, uniqueArg, desiredFallbackEventType, 0);
            case RESPONSE_FROM_CACHE:
                String arg = uniqueId.get() + "";
                return new Command(setter, HystrixEventType.SUCCESS, 0, arg, desiredFallbackEventType, 0);
            default:
                throw new IllegalStateException("not supported yet");
        }
    }

    @Override
    protected Integer run() throws Exception {
        try {
            Thread.sleep(executionLatency);
            switch (executionResult) {
                case SUCCESS:
                    return 1;
                case FAILURE:
                    throw new IntendedException("induced failure");
                case BAD_REQUEST:
                    throw new HystrixBadRequestException("induced bad request");
                default:
                    throw new IllegalStateException("unhandled HystrixEventType : " + executionResult);
            }
        } catch (InterruptedException ex) {
            System.out.println("Received InterruptedException : " + ex);
            throw ex;
        }
    }

    @Override
    protected Integer getFallback() {
        try {
            Thread.sleep(fallbackExecutionLatency);
        } catch (InterruptedException ex) {
            // do nothing
        }
        switch (fallbackExecutionResult) {
            case FALLBACK_SUCCESS:
                return -1;
            case FALLBACK_FAILURE:
                throw new IntendedException("induced failure");
            case FALLBACK_MISSING:
                throw new UnsupportedOperationException("fallback not defined");
            default:
                throw new IllegalStateException("unhandled HystrixEventType : " + fallbackExecutionResult);
        }
    }

    @Override
    protected String getCacheKey() {
        return arg;
    }

    private static class IntendedException extends RuntimeException {

        public IntendedException(String message) {
            super(message);
        }
    }
}
