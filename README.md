### Code refactoring
I splitted AntheusDal into several *Dal classes along with mappings.kt and tables.kt files.
IMO (and by experience) a unique class managing several entities tends to become big, messy and prone to code duplication and errors.
I prefer classes: with a self-explanatory name, simple, short and focus on one concern. It's easier and faster to read and review.
It also allows to have straight forward coding standards enforced during code reviews. This way devs can still code in there "own way"
but with the same structure without effort.
It's also useful: 
- onboarding new devs: knowing one component structure means knowing all (except the business part of course) 
- searching with IDE, ex: search for 'customer' gives everything related to Customer* which is very convenient
- ... among other nice things that don't cross my mind right now =)

Side effect of that is having more classes but IDEs are here to help ,)

#### Example of coding standard
A Service can only call its corresponding Dal. Ex: Service_A can only Dal_A not Dal_B.
Instead Services call each other.

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

### Final note
Of course I know that the code I give you is far from complete and perfect and I almost feel ashamed to give you that
but I wish to have the chance to talk through it with some of you guys. 


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
