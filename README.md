## Process

### Design
Before I started implementation, I focused on high level architecture and how to approach batch processing 
correctly. Some of the challenges I found here:
* How to achieve scalability so multiple processes/threads can handle invoices without blocking each other and with avoiding double payments?
* What if external payment service is slow and invoice volume is high? 
* What if app is down during the time it’s scheduled to start processing and how to retry after it becomes healthy?

At that moment I decided:
* Application has to charge invoices simultaneously by multiple threads. In the real world `Payment Provider` would probably
be regular REST service so we could some nonblocking http client to handle multiple requests by a single thread
* To avoid charging the same invoices at the same time I decided to use database locking (`SELECT..FOR UPDATE`) 
but to keep database transactions short I introduced new invoice type(`IN PROGRESS`) so the thread that fetches invoices 
first locks rows, changes invoice statuses to `IN_PROGRESS` and commit a transaction. 
I tested it using a real database (`postgresql`)
* To fetch invoices from database in batches as I expect a high volume of pending invoices to process so getting all of them
in a single query is not an option

### Billing Service
I decided to use Project Reactor here because I think it's very good to orchestrate such flows in a declarative style. 
What's more, it allows to easily wrap synchronous, blocking calls and separate them from rest of the code.
Later we could replace blocking calls with nonblocking implementation like [reactor netty client](https://github.com/reactor/reactor-netty)
and [r2dbc](https://github.com/r2dbc).

Firstly, I created a pending invoices publisher. 
It works in pull manner which means it publishes next elements only when subscriber request them (`sink.onRequest`). 
Then I focused on charging a single invoice and handling all corner cases. It runs on [elastic scheduler](https://projectreactor.io/docs/core/release/api/reactor/core/scheduler/Schedulers.html#elastic--)
that is a good choice for I/O blocking work.
I added a timeout of 5s (could be configurable) and thought it's worth retrying 
similar to network exception using exponential backoff strategy. 
I assumed our external payment provider request is idempotent so it's safe to call it multiple times with the same invoice. 
Otherwise, they would have provided `status` method or something similar.
Finally, I connected both parts in `chargeAll` method. It's worth mentioning `limitRate` operator 
that controls the number of rows fetched by pending invoices publisher 
while `flatMap` enables concurrency (number of subscriptions limited to 50 at the same time).

### Scheduling 
I run invoice processing tasks one by one with 10 minutes delays if it's 1st day of a month. I don't know the exact requirements and 
I assumed new pending invoices could appear in any moment.

## Alternative approach

* I think that billing service should be separate service. It runs only once per month but consumes resources all the time.
One idea would be to create AWS Lambda scheduled by CloudWatch Events. 
* Other option to handle load balancing could be to evaluate some publisher/subscriber pattern. 
One processes would fetch pending invoices and publish messages. 
There would a job queue that would allow sharing work between multiple workers.
* [Quartz Scheduler](http://www.quartz-scheduler.org) with persistent Job Store could be a possible solution as well


