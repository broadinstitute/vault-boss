# BOSS REST API 

A RESTful implementation of the BOSS API spec.

## Resources 

This implementation identifies two types of resource: Group and Object, both coming in two flavors (``store``, for groups/objects tracking data in a backing object store that is under the BOSS API management, and ``fs``, for groups/objects that track data in an underlying filesystem).  

Groups are named by URL schemes 
```
/group/fs/{groupId}
/group/store/{groupId}
```

Every Object is attached to exactly one Group, so the Objects are named as sub-resources of the corresponding Group:
```
/group/fs/{groupId}/{objectId}
/group/store/{groupId}/{objectId}
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
