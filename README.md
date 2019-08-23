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


────────── / ──────────

### Solution

No change is done to the general configuration of the system, so it can be run and tested as usual.


The system now has two new services:

1. Timer Service: Fired with the start of the system. The function setup() is a recursive function that gets the current date and calculates the seconds until the first day of the next month. Sleeps for that amount of seconds, and then makes a call to BillingService. Hence, BillingService runs only every first day of a month. When BillingService is done with work, the setup function in TimerService is just called again.

2. Billing Service: Fired by the TimerService on the first day of every month. Uses an external and two internal services; PaymentProvider, InvoiceService, and CustomerService. The whole logic is split into 3 functions, and based on fetching all invoices that have their status as "PENDING", then attempting to charge them from customers' accounts. The handling of failed cases and exceptions is done as follows. If a payment fails on PaymentProvider due to: 
    * Insufficient funds: Admins will be notified about this whenever this happens. The customer will be notified about this in every 5 days until the situation is resolved. Another attempt to charge the invoice from this customer will be made every day until it's successful. For handling this case further, a subscription cancelling mechanism that will execute after a point can be implemented. A discussion can be held with the customers to come up with an appropriate policy.
    * A mismatch between the invoice currency and the customer currency: Assuming that Invoice entity in the database is filled with the data from Customer entity later regarding data storage timeline, Customer table is chosen as the one that has the correct data. As a result, the currency in the Invoice entry will be set to the currency from the Customer entry. An attempt to charge the payment will be made again, after a short delay.
    * A customer not being found: This problem might need to be handled on a human level, since there is not another unique indicator in Invoice model, that relates to a customer. A possible system solution could be to add an invoiceList field (filled with invoiceIds) to the Customer model, to keep the track of the invoices of a customer. Storing this would allow us to go and search for an invoice in Customer entity, and find out whether the customer does actually exists or not.  
    * A network error: In this scenario, another attempt for charging the invoice will be made after a short delay.

The other functions named as notifyAdminOnSlack and notifyCustomerByEmail are faking the functions of sending a message to a Slack channel regarding billings and sending an email to a customer, respectively.


Keywords:
Timer service is started by **launch** in a **Coroutine Scope** for not blocking the rest of the AntaeusApp.kt. **Suspend** is used to enable the usage of the **delay()** function.
