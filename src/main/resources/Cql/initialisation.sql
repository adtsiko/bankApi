CREATE KEYSPACE finance
WITH replication = {'class':'SimpleStrategy', 'replication_factor' : 3};

CREATE TABLE finance.transactions(
   emp_id int PRIMARY KEY,
   amount int
   );
