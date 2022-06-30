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
```
./mvnw -Penv-prod clean package
```

### Native

```
./mvnw -Pnative test
./mvnw -DskipTests -Penv-prod,native package
```

Mir ist das in der Gesamtheit noch zuwenig klar und ich verstehe die Anleitung (spring-native) auch nicht so wirklich. Herausfordernd auch weil ich noch Gradle-Projekte habe. Bisher setzte ich bei den Tests das Profile "native" nicht und es hat funktioniert (auch mit Lucene). Hier war es aber notwendig. Es scheint (siehe POM), dass auch nur dann der Agent läuft (?). Dafür wird jetzt das Image zweimal gebuildet. 

## Run

## Todo
- Jar versioning? In Kombination mit Dockerimage (gh action workflow)
- Tests
  * disable commandlinerunner in tests?
- ...