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

### In completing this challenge, I implemented 3 features:
1. [FetchInvoiceByStatus](https://github.com/bmwachajr/antaeus/commit/47d729b16c9d69235ddb7ca53a80d942dbd7fc78): As the name suggests, this feature enables us to fetch invoices by their status using the path `/v1/invoices/status/{:status}` - Returns a list of linvoices with the status `{:status}`.

Invoices can have 5 statuses `PENDING`, `FAILED`, `PAID`, `UNPAID`, `CURRENCY_MISMATCH` and `INVALID_CUSTOMER`.

Invoice statues enable us to query the right invoice category, charge invoices and manage them incase of exceptions such as network failures, currency mismatchs and invalid customers.

2. [Billing Service](https://github.com/bmwachajr/antaeus/commit/8be0f4ec4b44b3f1793990f34040889ac4eb1153: This feature enables us to bill the all invoices by `{:status}`.

The Billingservice is schedule to charge `PENDING` invoices on the first of every month, a scheduler starst a job that runs the billing service. This job recurrs and only bills invoices on the first of the month.

As a fail safe, an api endpoint to manually trigger billing has been added `/v1/invoices/status/:status/bill`. This gives the power to bill incoices out of season.

3. [Bill an invoice](https://github.com/bmwachajr/antaeus/commit/47d729b16c9d69235ddb7ca53a80d942dbd7fc78): It's a given that some invoices will fail to be paid because of an exception occurring. 

The billing services graciuosly fails and updates the status of these invoices to either of `FAILED`, `UNPAID`, `CURRENCY_MISMATCH` and `INVALID_CUSTOMER`.

These Invoices can be individually charged using the endpoint `/v1/invoices/{:id}/bill`.

### Testing
Added a [BillingServiceTest](https://github.com/bmwachajr/antaeus/commit/529578a0fe4d4357fa436f29bbb71b4c0c9404d4)

### Time spent
I spent appromixately a `week` leveling up on both leveling up on kotlin, experimenting with the apps javelin framework and working on the challenge. Approximately `3 hours a day`.
