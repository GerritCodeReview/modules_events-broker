# Events Broker API for Gerrit Code Review

API of a generic events broker for use with Gerrit Code Review.

Enables the de-coupling between Gerrit, plugins and the different implementations
of a generic events broker.

### Stream Events Publisher

It is a quite common use case for consumers of this library to listen for Gerrit
events and to stream them on a specific topic.

Since the implementation of such logic is always the same, this library provides
a generic stream events publisher which will perform the relevant operations.

In order to listen and stream gerrit events, consumers of this API just need to
provide a named annotation with the name of the stream events topic and then
explicitly bind the Stream Events Publisher, as such:

```java
import com.gerritforge.gerrit.eventbroker.StreamEventPublisher;
import com.google.gerrit.extensions.registration.DynamicSet;
import com.google.gerrit.server.events.EventListener;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class SomeModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(new TypeLiteral<String>() {
        })
                .annotatedWith(Names.named(StreamEventPublisher.STREAM_EVENTS_TOPIC))
                .toInstance("name_of_the_stream_events_topic");

        DynamicSet.bind(binder(), EventListener.class).to(StreamEventPublisher.class);
    }
}
```

Note: To avoid message duplication Stream Events Publisher uses [gerrit.instanceId](https://gerrit-review.googlesource.com/Documentation/config-gerrit.html)
and Event.instanceId to filter out forwarded events. This means that gerrit.instanceId is a mandatory
parameter and events without instanceId will be dropped.