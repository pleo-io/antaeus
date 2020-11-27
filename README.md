## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!


### Initial thoughts
* Scoping the problem

Initial considerations and assumptions
- potentially millions of customers going forward
- one invoice must be sent exactly once
- one customer could have many invoices
- another component in the system keeps track of the due amount and/or type of subscription 
- in case of delay: additional administration fee, interest, etc. as well as stopping the subscription if invoice has not been paid after 2 reminders

- no memory limitation to begin with
- system restart should not affect the schedulling outcome
- DB read-write heavy
- calling external service (payment service)


* Major components

Features:
- send a mail with an invoice / call payment service on the 1. each of month
- have a due date
- follow up on it: send reminders, adjust amount (interest, adm. fee)
- adequate error handling depending on the error type - retry, request manual processing


Scalability
- asynchronous processing 
- DB should usually be based on replication scheme to avoid being single point of failure; sharding
- non-relational DB is better at scaling due to lack of joins; in case of relational DB, add redundant info in a table to speed up the info retrieval
- horisontal scaling on the Application servers

- schedule the invoices in advance in a background process (pre-processing)
- mark each with a status "processing" -> "scheduled"
- possibly use an intermediate layer like MQ
- send them on the 1. of the month

* Key issues
- subscription cancellation - those should not be sent even if scheduled already
- subscription change - those should be reschedulled (if scheduled already)
- risk for breaking the mail server - throughput must be lower than the bandwidth of the mail server at the time of sending all mails on 1. month; maybe horisontal scaling together with load-balancing across the app servers
- depending on the time zone, customers might get their invoice(s) the day before/after