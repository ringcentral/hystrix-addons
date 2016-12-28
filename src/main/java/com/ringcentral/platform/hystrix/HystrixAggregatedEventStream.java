package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.metric.HystrixCommandCompletion;
import com.netflix.hystrix.metric.HystrixCommandCompletionStream;
import com.netflix.hystrix.metric.HystrixEventStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.util.function.Predicate;

/**
 * Rx-Java event stream that aggregates all events for all commands and dynamically adds new type of events during it's work
 */
public class HystrixAggregatedEventStream implements HystrixMetricsInitializationListener, HystrixEventStream<HystrixCommandCompletion> {

    private static final Logger log = LoggerFactory.getLogger(HystrixAggregatedEventStream.class);
    private final Predicate<HystrixCommandMetrics> filter;
    private final Subject<Observable<HystrixCommandCompletion>, Observable<HystrixCommandCompletion>> streams;
    private final Observable<HystrixCommandCompletion> aggregatedStream;

    public HystrixAggregatedEventStream(HystrixMetricsInitializationNotifier notifier, Predicate<HystrixCommandMetrics> filter) {
        this.filter = filter;
        streams = PublishSubject.<Observable<HystrixCommandCompletion>>create().toSerialized();
        aggregatedStream = Observable.merge(streams).share();
        notifier.addListener(this);
    }

    @Override
    public Observable<HystrixCommandCompletion> observe() {
        return aggregatedStream;
    }

    @Override
    public void initialize(HystrixCommandMetrics metrics) {
        if (filter.test(metrics)) {
            final HystrixCommandKey key = metrics.getCommandKey();
            log.debug("Aggregate stream for command {}", key.name());
            streams.onNext(HystrixCommandCompletionStream.getInstance(key).observe());
        }
    }
}