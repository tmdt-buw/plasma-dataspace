# Knowledge Graph Service
[![pipeline status](https://git.uni-wuppertal.de/eskape/implementation/services/knowledge-graph-service/badges/master/pipeline.svg)](https://git.uni-wuppertal.de/eskape/implementation/services/knowledge-graph-service/-/commits/master)
## Requirements
* Java 11
* Neo4J
* Maven

## Recommendations
* IDE: IntelliJ

## Setup
* Setup a Neo4J Instance 
* Checkout the Repository
* Copy the settings.xml from the project's .m2 folder into your local .m2 folder 
  * Enter your username and password for your Active Directory Account
* Copy the application-dev-template.properties to application-dev-<your-tim>.properties
  * Enter the credentials for your Neo4J instance
* Start the Application with your Spring Profile (-Dspring.profiles.active=dev-<your-tim>)

# Documentation Inherited from Template
Additional documentation can be found in gitlab in the `Documentation` project
of the `PLASMA` group and it's wiki.

## Java Version
The java version of the service artifact is 11.  
The java version of the shared artifact ist 8.

## Development Preparation
* Install OpenJDK 11
* Add PLASMA Maven Repository as follows:
    1.  Generate an API Access Token in GitLab
    2.  Go to your local .m2 folder and create a file named settings.xml 
    3.  Add the following content to the file where you replace "yourToken" with the token you received in the first step (Do not change the name property!)
```
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
      xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
      xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                          https://maven.apache.org/xsd/settings-1.0.0.xsd">
   
   <servers>
   <!-- Maven PLASMA Repository -->
        <server>
            <id>buw-gitlab</id>
            <configuration>
                <httpHeaders>
                    <property>
                        <name>Private-Token</name>
                        <value>yourToken</value>
                    </property>
                </httpHeaders>
            </configuration>
        </server>
   </servers>
</settings>
```

## IntelliJ
### Shared configuration
The code style settings and inspection profiles are managed in their separate
git repositories. They are configured as git submodules already. If they are
not presend after checking out this project they must be initialized via:
1. `git submodule init` which initializes the submodule repositories
1. `git submodule update --remote --rebase` which pulls their content from the
   upstream

### Dictionaries
Dictionaries are shared and committed to share them with all users. 
This avoids warnings to differ for different users.

## Maven goals
| goal | effect |
| ---- | ------ |
| clean | Removes all generated files in `/target`. |
| compile | Creates the binaries for the project, does not post-process them (e.g. the null checks for annotated methods. |
| test | Runs all test suits which match `*Test.java`. |
| package | Creates artifact jars and an executable in `service/application/target/`. |
| verify | Runs all test suits which match `*ITCase.java`. |
| site | Generates documentation in `target/site/javadoc` and test reports in `target/site/project-reports.html` |
| deploy | Deploys the packed artifacts in the configured repository. This requires the following environment variables to be set: `MAVEN_REPO_USER`, `MAVEN_REPO_PASS`. |
| jib:build | Creates a docker image and deploys it in a repository. This must be run on the application artifact or with `-f /service/application/pom.xml`. |
`-DskipTests` skips test executions.

## Testing
Tests are executed with JUnit5 (jupiter).  
Mocking, Stubbing and inspection can be done with _Mockito_.  
Unit test names must match `*Test.java`.  

### Integration tests
Integration test names must match `*ITCase.java`. They should spawn their
required infrastructure independently to allow parallel execution.
This can be done programmatically or with the `services` section in
`.gitlab-ci.yml` to avoid collisions.

## Service Discovery
Services are discovered via `Eureka` and accessed via `Feign`. \
For this to work conveniently the REST-Api should be extracted to `shared/api`
and it's implementation (`service/rest-api`) as well as the client
(`service/clients`) should implement it.
The client can now be autowired into components which require to access other
services. Credentials must be configured for each external service separately.

## Gitlab-CI
### Maven Nexus Deployment
Configure the `MAVEN_REPO_USER` and `MAVEN_REPO_PASS` in the gitlab project to
allow artifact deployment.

### Java Version
If you need to change the java version update the `.gitlab-ci.yml` file
accordingly.

### Docker
For the `docker_build` job to be successful the project needs to activate the
docker registry in gitlab. It can be triggered with maven via the `jib:build`
on the `serivce/application` module.

### Git
For a new project configure the project to only allow merges if they are part
of a semi linear history (`gitlab -> settings -> general -> merge requests`).  
Protect the `master` branch from direct pushes under 
`gitlab -> settings -> repository -> protected branches`.
Changes to the code are always implemented on a branch. Before merging rebase
the branch on the current master. Clean up the history of the branch to allow
help finding changes in the log before merging.

#### 7 Commit Rules [^1]
1. Separate subject from body with a blank line
1. Limit the subject line to 50 characters
1. Capitalize the subject line
1. Do not end the subject line with a period
1. Use the imperative mood in the subject line
1. Wrap the body at 72 characters
1. Use the body to explain **what** and **why** vs. how

## Swagger2
The Swagger documentation can be found at 
`http://localhost:${server.port}/${server.servlet.context-path}` which is
http://localhost:8181 by default.  
See `/service/application/src/main/resource/application.properties` for the
current definition.

[^1]: https://chris.beams.io/posts/git-commit/