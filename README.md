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

##### Scoping the problem

Initial considerations and assumptions
* potentially millions of customers going forward
* one invoice must be sent exactly once
* one customer could have many invoices
* another component in the system keeps track of the due amount and/or type of subscription 
* in case of delay: additional administration fee, interest, etc. as well as stopping the subscription if invoice has not been paid after 2 reminders

* no memory limitation to begin with
* system restart should not affect the schedulling outcome
* DB read-write heavy
* calling external service (payment service)


##### Major components

Features:
* send a mail with an invoice / call payment service on the 1. each of month
* have a due date
* follow up on it: send reminders, adjust amount (interest, adm. fee)
* adequate error handling depending on the error type - retry, request manual processing


Scalability
* asynchronous processing 
* DB should usually be based on replication scheme to avoid being single point of failure; sharding
* non-relational DB is better at scaling due to lack of joins; in case of relational DB, add redundant info in a table to speed up the info retrieval
* horisontal scaling on the Application servers

* schedule the invoices in advance in a background process (pre-processing)
* mark each with a status "processing" -> "scheduled"
* possibly use an intermediate layer like MQ
* send them on the 1. of the month

##### Key issues
* subscription cancellation - those should not be sent even if scheduled already
* subscription change - those should be reschedulled (if scheduled already)
* risk for breaking the mail server - throughput must be lower than the bandwidth of the mail server at the time of sending all mails on 1. month; maybe horisontal scaling together with load-balancing across the app servers
* depending on the time zone, customers might get their invoice(s) the day before/after

### Comments on the actual implementation

##### Summary

Overall I have aimed at balance between resources (tech stack experties, time) and usability. Indeed, it ended up being a simple yet realistic solution (for a small business to begin with). It was important for me that the functionality is easy to read, extend/maintain and test.
Have kept in mind SOLID OOP principles while working on it.
I do realise I have not used Kotlin's full power, neither its conciseness, though I am afraid I got hooked on it and would eagerly deepen my knowledge.
Very shortly after I started, I did appreciate the chance I have been given to get an idea of the type of technology stack I would deal with when part of Pleo's crew.
I should also admit I was a bit sceptical towards Kotlin as a programming language when I first read what the idea of its creation has been. Surprisingly (positively) after a day of familiarising myself with the basic concepts and syntax, I started enjoying its straightforwardness and light-weight while not compromising on power.

##### Functionality

* The billing service starts straight away with starting the application, but gets suspended until the first of the coming month (in UTC) and only then it starts charging all pending invoices.
* In case of a problem during the charging process, the issue is logged. Added a rest call to retrieve those logs - could be used in admin GUI where each problem can be handled manually, if needed.
* In the original Invoice table, those problematic invoices remain in state PENDING. This way if no additional processing (manual or automatic) sorts the problem out, then at the next billing there will be another attempt to charge them. While the nature of some of the potential issues are hard to get solved without additional processing (e.g. currency mismatch), others might not be present at the next billing cycle (for example customer's account balance)
* As charging relies on an external provider, calls to it are asynchronous.
* Introduced a config object to hold parameters in one place, so it is easy to modify, for example, the billing scheme. At a later stage it could be decided that billing should take place every 3 months, or even introduce different kinds of subscription and/or billing models.

##### Future improvements

* Data Model.
I would believe in a real-life system an Invoice would have ```issueDate``` and ```dueDate``` fields and some logic around reminders when the payment deadline has been exceeded, interests and additional administration fee added every time a new reminder is sent.
In addition, I would add a ```Customer.Status {ACTIVE / INACTIVE}``` field to ```Customer``` and would not delete a customer, but rather change its status.

* Automatic processing of the problematic invoices. For example the currency mismatch could be handled by a third party currency converter. There could be logic for retrying charging for Invoices not paid due to Network error shortly after, and those that were paid but the system failed to mark them as such in the DB, could be automatically updated too, so the customer is not charged second time for them.
* Regular cleanup/archiving of the paid invoices to keep Invoice table "clean"'".
* Scalability.
SQlite's default behaviour seems not to allow concurrent writes. Since billing is heavily dependent on DB read/write operations, I would consider making DB calls asynchronous as well.
Processing could also happen in portions to avoid overload on the payment days (and potentially not even managing to charge for all invoices within 24h as the business grows)

* Security.
Security is a whole separate topic on its own. I would spare the details (also because I am sure I have yet to learn so much more about in this field), and just like in a movie trailer will stop here - leaving the reader intrigued. :)

##### Time reporting

I have been working on Antaerus challenge over few days. If I sum up the total time I have spent on it, I would say it is around 2 full man days. As a total newbie to Kotlin, Mockk, Gradle, I have spent significant part of that time going through tutorials, examples or reported issues similar to mine (for example Antaeus would not build in the beginning, and it turned out it is because its current version did not support Java 15, thus the upgrade to Gradle 6.7.1 and the followed fixes of deprecated feature usages)