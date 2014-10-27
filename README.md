# BOSS REST API 

A RESTful implementation of rev3 of the BOSS API spec.

## Resources 

This implementation identifies one core resource: the Object.

Every Object has a unique ID, and is named with a URL in the following format:
```
/object/{objectId}
```

## Methods 

An HTTP ``GET`` on any resource will return the resource's representation (in this, and all methods described below, the client should specify the ``Accept: application/json`` header, as the service only returns resource representations as JSON).



## Configuration 

Example of a typical configuration file: 
```
server:
  adminConnectors:
    - type: http
      port: 8181
  applicationConnectors:
    - type: http
      port: 8180
database:
  driverClass: com.mysql.jdbc.Driver
  user: <MYSQL_USERNAME>
  password: <MYSQL_PASSWORD>
  url: jdbc:mysql://<HOST>:<PORT>/<DBNAME>
  validationQuery: SELECT 1 
objectStore:
  endpoint: http://10.200.10.4:7070
  pathStyleAccess: true
  username: <AMPLIDATA_USERNAME>
  password: <AMPLIDATA_PASSWORD>
```

Please note that the testGeneratePresignedURL test will fail unless you have ~/.aws/config specified correctly.  Ask a teammate for a copy.


## Database Schema

Currently, the database consists of three tables: 

### OBJECTS

The ``OBJECTS`` table contains the representation of all the objects stored by the BOSS system.

Most identifiers are currently limited to 255 characters -- the ``location`` field is limited to 1024 characters.

Column Name | Column Type
------------|------------
objectId    | varchar(255)
ownerId     | varchar(255) 
sizeEstimateBytes | int
objectName | varchar(255) 
active | boolean
location | varchar(1024) 
storagePlatform | varchar(255) 

### READERS and WRITERS 

The ACL (the list of readers and writers) for each object is stored in the tables READERS and WRITERS, respectively.
These two tables have an identical schema to each other.

Column Name | Column Type
------------|------------
id | varchar(255) 
username | varchar(255) 

The ``id`` field is a foreign-key into the OBJECTS table.  


