## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## The solution

### 1. RFC
To solve the problem of scheduling payments of invoices on the first of the month, an 
[invoice billing scheduling RFC](/docs/rfc-invoice-billing-scheduling.md) was drafted.
The RFC takes an in-depth look at the problem and the requirements arising from it and weighs competing solutions 
against each other and recommends the most appropriate.
 
The intention of the document is that it documents the process of making architectural and design decision as well as
being used as a tool for peer reviewing and communicating such decisions.

The RFC establishes a criteria, from desired properties, in weighing computing solutions as follows:

> A robust payment scheduling mechanism is thus needed. The mechanism should (among other properties):

> - be horizontally scalable (distributable - ability to run on different machines
> - guarantee at most once delivery - we only want to charge an invoice once
> - guarantee durability and recoverability - we want the schedules to persist even after the application or its host are 
restarted
> - be dynamically configurable with granularity and little code changes - we want control (preferably user input driven) 
to change when and how billing is scheduled e.g. we may want to bill every day or every week e.t.c. Level of granularity
is also rather important e.g. a customer may prefer to pay on a daily basis while another prefers monthly
> - perform pre-execution checks - for extra precaution, we want to determine whether to proceed with the call to the 
payment provider based on predetermined rules e.g. `Invoice.Status` not being `PAID`. Configurability here is also
a consideration.
> - support rescheduling - since failure is a reality in the world of systems, we want the ability to safely retry without
any unwanted side effects
> - be extensible - we want the solution to be easily extended to fit future requirements
> - be efficient and performant - as we work hard to grow the business, so does the number of customers and invoices to be
billed; scale. The solution should withstand increase in scale with a low resource footprint so as not to starve the
application
> - be observable - we want the ability to peek into it and understand it's working for easy troubleshooting

From this process, `Solution 2: Time-based delay queue scheduling` is determined to be the most appropriate.

### 2. Design
The favoured solution proposes the use of the delay and schedule feature of ActiveMQ to schedule invoice billing for 
execution at a later time. Scheduling can happen way in advance of the target billing date e.g. on daily, on invoice 
creation etc.

The choice of ActiveMQ over Redis (and any other MOM) in spite of both having sorted persistence of delayed and 
scheduled messages is the fact that ActiveMQ is a dedicated MOM optimized for reading messages, one at a time, with 
queue deadlock management guaranteeing an at most once delivery; very desirable in a system involving payments. 
With Redis, deadlock management and at most once delivery guarantees would have to be handled by the application.

The sequence diagram below represents the flow and interactions between components called for by this design:

![](docs/img/time-based-delay-queue-scheduling-transparent.png)

### 3. Components
#### **InvoiceService**
The invoice service provides the methods to fetch and modify an `Invoice`. 
It has been extended by adding the following behaviours:
- retrieve invoices by status (e.g., `InvoiceStatus.PENDING`)
- update the status of an `Invoice`.

#### **CustomerService**
The customer service provides methods to fetch customers. 
There has been no modification done in this service.

#### **BillingService**
The billing service wraps a [Quartz Scheduler](http://www.quartz-scheduler.org/) to provide a mechanism to trigger 
scheduling of invoices at a configured interval (every minute, hourly, daily) via a cron job.

While still not ideal, it's a better divide and conquer alternative that incrementally schedules any new invoices at the
established intervals. Using the [Quartz Scheduler](http://www.quartz-scheduler.org) comes in handy as it guarantees 
that this cron runs only once at a time. Being clustered enhances failure tolerance.

This class forms the entry point of the invoice billing scheduling mechanism. The Job it starts schedules invoices that 
are still outstanding (i.e. those whose status is not `InvoiceStatus.PAID` by delegating to the `TaskScheduler`.

An invoice that is successfully scheduled for billing is updated with a newly introduced `InvoiceStatus.SCHEDULED`.

#### **TaskScheduler**
This is an interface laying out the behaviour of a task scheduler. It defines a method `schedule` that is a task
scheduler's public API.

```kotlin
fun schedule(destination: String, payload: Any, schedule: Schedule) : Boolean
```

A `TaskScheduler` is responsible for determining the delay times of invoice billing tasks and enqueueing them on the 
broker's delay queue. This is based on configuration driven by a DSL that is suitable. For instance, cron expressions 
passed into date-time objects can be used as a starting point as it suits most of the schedule definition needs. 
If need be, using a more advanced DSL backed by a rules engine can be incorporated without breaking any existing 
functionality as it only needs to resolve to a date-time object from which a difference in time  (the delay) can be 
calculated.

Moreover, since each invoice billing is scheduled in 'isolation' by applying the schedule configuration to derive a 
scheduling delay, it becomes possible to apply schedule configuration at a much lower level of granularity. 
For instance, rather than applying a global `first of the month`, each customer could have a schedule configuration of 
their own. Each invoice could also have a schedule configuration of it's own differing from another invoice even though 
they belong to the same customer.

#### **DelayedTaskScheduler**
This is a concrete implementation of `TaskScheduler` based on delayed scheduled queueing. It applies a supplied 
`Schedule` to generate a delay for use in sending a task to a delayed queue which is delegated to a `JmsProvider`.

It handles such nuances as applying time zones to derive precision closer to the locale of the customer. 
For instance, if we have customers in a GMT+0300 timezone and the desired billing time is the first of every month 
e.g. 1 January 00:00:00, we want to delay this billing task in such a way that that local time is achieved despite 
having the servers running the task elsewhere.

Despite ActiveMQ support cron expression based scheduling, with the `ScheduledMessage.AMQ_SCHEDULED_CRON` property, 
using a delay (in ms) calculated from the `Schedule` is favored because it's much more generic eliminating a tight 
coupling to cron syntax hence support for a wider custom schedule definition DSL becomes possible.

The cron expression is first evaluated into a date-time object from which a delay (the difference in time between the 
current time and the target time) is calculated.

#### **Schedule**
A domain class that holds a task schedule configuration from which task scheduling delays are calculated.

#### **JmsProvider**
An interface laying out the behaviour of a messaging system that implements the JMS specification.

```kotlin
fun send(destination: String, message: String, delay: Long)
``` 

#### **ActiveMQAdapter**
A concrete implementation of `JmsProvider` for the ActiveMQ MOM.

It handles establishing connections and sessions, creating a producer as well as a payload to send to ActiveMQ.

#### **MessagingMessageListener**
An abstract class extending a `javax.jms.MessageListener` and implementing an asynchronous message listener that JMS 
can push a message to.

This class is designed to be extended by workers/consumers of ActiveMQ queues allowing them to receive messages which
they can then handle in a custom way.

#### **InvoiceBillingWorker**
A invoice billing worker class extending the `AbstractWorker` class which extends the `MessagingMessageListener` class.
This class is thus the listener to the invoice billing queue which on message handles invoice billing tasks pushed to it
by delegating to a `PaymentProvider` to charge a customer's account. 

It also knows how to handle different outcomes. A success response from the `PaymentProvider` leads to the invoice 
status being updated to `InvoiceStatus.PAID` while a failure one leads to `InvoiceStatus.FAILED`. Varying exceptions are
caught and handled in tailored handle blocks.

Before a customer account is charged through a call to the `PaymentProvider`, a chain of pre-execution validation
interceptors is fired to ensure that the task is still viable. `ValidateInvoiceStatusInterceptor` is one such 
validation interceptor that confirms that an invoice's status has not moved to `InvoiceStatus.PAID` after the task was
scheduled.

### Implementation & Code organization
Most of the logic is domiciled in the `pleo-antaeus-core` package. 

Within it, an `pleo-antaeus-core.io.antaeus.core.infrastructure.messaging` package houses all the JMS and ActiveMQ 
concerns. DTOs are used for messaging with marshalling and unmarshalling happening between the infrastructure layer and 
the application. A `JsonSerializationHelper` in the `pleo-antaeus-core.io.antaeus.core.infrastructure.util.json` package
was coded for this reason. It is merely a thin wrapper around `com.fasterxml.jackson.databind.ObjectMapper`.

The `pleo-antaeus-core.io.antaeus.core.scheduler` package houses the `TaskScheduler` interface and the it's concrete
implementation `DelayedTaskScheduler`.

The `pleo-antaeus-core.io.antaeus.core.workers` houses the billing workers classes that include: `InvoiceBillingWorker` 
listener attached to the queue and validation interceptors.

Following the methodology of the [12 factor app](https://12factor.net/), config is stored in the environment which makes
it easy to customize the application, scaling is concurrency based e.g. that of workers and log events have been added.

### Testing

Unit tests were added for various classes. MockK has been employed in mocking out dependencies in these unit tests.

### Enhancements
There is still room for improvements some of which are outlined below:

- A more robust strategy is of triggering scheduling is an event-driven reactive approach where upon creation of an 
`Invoice`, a domain event is broadcast to all interested and subscribed "parties" (such as a billing service) who in 
turn react to it. An event is a message to numerous independent endpoints sent when an action has been carried out. 
With such an approach, billing could be curved out as a micro-service of it's own that reacts to domain events.
With the use of events, other downstream actions also become possible. For instance, a notifications service could 
listen to invoice domain events and fire notification commands as a result.

- Applying a more robust and custom DSL in defining `Schedule` properties. Currently, it's driven by cron expressions
even though the underlying delay mechanism is agnostic of this as a `Schedule` is resolved to a delay in milliseconds.
A more custom DSL could be backed by a fully fledged rules engine operating on relevant context formed from domain
objects such a `Invoice`, `Customer` etc.

- The web application and the workers could be split out into different containers to allow more granular optimization
and control over each. Currently, they both run in the same container. Splitting them out helps with fine grained
scaling by adding more resources only where they are needed.

- Enhance logging through the use of a context building library such as MDC that would allow renders in different
formats such as JSON, KV. For now, things have been kept simple with string formatted log lines. Moreover, adding
metrics instrumentation, error reporting would enhance observability.

- For better internationalization, and as a API standard, the `Amount` object should include a `precision` field 
representing the conversion precision. The `value` field on it's part should always bear the minor currency equivalent 
of an amount with `precision` applied in making conversions.

```kotlin
data class Money(
    val value: BigDecimal,
    val precision: Int,
    val currency: Currency
)
```

- Another consideration of the API standards to make is including trace information enveloped in request and response
bodies. Fields such as `messageId` and `timestamp` are rather important in tracing requests. They also come in quite
hand in performing idempotency checks so that a message is applied with an at most once guarantee.

- Testing should cover a good part of the implementation. More test suites such as integration tests, end to end tests,
load and performance tests etc should also be included.

### Notes
It has been a very inspiring and fulfilling experience working on this. In spite of it being a challenge, it's modelled
around real world problems that perhaps most people face. I certainly resonate with the scheduling problem and have
given it much though now and in the past. For this, I have loved every bit of this challenge.

I have tried to provide reasonable justifications for decision and gone the way of an RFC to document them. Some could
be based on assumptions that I held.

In terms of time spent on the challenge, a total of 23 hours is the figure. Below is a rough breakdown of how the hours
are distributed among various tasks:
- 3 hrs: Setting up the dev environment, breakdown of the problem, gathering requirements, getting familiar with the
code base
- 8 hrs: Researching and writing the [invoice billing scheduling RFC](/docs/rfc-invoice-billing-scheduling.md) that 
proposes competing solutions and ways them to arrive at a preferred solution, architecting and designing the proposed
solution
- 10 hrs: Implementing the design in code, unit tests, other kind of testing that's not necessarily automated
- 2 hr: Documentation of work done, polishing up code

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.


*Running through docker*

Install docker for your platform

```
make docker-run
```

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```


### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the "rest api" models used throughout the application.
|
├── pleo-antaeus-rest
|        Entry point for REST API. This is where the routes are defined.
└──
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
