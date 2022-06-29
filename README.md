![Build Status](https://github.com/edigonzales/modelfinder/actions/workflows/main.yml/badge.svg)

# modelfinder

## Health checks
http://localhost:8080/actuator/health/

Nach dem Hochfahren wird der Suchindex erstellt. Während des Erstellens ist die Anwendung "live" aber noch nicht "ready".

## Suchindex
- was
- wie
- wann (update)
- ...
- ...
- ...
- ...

## Develop
First terminal:
```
./mvnw spring-boot:run -Penv-dev -pl *-server -am
```

Second terminal:
```
./mvnw gwt:codeserver -pl *-client -am
```

Or without downloading all the snapshots again:

```
./mvnw gwt:codeserver -pl *-client -am -nsu
```

## Build

### JVM

### Native

```

```

## Run

## Todo
- Docker images versioning. 
- Tests
  * disable commandlinerunner in tests?
- ...