# modelfinder

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
