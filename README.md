# Useful Hystrix addons


[![][travis img]][travis]

1. **ComposedHystrixMetricsPublisher**

According to Hystrix specification it is possible to register only one plugin of every type. This class is supposed to avoid this limitation and register list of plugins.
All of these plugins will be called sequentially in case of Hystrix events. Sometimes it can be useful.

Example:
```java
    HystrixCodaHaleMetricsPublisher plugin1 = new HystrixCodaHaleMetricsPublisher(new MetricRegistry());
    HystrixMetricsInitializationNotifier plugin2 = new HystrixMetricsInitializationNotifier();
    ComposedHystrixMetricsPublisher composedPlugin = new ComposedHystrixMetricsPublisher(plugin1, plugin2);
    HystrixPlugins.getInstance().registerMetricsPublisher(composedPlugin);
```

2. **HystrixAggregatedEventStream**

This event stream aggregates all Hystrix events for every Hystrix Command and exposes it as a rx-java Observable. 
It also dynamically adds lazy-initialized commands so event stream is fully aggregated.

Example:
```java
    HystrixMetricsInitializationNotifier initNotifier = new HystrixMetricsInitializationNotifier();
    HystrixAggregatedEventStream aggregatedStream = new HystrixAggregatedEventStream(initNotifier, m -> true);
    ...
    aggregatedStream.observe().map(<...>).filter(<...>);

```

[travis]:https://travis-ci.org/RC-Platform-Disco-Team/hystrix-addons
[travis img]:https://api.travis-ci.org/RC-Platform-Disco-Team/hystrix-addons.svg?branch=master