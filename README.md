# Spring Boot Sample Continuous Integration Gateway

For continuous integration it is essential that you can test the changes to your application in the real environment. This sample 
project shows how you can deploy multiple versions of your application(s) and route the traffic for an individual user to a 
specific instance of the application.

![Overview](docs/overview.png?raw=true "Overview")

## The workflow

1) A developer commits his changes to a branch and creates a pull request
2) A CI software like Jenkins detects this pull request, builds the software and deploys it
3) The application registers itself at Eureka with ```$ApplicationName``` and MetaData: ```branch: $branchName```
4) The developer (or automated E2E Tests) set a cookie ```$ApplicationName=$branchName``` (see ServiceConfig)
5) All traffic to the application instances goes through the Zuul Gateway that interprets this cookie
6) Result of automated E2E Tests is reported back to pull request


## Howto test this example

### Compile all applications

```
mvn package
```

### Start all applications

#### EurekaServiceRegistry

Standard Eureka Registry:

```
cd EurekaServiceRegistry/target
java -jar EurekaServiceRegistry-1.0-SNAPSHOT.jar
```

#### SampleService

The sample service is a very basic rest endpoint that has a call counter and shows the branch name that it used to register at Eureka.
We will start multiple instances of this application with different branch names. 

Start with branch name ```master``` which is defined in ```application.yml```
```
cd SampleService/target
java -jar SampleService-1.0-SNAPSHOT.jar
```

Start more instances with different branch names
```
java -jar -Deureka.instance.metadataMap.branch=feature/something_important SampleService-1.0-SNAPSHOT.jar
java -jar -Deureka.instance.metadataMap.branch=feature/major_layout_changes SampleService-1.0-SNAPSHOT.jar
java -jar -Deureka.instance.metadataMap.branch=release/17.52 SampleService-1.0-SNAPSHOT.jar
java -jar -Deureka.instance.metadataMap.branch=feature/minor_bugfix SampleService-1.0-SNAPSHOT.jar
```

#### ServiceConfig

The service ServiceConfig is a sample application that queries Eureka for all available application instances and allows you to set the correct cookie.

Start the ServiceConfig:

```
cd ServiceConfig/target
java -jar ServiceConfig-1.0-SNAPSHOT.jar
```

#### ZuulGateway

A standard Zuul instance with a small filter that looks at the users cookies to determine the correct route to the requested application instance.
If no cookie is found the application with the metadata branch=master will be used.
 
```
cd ZuulGateway/target
java -jar ZuulGateway-1.0-SNAPSHOT.jar
```

### Demo

After starting all application instances navigate with a browser to
```
http://127.0.0.1:8080/sampleservice
```

You should see a message: 
>Application: SampleService has been called 1 times. branch: master

Navigate to
```
http://127.0.0.1:8080/serviceconfig
```

and select a different branch/instance of the SampleService application:

![ServiceConfig](docs/serviceconfig.png?raw=true "ServiceConfig")

Navigate back to the sampleservice application.
```
http://127.0.0.1:8080/sampleservice
```
You should now see: 
>Application: SampleService has been called 1 times. branch: feature/minor_bugfix

When using a different browser / incognito window / deleting the cookies you will switch back to the "master" instance of the application.


## Stuff to investigate

* [Spring Cloud Gateway](http://cloud.spring.io/spring-cloud-gateway/) instead of Zuul 
  * Non-blocking API vs blocking (in Zuul)
  * support for long lived connections like websockets


