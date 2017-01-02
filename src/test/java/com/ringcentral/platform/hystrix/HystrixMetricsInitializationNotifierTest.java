package com.ringcentral.platform.hystrix;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.strategy.HystrixPlugins;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.netflix.hystrix.HystrixEventType.FAILURE;
import static com.netflix.hystrix.HystrixEventType.SUCCESS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class HystrixMetricsInitializationNotifierTest {

    private static final Logger log = LoggerFactory.getLogger(HystrixMetricsInitializationNotifierTest.class);
    private final static HystrixCommandGroupKey GROUP_KEY = HystrixCommandGroupKey.Factory.asKey("Util");
    private final static HystrixCommandKey COMMAND_1 = HystrixCommandKey.Factory.asKey("Command1");
    private final static HystrixCommandKey COMMAND_2 = HystrixCommandKey.Factory.asKey("Command2");


    private List<HystrixMetricsInitializationListener> mocks = new ArrayList<HystrixMetricsInitializationListener>(){{
        add(mock(HystrixMetricsInitializationListener.class));
        add(mock(HystrixMetricsInitializationListener.class));
        add(mock(HystrixMetricsInitializationListener.class));
    }};
    private List<HystrixMetricsInitializationListener> listeners = new ArrayList<HystrixMetricsInitializationListener>(){{
        addAll(mocks);
    }};
    private HystrixMetricsInitializationNotifier notifier = new HystrixMetricsInitializationNotifier(){{
        listeners.forEach(this::addListener);
    }};

    @Before
    public void reset() {
        HystrixPlugins.reset();
        mocks.forEach(Mockito::reset);
    }

    @Test
    public void test() {
        mocks.forEach(m -> doAnswer(a -> {
            HystrixCommandMetrics metrics = a.getArgument(0);
            log.info("initialize for command {} and group {}", metrics.getCommandKey().name(), metrics.getCommandGroup().name());
                    return null;
                }).when(m).initialize(any())
        );

        HystrixPlugins.getInstance().registerMetricsPublisher(notifier);

        for (int i = 0; i < 2; i++) {
            HystrixCommand<Integer> cmd = Command.from(GROUP_KEY, COMMAND_1, SUCCESS, 50);
            cmd.observe();
        }

        for (int i = 0; i < 5; i++) {
            HystrixCommand<Integer> cmd = Command.from(GROUP_KEY, COMMAND_2, FAILURE, 60);
            cmd.observe();
        }

        mocks.forEach(m -> {
            verify(m, times(1)).initialize(withKey(COMMAND_1));
            verify(m, times(1)).initialize(withKey(COMMAND_2));
            verifyNoMoreInteractions(m);
        });
    }

    private static HystrixCommandMetrics withKey(HystrixCommandKey key) {
        return argThat(new HystrixCommandMetricsMatcher(key, null));
    }

    private static class HystrixCommandMetricsMatcher implements ArgumentMatcher<HystrixCommandMetrics> {

        private final HystrixCommandKey keyValue;
        private final HystrixCommandGroupKey groupValue;

        private HystrixCommandMetricsMatcher(HystrixCommandKey key, HystrixCommandGroupKey group) {
            this.keyValue = key;
            this.groupValue = group;
        }

        @Override
        public boolean matches(HystrixCommandMetrics arg) {
            return (keyValue == null || arg.getCommandKey() == keyValue)
                    && (groupValue == null || arg.getCommandGroup() == groupValue);
        }
    }
}