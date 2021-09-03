# Events Broker API for Gerrit Code Review

API of a generic events broker for use with Gerrit Code Review.

Enables the de-coupling between Gerrit, plugins and the different implementations
of a generic events broker.

### Stream Events Publisher

It is a quite common use case for consumers of this library to listen for Gerrit
events and to stream them on a specific topic.

Since the implementation of such logic is always the same, this library provides
a generic stream events publisher which will perform the relevant operations.

In order to listen and stream gerrit events, consumers of this API need to
provide a binding for the `StreamEventPublisherConfig` configuration and
`java.util.concurrent.Executor` binding annotated with `StreamEventPublisherExecutor`
annotation. A default single threaded implementation (`StreamEventPublisherExecutor`)
is provided by the library. The last step is to explicitly bind the Stream Events
Publisher, as such:

```java
import com.gerritforge.gerrit.eventbroker.publisher.StreamEventPublisher;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.AbstractModule;

public class SomeModule extends AbstractModule {
    @Override
    protected void configure() {
        long messagePublishingTimeout = 1000L;

        bind(StreamEventPublisherConfig.class)
                .toInstance(new StreamEventPublisherConfig(
                    "name_of_the_stream_events_topic",
                    messagePublishingTimeout));
        
        bind(Executor.class).annotatedWith(StreamEventPublisherExecutor.class).toProvider(StreamEventPublisherExecutorProvider.class);
        DynamicSet.bind(binder(), EventListener.class).to(StreamEventPublisher.class);
    }
}
```

Alternative way to setup Stream Event Publisher is to use default Guice module:

```java

import com.gerritforge.gerrit.eventbroker.publisher.StreamEventPublisherModule;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class SomeModule extends AbstractModule {
    @Override
    protected void configure() {
        long messagePublishingTimeout = 1000L;
        bind(StreamEventPublisherConfig.class)
                .toInstance(new StreamEventPublisherConfig(
                    "name_of_the_stream_events_topic",
                    messagePublishingTimeout));
        install(new StreamEventPublisherModule());
    }
}
```

Note: To avoid message duplication Stream Events Publisher uses [gerrit.instanceId](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html)
and Event.instanceId to filter out forwarded events.

### Broker Metrics

When `StreamEventPublisher` is used user can optionally bind an implementation of
the BrokerMetrics` interface. This will allow to collect metrics about
successful/failure stream events publishing. If no binding is provided default
implementation will skip collecting metrics:

```java
import com.google.gerrit.extensions.registration.DynamicItem;
import com.google.inject.AbstractModule;

public class SomeModule extends AbstractModule {
    @Override
    protected void configure() {
        DynamicItem.bind(binder(), BrokerMetrics.class)
            .to(BrokerMetricsImpl.class)
            .in(Scopes.SINGLETON);
    }
}
```

Note: `BrokerMetrics` implementation must be bound in a plugin module.
