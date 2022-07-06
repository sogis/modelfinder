![Build Status](https://github.com/edigonzales/modelfinder/actions/workflows/main.yml/badge.svg)

# modelfinder

Einfaches Suchen und Finden von INTERLIS-Datenmodellen. Es werden die Default-INTERLIS-Modellablagen und die damit verknüpften Ablagen auf Basis der jeweiligen _ilimodels.xml_-Datei. Siehe _LuceneSearcher.java_ zwecks Umfang und Struktur der Indexierung. 

## Anleitung

Die Anwendung kann vom Benutzer über zwei Query-Parameter gesteuert werden (z.B. über einen Bookmark):

### expanded

Ist `expanded=true` gesetzt, sind die INTERLIS-Modellablagen im Resultatefenster aufgeklappt. Fehlt der Query-Parameter oder ist er ungleich `true`, sind die Modellablagen nicht aufgeklappt. Die Details der Modelle sind immer zugeklappt.

```
http://localhost:8080?expanded=true
```

### ilisite

Mit `ilisite=<repo_name>` wird nur innerhalb dieser INTERLIS-Modellablage gesucht. Weil man (ich?) momentan die transitiven iliSite-Url nicht kennen kann, sondern anhand der Modelldatei-Url bloss den Domain-Namen, wird im Prinzip dieser verwendet.

```
http://localhost:8080?ilisite=models.geo.admin.ch
```

### query
Wir `query=<query string>` verwendet, kann direkt via URL gesucht/gefiltert werden. 

```
http://localhost:8080?query=wald
```

### nologo
Mit `nologo=true` wird das Logo nicht gerendert. Bei allen anderen Werten als `true` wird das Logo gerendert.

```
http://localhost:8080?nologo=true
```

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

### Docker
```
docker run -p 8080:8080 sogis/modelfinder
```

### Health checks
http://localhost:8080/actuator/health/

Nach dem Hochfahren wird der Suchindex erstellt. Während des Erstellens ist die Anwendung "live" aber noch nicht "ready". Der Scheduler scheint mir auf Digitalocean nicht zu funktionieren. Ebenfalls können einige Repos nicht gelesen werden (Firewall?).


## Todo
- Jar versioning? In Kombination mit Dockerimage (gh action workflow)
- ...