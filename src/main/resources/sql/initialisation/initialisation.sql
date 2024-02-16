 CREATE TABLE IF NOT EXISTS users
    (userId VARCHAR(100) UNIQUE NOT NULL PRIMARY KEY,
    title VARCHAR(50) NOT NULL,
    firstName VARCHAR(50) NOT NULL,
    lastName VARCHAR(50) NOT NULL,
    emailAddress VARCHAR(100) NOT NULL,
    age INT NOT NULL
    )
;

CREATE TABLE IF NOT EXISTS users_address
    (userId VARCHAR(100) UNIQUE NOT NULL PRIMARY KEY,
    firstLineAddress VARCHAR(100) NOT NULL,
    region VARCHAR(50),
    postCode VARCHAR(100) NOT NULL
    )
;

CREATE TABLE IF NOT EXISTS users_occupation
    (userId VARCHAR(100) UNIQUE NOT NULL PRIMARY KEY,
    occupation VARCHAR(50) NOT NULL,
    industry VARCHAR(50) NOT NULL,
    income INT NOT NULL
    )
;

CREATE TABLE IF NOT EXISTS users_balance
    (userId VARCHAR(100) UNIQUE NOT NULL PRIMARY KEY,
    balance INT NOT NULL,
    pending INT NOT NULL,
    overdraft INT NOT NULL
    )
;

CREATE TABLE IF NOT EXISTS creditScoreRating
    (userId VARCHAR(100) UNIQUE NOT NULL PRIMARY KEY,
    creditScore INT NOT NULL
    )
;
