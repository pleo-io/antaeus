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

## My thought process
I should start by saying that I have no experience in Java or Kotlin. My first experience with Kotlin (or Java in general) is this challenge and I am glad that I could explore a new language. I come from a Python background (and Golang too). 
Although Evan set me the challenge a week ago, I could not work on it till Sunday because I had to undergo a toe surgery. I am sorry for the delay in submission.

Coming to the challenge, the first thought that hit my brain was to add another endpoint for billing and use an external cronjob to hit on the first of every month thereby having the freedom to even manually do billing whenever needed. So, I added a REST endpoint "/rest/v1/billing/charge" which, on a GET request, go through the database of invoices, filter those that are pending, send through mock payment gateway and charge them. When a successful payment occurs for a bill, I change the status of the invoice to PAID from PENDING in DB. Also, I have added a table for Bills in the DB to keep a track of bill details like success of payment, reason for failure if the payment fails, etc. I have also added two more endpoints (like the customer and invoice ones) to fetch all bills and a specific bill ("/rest/v1/billing/bills{:id}").
After building the rest endpoints, I have moved on to the actual challenge (or at least from what I understand) which is billing periodically as a process internal to the app unlike a GET request using a cron job on the endpoint. To be honest, this was a tricky part for me and ate a lot of my time. I have Googled a lot trying to find the best solution for internal periodic tasks. I have narrowed down to two options: using quartz-scheduler library or using kotlin-coroutines. I have asked around on Slack/Discord to know which is the better tool and I learnt that quartz-scheduler will be a better tool if scaling is important and kotlin-coroutines are better for smaller jobs. So, I went ahead with kotlin-coroutines and built an internal scheduler removing the need for an external cronjob (although I have still left the endpoints on).

It took me around 8-9 hours to come up with this solution/submission and 1 hour preparation on Kotlin syntax basics (from this: [Kotlin for Python developers](https://github.com/Khan/kotlin-for-python-developers)). I wish I could spend more time on the challenge but because of my surgery, all my work got stalled. So, I just had this Sunday to work on this challenge and submit it before resuming my other work. It was a fun challenge and I developed a liking towards Kotlin/Java :) .

