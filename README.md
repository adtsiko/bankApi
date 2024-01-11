# bankApi

This is a personal project, many of these features in real life would need to be their own microservices.

Aim: Create an API that will take user transactions from a kafka topic and then send them to bigtable instance. 

Tech Stack: Kafka, Scala, Docker, bigTable, postgres, (may need a cache), minio

Milestone 1: Is define all the requirement and features required to maximise :tick
Milestone 2: Create the backbone with all tech stacks :tick
Milestone 3: Add all features + tests
Milestone 4: Create an API that will send transactions to kafka to test the durability
Milestone 5: Setup a cloud infrastructure either in GCP or AWS


Milestone 1:
    What do I want this application to do?
    Users:
        - Create user registration (basic information)
        - Authentication (Will do this after milestone 5)
    Accounts:
        - Types of Accounts (savings, investments, checking)
        - Transferring money between accounts
        - generating account statements
    Transactions:
        - transactions will be implemented through kafka
        - deposits, withdrawal, transfers will be each on kafka topic
        - consistency and atomicity of these transactions
    bigTable:
        - User and account Information
        - Efficiently querying and updating data in BigTable
        - Consistency between kafka and BigTable
    postgreSQL:
        - Stores aggregated data
        - BI/complex queries
    Asynchronous Processing:
        - The API will require to run many threads awhich are handling many features at the same time (main reason why scala cats as it has small light weight fibers as I can run many of them on a single thread).
    Security:
        - Implement secure communication using HTTPS
        - Protecting sensittive data in transit and at rest
    Monitoring and Telemetrics:
        - Integrate monitoring Tools for system health
        - Add logging and metrics
    Error Handling:
        - Make sure all computations have more that satisfactory error handling so that if a failure occure, money just doesn't disappear
        - Handling edge cases
    Tesing:
        - Writing unit tests adn integration tests
        - Scenarios simulating
    Caching: 
        - Implement caching systems for freq accessed data

    (may include rate limiting depends on how much data I can generate, will use small instances in cloud infrastructure + I can't afford big instnces and there's no need)
