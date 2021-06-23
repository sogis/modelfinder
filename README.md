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

## Develop
First terminal:
```
mvn spring-boot:run -Penv-dev -pl *-server -am
```

Second terminal:
```
mvn gwt:codeserver -pl *-client -am
```

Or without downloading all the snapshots again:

```
mvn gwt:codeserver -pl *-client -am -nsu
```

## Todo
- Rename packages
- Docker images versioning. 
- Build image with Maven?
- Maven build noch ins Image verlagern. Ist so ein wenig verfrickelt.
- Tests
  * disable commandlinerunner in tests?
