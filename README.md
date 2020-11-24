## How I came to a solution

Read the instructions and fully understand the needs (readme, navigate in the code, take a look at the data). 
So far, I'm not 100% sure what has to be done but from what I understand I would do a cron job or a scheduled task.

Once I was done reading I had a pretty good idea of what kind of solution I was going to propose and do ... a task/job that would be triggered using something like a cron expression. 
Then I had 3 potential solution that came up on in my mind.

The first one is writing a code in a kotlin file and run it periodically using k8s's CronJob.
Last internship I did something similar, I've coded a Django command and it would run using k8s's Cronjob to cleanup AWS s3.
It's basically the same problem but with a different context.

Those commands would be usefull to compile and run my code in the case I'm choosing this solution :

```
$ kotlinc chargeSubsAmount.kt -d chargeSubsAmount.jar
```
```
$ kotlin -classpath chargeSubsAmount.jar HelloKt
```

The second one is also based off running a task periodically but instead of using k8s's Cronjob, it would use kotlin coroutine.
If I remember correctly one of my colleague during an internship did a presentation about Kotlin coroutine 2 years ago.
It was quite new back then but as I remember it had a great potential for executing tasks/jobs periodically

The last solution I had in mind was about using AWS Lambda or Scheduled tasks but this requires some fancier shinanigens. 
Either I have to host it on AWS or if I want to keep it locally, I have to setup something like NGINX for AWS to be able to send computed data back.
I'm not that experienced with AWS and it evolves so quickly that I don't know every feature available.

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

_Running Natively_

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

_Running through docker_

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

- [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
- [Javalin](https://javalin.io/) - Simple web framework (for REST)
- [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
- [JUnit 5](https://junit.org/junit5/) - Testing framework
- [Mockk](https://mockk.io/) - Mocking library
- [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!
