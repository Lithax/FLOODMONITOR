# Fragestellungen

## 1. Was sind die Hauptmerkmale von JSON?

JSON (JavaScript Object Notation) ist ein textbasiertes, schlankes Datenformat. Daten bestehen aus Objekten (`{"key": value}`) und Arrays (`[...]`). Erlaubte Werte sind String, Zahl, Boolean, `null`, Objekt und Array. Es ist sprachunabhängig, für Menschen gut lesbar, leicht zu parsen und in JavaScript direkt nutzbar. JSON kennt keine Kommentare, keine Attribute und von sich aus kein Schema.

Quellen: [json.org](https://www.json.org/json-en.html), [RFC 8259](https://www.rfc-editor.org/rfc/rfc8259)

## 2. Was sind die Hauptmerkmale von XML?

XML (Extensible Markup Language) ist eine Auszeichnungssprache. Daten werden als hierarchischer Baum aus Elementen (`<tag>...</tag>`) mit optionalen Attributen dargestellt. XML ist selbstbeschreibend, unterstützt Namespaces, Kommentare und gemischte Inhalte und lässt sich mit XSD validieren sowie mit XPath und XSLT abfragen und transformieren. Es ist ausführlicher als JSON, dafür strenger strukturiert.

Quelle: [W3C XML 1.0](https://www.w3.org/TR/xml/)

## 3. Wie unterscheidet die Applikation, ob JSON oder XML zurückgegeben wird?

Der Client wählt das Format über den `Accept`-Header (Content Negotiation). Die Controller deklarieren mit `produces = [APPLICATION_JSON_VALUE, APPLICATION_XML_VALUE]`, welche Formate möglich sind. Spring wählt anhand des `Accept`-Headers den passenden `HttpMessageConverter` (Jackson für JSON, Jackson XML für XML). Ohne `Accept`-Header wird das erste Format in `produces` verwendet, also JSON. Wird ein nicht unterstütztes Format angefragt, wirft Spring eine `HttpMediaTypeNotAcceptableException`, die der `GlobalExceptionHandler` in `406 Not Acceptable` übersetzt.

Quellen: [RFC 9110, Content Negotiation](https://www.rfc-editor.org/rfc/rfc9110#name-content-negotiation), [Spring Framework, @RequestMapping](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)

## 4. Was zeichnet ein REST Interface aus und warum wird es heute so oft verwendet?

REST ist ein Architekturstil mit diesen Merkmalen:

* Client-Server-Trennung
* Zustandslos: jede Anfrage enthält alle nötigen Informationen
* Cachebar
* Einheitliche Schnittstelle: Ressourcen werden über URIs identifiziert, über Standard-HTTP-Methoden bearbeitet und in Repräsentationen wie JSON oder XML übertragen
* Schichtenarchitektur

Es wird oft verwendet, weil es auf dem bestehenden HTTP-Standard aufbaut, plattform- und sprachunabhängig ist, durch Zustandslosigkeit und Caching gut skaliert und von fast allen Tools und Frameworks unterstützt wird.

Quelle: [Fielding, Architectural Styles and the Design of Network-based Software Architectures, Kapitel 5](https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm)

## 5. Unterschied zwischen GET, POST, PUT und PATCH

| Methode | Zweck | Eigenschaften |
|---|---|---|
| GET | Ressource lesen | sicher (verändert nichts), idempotent |
| POST | Ressource anlegen oder Verarbeitung auslösen | weder sicher noch idempotent |
| PUT | Ressource vollständig ersetzen (oder anlegen) | idempotent |
| PATCH | Ressource teilweise ändern | nicht zwingend idempotent |

Quellen: [RFC 9110, Methods](https://www.rfc-editor.org/rfc/rfc9110#name-methods), [RFC 5789 (PATCH)](https://www.rfc-editor.org/rfc/rfc5789)

## 6. Welche Stellen im Quellcode muss ich für einen neuen Request ändern?

* **Controller:** neue Methode mit `@GetMapping`, `@PostMapping` usw. und Pfad (`StationController` oder `AlertController`), inklusive `produces` für JSON und XML
* **Service:** die Geschäftslogik (`StationService`), da Controller keine Logik enthalten sollen
* **Model:** neue oder geänderte Datenklassen, falls neue Daten geliefert oder entgegengenommen werden
* **Exception Handling:** neue Fehlerfälle im `GlobalExceptionHandler` abbilden
* **Dokumentation:** `@Operation` für OpenAPI
* **Tests:** passender Test in `StationApiTest`
* **Webclient:** nur, wenn der Endpunkt dort verwendet werden soll

Quelle: [Spring Framework, @RequestMapping](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-requestmapping.html)

## 7. Kann man OpenAPI und Swagger vergleichen?

Nur eingeschränkt, weil sie nicht auf derselben Ebene liegen. **OpenAPI** ist die Spezifikation: ein standardisiertes, sprachunabhängiges Format (JSON oder YAML) zur Beschreibung von REST-APIs mit Endpunkten, Parametern, Bodies, Statuscodes und Authentifizierung. Sie wurde ursprünglich als Swagger Specification entwickelt und später an die OpenAPI Initiative übergeben. **Swagger** ist heute der Name der Werkzeuge rund um diese Spezifikation, zum Beispiel Swagger UI (interaktive Dokumentation), Swagger Editor und Swagger Codegen. Kurz: OpenAPI beschreibt die API, Swagger sind Tools, die diese Beschreibung erzeugen, anzeigen oder verwenden. In diesem Projekt erzeugt springdoc die OpenAPI-Beschreibung unter `/api/v1/api-docs`, und Swagger UI stellt sie unter `/api/v1/swagger-ui.html` dar.

Quellen: [OpenAPI Specification](https://spec.openapis.org/oas/latest.html), [swagger.io](https://swagger.io/)

## 8. Wo liegt im Code-Snippet der Fehler?

```java
@GetMapping("/stations/add")
public String createStation(@RequestParam String name, @RequestParam double warning) {
    stationService.save(new Station(name, warning));
    return "Station successfully created!";
}
```

Der Hauptfehler ist, dass eine **GET-Anfrage Daten verändert**. GET muss sicher und idempotent sein. Browser, Crawler, Prefetching und Caches dürfen GET-Anfragen jederzeit ausführen, wodurch ungewollt Stationen angelegt werden. Außerdem begünstigt es CSRF, weil ein einfacher Link oder ein Bild-Tag die Aktion auslösen kann.

Weitere Probleme:

* Das Verb steckt im Pfad (`/add`). REST verwendet stattdessen die HTTP-Methode auf einer Ressource.
* Es gibt keine Validierung (leerer Name, negativer oder ungültiger Wert).
* Die Antwort ist ein Klartext-String statt `201 Created` mit der neuen Ressource und einem `Location`-Header.
* Die Daten stehen im Query-String und landen in Logs und der Browser-Historie.

Richtig wäre `POST /api/v1/stations` mit JSON- oder XML-Body, Validierung (`@Valid`) und der Antwort `201 Created`.

Quellen: [RFC 9110, Safe Methods](https://www.rfc-editor.org/rfc/rfc9110#name-safe-methods), [OWASP CSRF Prevention Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html)