## Challenge notes
I decided to go very simple with the implementation and explain what I would do here.
I also went for this option since I've never used Kotlin and it was taking me a lot of time to find
how to do the stuff I wanted to do (I'm used to Java/Maven/SpringBoot/Hibernate).
So if I work with you guys I will of course invest time to adjust to all your stack.
Also I couldn't code as much as I wanted the last 2 years because I was Team lead and PO so I'm a bit rusty =)
Finally I wanted to come back to you this week and time flies and I had/have and will have many interviews...
I hope I can convince you with a readme and a tiny bit of code! =) 

(also aiming for more devops stuff)

### Sheduler/Trigger every 1st of the month
This project looks very much like a backend REST API so I treated it accordingly and decided to keep it as simple
and light as possible. With that in mind I decided to not implement a scheduler to trigger the invoice 
payment on every 1st of the month inside the API but instead only expose an endpoint to pay the pending invoices.
I guess it means that I fail to complete the challenge straight away ^^" ... 

IMO scheduling and triggering is not the role of a backend REST API (even if it can be done using quartz for example).
Also it would constantly consume CPU and RAM and for a stateless app running on a cloud infrastructure I would not want it
especially if the cloud provider charge you based on resource consumption.
I think that this scheduling and trigger task should be done using an external and easily configurable tool calling
the endpoint every 1st of the month (maybe a tool centralising all the scheduled tasks of the system instead of 
having multiple schedulers spread all over the ecosystem).
The endpoint can also be called on demand by the frontend of a business operator or by the system if a NetworkException
occurs and the system wants to retry several time to pay an invoice before logging/sending an error message. No need
for multiple and complex endpoints only one does the trick.

So I didn't implement that part and assume this is done outside of this project.

### DAL
I would split the DAL into several DAL classes (so here CustomerDal and InvoiceDal) since one dal can become very big and messy
if it manages many entities. It's clearer to read and review.

### Services
I would only allow a service to call its corresponding dal (Service_A can only Dal_A not Dal_B) and enforce it 
in the code reviews. It's clearer to read and review.
Services can of course call each other.

### PaymentProvider
I was tempted to update the implementation to actually throws the CustomerNotFoundException and CurrencyMismatchException
and randomly throws a NetworkException but I decided not to because it was not the point of the challenge I choose to assume
it is (even though the code itself isn't).

### Unit test
I'm used to create at least one success and one error test for each service method. Here I added the signatures but didn't
implemented them. I would have created an in memory database like the one created when the app starts but constant test
samples and use it in the unit tests (and clean/recreate it between tests when necessary)

### Commit
I make sure that each commit is small and consistent. For example: a dal method,
with the corresponding service method (with comment if there is tricky parts) and the corresponding unit test.

### Branch
I code in dedicated branch because it's easier for code review after using Merge Request.
And it avoids conflict when working in the same branch and keeps the repo clean and clear




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
