package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.metric.HystrixCommandCompletion;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import org.junit.Before;
import org.junit.Test;
import rx.observers.TestSubscriber;

import static com.netflix.hystrix.HystrixEventType.FAILURE;
import static com.netflix.hystrix.HystrixEventType.SUCCESS;

public class AggregatedHystrixCommandCompletionStreamTest {

    private final static HystrixCommandGroupKey GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("Util");
    private final static HystrixCommandKey COMMAND_1 = HystrixCommandKey.Factory.asKey("Command1");
    private final static HystrixCommandKey COMMAND_2 = HystrixCommandKey.Factory.asKey("Command2");
    private HystrixMetricsInitializationNotifier notifier = new HystrixMetricsInitializationNotifier();

    @Before
    public void setUp() {
        Hystrix.reset();
        HystrixPlugins.reset();
        HystrixRequestContext.initializeContext();
        HystrixPlugins.getInstance().registerMetricsPublisher(notifier);
    }

    @Test
    public void testAggregatedStream() {
        TestSubscriber<HystrixCommandCompletion> subscriber = new TestSubscriber<>();
        new AggregatedHystrixCommandCompletionStream(notifier, t -> true).observe().subscribe(subscriber);
        Command.from(GROUP_KEY, COMMAND_1, SUCCESS, 10).execute();
        Command.from(GROUP_KEY, COMMAND_1, SUCCESS, 10).execute();
        Command.from(GROUP_KEY, COMMAND_1, FAILURE, 10).execute();
        Command.from(GROUP_KEY, COMMAND_1, SUCCESS, 10).execute();
        Command.from(GROUP_KEY, COMMAND_2, SUCCESS, 10).execute();
        Command.from(GROUP_KEY, COMMAND_2, FAILURE, 10).execute();
        Command.from(GROUP_KEY, COMMAND_2, SUCCESS, 10).execute();

        subscriber.assertValueCount(7);
        subscriber.assertNoErrors();
        subscriber.assertNoTerminalEvent();
    }

    @Test
    public void testAggregatedStreamWithFilter() {
        TestSubscriber<HystrixCommandCompletion> subscriber = new TestSubscriber<>();
        new AggregatedHystrixCommandCompletionStream(notifier, c -> c.getCommandKey() == COMMAND_1).observe().subscribe(subscriber);
        Command.from(GROUP_KEY, COMMAND_1, SUCCESS, 10).execute();
        Command.from(GROUP_KEY, COMMAND_2, FAILURE, 10).execute();
        Command.from(GROUP_KEY, COMMAND_1, FAILURE, 10).execute();
        Command.from(GROUP_KEY, COMMAND_1, SUCCESS, 10).execute();

        subscriber.assertValueCount(3);

        Command.from(GROUP_KEY, COMMAND_2, SUCCESS, 10).execute();
        Command.from(GROUP_KEY, COMMAND_2, SUCCESS, 10).execute();

        subscriber.assertValueCount(3);
        subscriber.assertNoErrors();
        subscriber.assertNoTerminalEvent();
    }

}