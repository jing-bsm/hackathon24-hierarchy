### POC for hackathon

A plain PostgreSQL implementation of hierarchy.

### Technologies
Spring Boot 3 + Docker PostgreSQL 16

### Test
`mvn test`

### How to use it
* First, initialize the docker postgres. Comment out the initial docker test and run it to populate the db data. See `persistent-ore`.
* `hierarchy-writer` uses spring batch to import new hierarchies.
* `rest-reader` provides REST API that serves functions for descendants, tree, leaf, attribute, etc.
* `async` is a library that requires Redis and provides some functions such as downloading large csv files in REST API.

### Features
* Use plain PostgreSQL to serve tree REST API for hierarchy.
* Also provide authorization for users that can access explicit or cascade nodes of a tree.
* Also give flexibility to search nodes by filter different conditions.
* It uses a lot of native queries and strongly tide up to PostgreSQL.

