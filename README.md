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

Example of a typical configuration file: See boss-ws/src/test/resources/boss-config.yml

## Database Schema

Currently, the database consists of three tables: Objects, Readers, and Writers.
You can see the details in the file boss-ws/src/main/resources/migrations.xml.

## Development Environment

### Installed Software 

**Java 7+** and **Maven** are required to build and test BOSS.

### AWS Configuration

It is also necessary to set up a local AWS configuration, in one of the following ways.
* Local file `~/.aws/config`
* Local file `~/.s3cfg`
* Environment variable `AWS_CREDENTIAL_FILE`
* Environment variables `AWS_ACCESS_KEY` and `AWS_SECRET_KEY`

For more detail, see the [AWS JDK Javadoc](http://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/com/amazonaws/services/s3/AmazonS3Client.html#AmazonS3Client())
