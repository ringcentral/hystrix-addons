package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.HystrixCommandMetrics;

public interface HystrixMetricsInitializationListener {

    void initialize(HystrixCommandMetrics metrics);
}
