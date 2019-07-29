# akka-frp
Akka + FRP + continuations

This is a library built on top of Akka that enables processing actor messages using FRP style event processing.

## Design principles

The design goal of this library is to combine the paradigms of actor style and FRP style programming, with continuation support added to further simplify encoding appliction flows.

This library is inspired by http://reactors.io, but takes a different approach in that it builts on top of existing libraries, instead of building everything from the ground up. This allows easier integration with existing ecosystems.

#### Akka provides state and concurrency control

Akka is the underlying framework that provides state management, concurrent execution, scalability, and message passing infrastructure.

This library takes full advantage of everything Akka provides, instead of reinventing the wheel.

#### FRP provides event processing

Functional reactive programming (FRP) excels at processing event streams in a type-safe and composable manner.

Our flavor of FRP is completely synchronous - all processing happens on the actor's receive thread, eliminating the need for error prone synchronization code.

#### Continuations provide a natural way to encode state machines

Compiler generated continuations (think async/await but for events instead of futures) further simplify FRP style event processing.

Inspired by the [Deprecating the Observer Pattern](https://infoscience.epfl.ch/record/148043/files/DeprecatingObserversTR2010.pdf) research paper.

## Architecture

### Channels

The library is built around the core concept of a _channel_. A channel is a unidirectional, typed, asynchronous communication mechanism, not unlike Unix pipe/fifo.

A channel has an input and an output. Channel input is essentially a combination of `ActorRef` and an id. Channel output is an FRP `EventStream[T]`. Channel input can be passed around just like `ActorRef`, and anyone can write to it, while channel output is owned by the actor that created the channel.

A channel can be persistent or transient. A persistent channel is like a well known TCP port, and allows to say 'send this message to ChannelX of ActorRef A'. A transient channel is like a randomly selected high numbered TCP port, and is opened on demand. Sender needs to have a reference to ChannelInput object to be able to send messages to a transient channel.

### Base traits

The library provides a set of base actor traits that work with channels.

#### ChannelActor

Base trait that provides channel based messaging. Allows opening channels and processing events.

#### PersistentChannelHandlerActor

A trait on top of `ChannelActor` for receiving messages from persistent channels. Analogous to TCP server.

#### RequestServerActor and RequestClientActor

Traits implementing request/response communication pattern. Analogous to HTTP server and client.

### FRP library

Used to provide a rich set of methods for processing events.

We are currently using [scala.frp](https://github.com/dylemma/scala.frp), [patched](https://github.com/uosis/scala.frp/tree/sync-source) to be fully synchronous.

### FRP continuation extensions

`EventStreamImplicits` - a set of methods to provide clean interface for handling events using [compiler generated continuations](https://github.com/scala/scala-continuations).

## Examples

There is a set of examples under [akka-frp/examples/src](akka-frp/examples/src) that showcase the major features of the library.

## Installation

The project uses [Mill](https://www.lihaoyi.com/mill/) for building. Wrapper script is checked in, and the only external dependencies are JDK and our patched version of FRP library.

1. Install FRP library from [here](https://github.com/uosis/scala.frp/tree/sync-source), using standard SBT local publish.
1. Build and publish: `./mill akka-frp.publishLocal`
1. Run tests: `./mill akka-frp.tests`
1. Run examples: `./mill akka-frp.examples.run`
