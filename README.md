# Building
In intelliJ, choose
```
New -> New from existing sources -> Import project from external model -> Gradle -> select "use gradle wrapper configuration"
```

You can also run gradle directly from the command line:
```
./gradlew clean run
```
# Usage
- Get all services with status `curl localhost:8080/service`
- Add service `curl -d '{"url":"http://www.google.se", "name":"Google"}' localhost:8080/service`
- Delete service with name `curl -X "DELETE" localhost:8080/service/"hej"` 
