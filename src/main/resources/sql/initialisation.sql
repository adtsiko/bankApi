 CREATE TABLE IF NOT EXISTS users
    (userId VARCHAR(100) UNIQUE NOT NULL,
    Name VARCHAR(50) NOT NULL,
    emailAddress VARCHAR(100) NOT NULL,
    region VARCHAR(50),
    age INT NOT NULL,
    creditScore INT NOT NULL
    )
;
