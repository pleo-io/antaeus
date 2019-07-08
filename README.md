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
```bash
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library

## Thoughts on system Architecture

1. The system is mostly IO bound on the three operations
    1. Reading invoices from the DB
    2. Posting the invoices to the external PaymentProvider
    3. Writing/Updating the invoices to the DB
    
    [SQLite blocks db access when a process is writing](https://sqlite.org/faq.html#q5). The reads/writes will contend for access to the DB,
    this will quickly become untenable when processing a lot of invoices. With that in mind, I think the best system
    would separate the invoice 'reads' from invoice 'writes' in different tables and use a datastore which supports
    concurrent writes (something like Postgresql which has MVCC, or even a noSQL solution since there's no relationship between
    invoices)
    
2. This job will seem to run in a Web application which exposes a REST api. It's usually necessary to have more than 1 
    instance of your Web app to be running to maintain availability. There 
    will often be many instances of your app sitting behind a load balancer. So if there is code in your app that executes 
    a scheduled job, _all_ instances of your app will be executing that job unless measures are taking to prevent this. 
    Imagine if you have 6 instances start up the invoice job and charge clients 6 time each!
    Ideally only 1 instance of your fleet should be elected to execute the invoice processing job. But having a distributed semaphore 
    for multiple different instances could be a real pain! So with that in mind, I feel like the RESTful web app should be separated from the 
    invoice processing logic.
    
3. Which bring me to my next point: scalability. If we have 1 app instance running the job and there are 1 million invoices to process.
   Given that we have to do 1 network call and 2 db calls, and these might realistically take ~500ms all together (This is made up but I don't expect
   a banking web API to respond below 100ms...maybe I'm jaded :P). This means a single thread would take ~6 days to complete the job. This means you couldn't 
   guarantee charging a user on the 1st of them month! Imagine at 10 million invoices with 1 full second processing time...
   This means that the system should parallelize the work to multiple threads asynchronously. But a single core might not be enough even if mostly IO bound.
   For this reason and for the reason above, I think the invoice processing job should be delegated
   to some framework like Hadoop, Spark, Flink, etcetc. The job could then be kicked off, the job cluster could connect 
   to the db/some db dump and do the processing in batches using multiple instances with multiple cores.
    

## Requirements

With all of the above in mind, I'm not going to build all of that. Here is list requirements that I plan to start hacking at:


 1. All `pending` invoices should be fetched on the first of the month and processed.
 Once charged, the invoice status in the DB should be updated to be `paid`. Once charged, there should be a log trace
 of the invoice somewhere.
  
 2. When calling the external payment provider, there should be a retry mechanism in case of Network error.
 
 3. When processing an invoice, if an error occurs (CustomerNotFound, CurrencyMismatch, InsufficientFunds, SystemError,
 NetworkError) the invoice should be logged, an 'error' metric should be incremented and the invoice and reason for failure should be
 saved to the db so that someone/a system can investigate.
 
 4. A customer should never be charged twice. This means that in the event that a customer is charged but the subsequent processing 
 of the invoice fails (db conn error for example), this invoice should be logged for manual investigation. But it should _not be reprocessed automatically_.
 (Note: In an ideal world the invoices have unique IDs and the PaymentProvider system could in theory be made aware of these IDs. 
 Then, should we try and charge a customer twice for the same invoice, their system would respond with a HTTP 409. This would 
 guarantee that a user never gets charged twice for the same invoice)
 
 5. Remote calls should be done asynchronously to boost throughput.
 
 6. When all invoices are processed, the `status` column should be updated to either "PAID" or "ERROR" based on the `error`
 column of the processed table (This step should not be done until all invoices have finished processing) 

 
