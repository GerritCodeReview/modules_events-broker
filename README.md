# Events Broker API for Gerrit Code Review

API of a generic events broker for use with Gerrit Code Review.

Enables the de-coupling between Gerrit, plugins and the different implementations
of a generic events broker.

### Stream Events Publisher

It is a quite common use case for consumers of this library to listen for Gerrit
events and to stream them on a specific topic.

Since the implementation of such logic is always the same, this library provides
a generic stream events publisher, which, if bound, will perform the relevant
operations.

In order to listen and stream gerrit events, consumers of this API just need to
provide a named annotation with the name of the stream events topic and then
explicitly bind the Stream Events Publisher, as such:

```java
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
