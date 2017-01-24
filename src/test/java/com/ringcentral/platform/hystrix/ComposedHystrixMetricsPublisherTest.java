package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.metrics.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class ComposedHystrixMetricsPublisherTest {

    @Before
    public void reset() {
        Hystrix.reset();
    }

    @Test
    public void test() {
        HystrixCommandKey key = HystrixCommandKey.Factory.asKey("key");
        HystrixMetricsPublisherCommand firstCommand = new HystrixMetricsPublisherCommandDefault(key, null, null, null, null);
        DummyPublisher publisher1 = new DummyPublisher(firstCommand);
        TestHystrixMetricsPublisher publisher2 = new TestHystrixMetricsPublisher();
        ComposedHystrixMetricsPublisher composedPublisher = new ComposedHystrixMetricsPublisher(publisher1, publisher2);
        HystrixPlugins.getInstance().registerMetricsPublisher(composedPublisher);

        testSingleInitializePerKey(publisher2);

    }

    private void testSingleInitializePerKey(TestHystrixMetricsPublisher publisher) {
        ArrayList<Thread> threads = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            threads.add(new Thread(() -> {
                HystrixMetricsPublisherFactory.createOrRetrievePublisherForCommand(TestCommandKey.COMMAND_A, null, null, null, null);
                HystrixMetricsPublisherFactory.createOrRetrievePublisherForCommand(TestCommandKey.COMMAND_B, null, null, null, null);
                HystrixMetricsPublisherFactory.createOrRetrievePublisherForThreadPool(TestThreadPoolKey.THREAD_POOL_A, null, null);
            }));
        }

        // start them
        threads.forEach(Thread::start);

        // wait for them to finish
        threads.forEach( t -> {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // we should see 2 commands and 1 threadPool publisher created
        assertEquals(2, publisher.commandCounter.get());
        assertEquals(1, publisher.threadCounter.get());
    }

    private static class TestHystrixMetricsPublisher extends HystrixMetricsPublisher {

        AtomicInteger commandCounter = new AtomicInteger();
        AtomicInteger threadCounter = new AtomicInteger();

        @Override
        public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(HystrixCommandKey commandKey, HystrixCommandGroupKey commandOwner, HystrixCommandMetrics metrics, HystrixCircuitBreaker circuitBreaker, HystrixCommandProperties properties) {
            return commandCounter::incrementAndGet;
        }

        @Override
        public HystrixMetricsPublisherThreadPool getMetricsPublisherForThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixThreadPoolMetrics metrics, HystrixThreadPoolProperties properties) {
            return threadCounter::incrementAndGet;
        }

    }

    private enum TestCommandKey implements HystrixCommandKey {
        COMMAND_A, COMMAND_B
    }

    private enum TestThreadPoolKey implements HystrixThreadPoolKey {
        THREAD_POOL_A
    }


    private static class DummyPublisher extends HystrixMetricsPublisher {

        private HystrixMetricsPublisherCommand commandToReturn;

        DummyPublisher(HystrixMetricsPublisherCommand commandToReturn) {
            this.commandToReturn = commandToReturn;
        }

        @Override
        public HystrixMetricsPublisherCommand getMetricsPublisherForCommand(HystrixCommandKey commandKey,
                                                                            HystrixCommandGroupKey commandGroupKey,
                                                                            HystrixCommandMetrics metrics,
                                                                            HystrixCircuitBreaker circuitBreaker,
                                                                            HystrixCommandProperties properties) {
            return commandToReturn;
        }
    }
}
