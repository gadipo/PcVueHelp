# XML Bulk Editing Tools for PcVue XML

This folder contains a Java 17, Maven-based toolset for bulk XML editing workflows:

- `xml-bulk-webapp`: Spring Boot web application (WAR packaging) for interactive XML bulk edits with preview and change report.
- `xml-bulk-cli`: standalone command-line tool for scripted XML bulk edits (supports dry-run and report output).
- `xml-bulk-core`: shared XML search/match/edit engine.

## Features

- Search/filter by tag name, attribute name/value contains, text contains, text regex, XPath-like path contains, and ancestor tag/text criteria for nested matching.
- Bulk operations:
  - replace text
  - replace attribute values
  - add/remove attributes
  - add/remove nodes
- Preview changes (`dryRun`) before applying.
- Return modified XML and detailed change report.
- Useful for PcVue scenarios such as template-object property changes, cloning-style updates, and retargeting instances where `BACnet_EDEKeyname` contains a substring.

## Build (all modules)

```bash
cd xml-bulk-edit-tools
mvn clean package
```

## CLI usage

Build:

```bash
cd xml-bulk-edit-tools
mvn -pl xml-bulk-cli -am package
```

Run (example dry-run):

```bash
java -jar xml-bulk-cli/target/xml-bulk-cli-1.0.0-SNAPSHOT-jar-with-dependencies.jar \
  --input /absolute/path/Vitania_Full.xml \
  --tag Property \
  --attr-name id \
  --attr-value-contains Alarm \
  --xpath-contains "/Root/TemplateInstance/Property" \
  --ancestor-tag TemplateInstance \
  --ancestor-text-contains Fire_Det \
  --replace-text -1 \
  --dry-run \
  --report /absolute/path/change-report.txt \
  --output /absolute/path/Vitania_Full.updated.xml
```

## Web app usage

Run in embedded container:

```bash
cd xml-bulk-edit-tools
mvn -pl xml-bulk-webapp -am package
mvn -pl xml-bulk-webapp org.springframework.boot:spring-boot-maven-plugin:3.3.5:run
```

Open: `http://localhost:8080/`

Build WAR for Tomcat deployment:

```bash
cd xml-bulk-edit-tools
mvn -pl xml-bulk-webapp -am package
```

WAR path:

- `xml-bulk-webapp/target/xml-bulk-webapp-1.0.0-SNAPSHOT.war`

Deploy this WAR to an external Tomcat instance.
