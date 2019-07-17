## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will pay those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

### Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|
|       Packages containing the main() application. 
|       This is where all the dependencies are instantiated.
|
├── pleo-antaeus-core
|
|       This is where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|
|       Module interfacing with the database. Contains the models, mappings and access layer.
|
├── pleo-antaeus-models
|
|       Definition of models used throughout the application.
|
├── pleo-antaeus-rest
|
|        Entry point for REST API. This is where the routes are defined.
└──
```

## Instructions
Fork this repo with your solution. We want to see your progression through commits (don’t commit the entire solution in 1 step) and don't forget to create a README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

Happy hacking 😁!

## How to run
```
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library

## Development log

Alright, as requested, this README will work as a sort of log where I'll be noting up the process of
developing the Antaeus challenge.

First, a quick self reminder to fix the UNIX line ends for running the app on Windows

* I've been using Windows for over a year now as the OS where I generally code, noticed Docker
has some issues and requires a specific version of windows 10 to work locally unless I use the
docker toolbox, sadly, never been able to enable virtualization on this pc despite being supported.
Moved to my Linux partition which hasn't been touched for over a year now and more errors popped up
based on my Linux partition state. Unluckily, I've dropped the idea on fixing it and requested some help
which is now allowing me to properly run the docker and the application!

* A day ago, I started thinking on how to tackle the problem and although it wasn't really
difficult to find a mental solution or a guideline to follow for resolving the challenge. I spent a
little more time debating in the approach and reading through the code in the application. First
thoughts are how compact Kotlin code looks like and really liked the idea of typing most variables
instead of letting the compiler assume what is the variable type. Due to my Java background, this
makes me feel a little more safe.

Checked the documentations and examples for some of the libraries like Javalin and Mockk. Focused
mostly on Javalin as my first small goal was fetching the issues marked as pending and creating a
rest endpoint for displaying the results.

The proposed solution for solving the challenge is going for the smaller picture to the bigger. The
application has a lot of space to increase its functions, add safety measures, add unit tests to
create a more robust application. The path to follow consists on:

1 - Fetching & Displaying PENDING issued invoices:
One of the many reasons why I love being a programmer, is seeing. Generally, clients, coworkers and
3rd parties enjoy seeing advancements. Although there's not a formal UI, we can display the
invoices to keep track and see progress. It doesn't only motivates on seeing the small steps but
also help visualizing the progress on the path.

2 - Add single invoice payment method:
This is the most basic function required. The reason to create a function for a single invoice and
not all of them at once is to minimize errors and make code that's friendly for creating an unit
test. We don't know who can work in code we make later and it's our responsibility to make our code
understandable.

3 - Create a task for paying a list of Pending invoices
Just the bigger picture of the previous step. Note that so far there are no validations done, as we
want to have functional code that solves the punctual challenge.

4 - Create a task to track the date time and execute the point 3 task
One of the requirements is to pay the pending invoices at the 1st of the month. Unlike Java, I do
not have a lot of knowledge in Kotlin and it can get trickier to do this. Although so far from what
I've read and seen about this language, not only finding the date shouldn't be too difficult but
also open possibilities to add localization to the task.

5 - Validations!
With the previous point finished, we're now technically done with the challenge. Except for the
exception handling, no pun intended. At this point, it's just refining the code and handling the
exception that can appear through the execution.

6 - Going wild.
Basically letting my imagination flow and open new functions to Antaeus. Example, allowing the
 database to update invoices after a successful payment. Adding a limit date field to the Invoice
 model, as honestly, you should be given a few days to pay an invoice before having issues with this to
 evade problems when a client is having X or Y issue. Based on the previous one, it should also try
 to inform the customer about a failure in the payment process with their invoice and also the app
 should try more than once to pay in case there's an issue with the connection which can cause an
 unfair issue for a customer. Lastly, unit testing, not having a lot of experience as I'm close to a
 year and half after obtaining my engineering degree, I've had to deal with unit testing for a good
 amount of times in the workplaces I've been in order to understand how important this is and
 understand the code done. Although some have given me a run for my money, I still appreciate an
 unit test as it keeps the code safe and in check.

So far! step one has been completed with a little time of tweaking around with Javalin and a first
attempt to create a general function to retrieve only pending invoices. It was later modified to a
more general funtion, overriding the same fetchInvoices() function while accepting a status and
retrieving the invoices based on it. This will help in case there are plans for adding new status,
example could be adding a "Failure" status which would either result on a phone call/ mail being
sent to contact the customer. Little random note, I have really enjoyed Kotlin so far, specially the
 Elvis operator and the reason why it was named like that, hoping to see if I can use it somewhere
haha.





