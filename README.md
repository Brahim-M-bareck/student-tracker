# Student Tracker — TP 04 (API REST Spring Boot)

Petite API REST qui expose une liste d'étudiants en mémoire, avec gestion globale des erreurs.

- Java 25
- Spring Boot 4.0.9
- Maven
- Starter `spring-boot-starter-webmvc`
- Package de base : `esp.irt.studenttracker`

## Lancer l'application

```bash
mvn spring-boot:run
```

L'application écoute sur <http://localhost:8080>.

## Endpoints

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/hello` | message de bienvenue |
| GET | `/api/students` | liste des étudiants |
| GET | `/api/students/{studentId}` | étudiant par index (0, 1, 2) |

Exemples :

```bash
curl http://localhost:8080/api/hello
curl http://localhost:8080/api/students
curl http://localhost:8080/api/students/2
curl -i http://localhost:8080/api/students/999   # 404
curl -i http://localhost:8080/api/students/abc   # 400
```

Les sorties réelles sont dans `livrables/captures-curl.md`.

## Tests

```bash
mvn test
```

Les tests utilisent `@SpringBootTest` + `@AutoConfigureMockMvc`. Avec Spring Boot 4, l'import
est `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` (les packages
`org.springframework.boot.test.autoconfigure.web.servlet.*` de Spring Boot 3 ne sont plus les
bons).

Pour un build complet :

```bash
mvn clean test
```

## Bonus

La section 9 du TP est implémentée dans une branche séparée, sans modifier `master` :

```bash
git checkout bonus-section9
mvn test
git checkout master
```

Elle contient `@ResponseStatus` sur l'exception, un champ `path` dans la réponse d'erreur et la
validation Bean Validation (`@NotBlank`, `@Valid`, `MethodArgumentNotValidException`). Voir
`BONUS.md` dans cette branche.

## Structure

```text
src/main/java/esp/irt/studenttracker/
├── StudentTrackerApplication.java
├── model/Student.java
└── rest/
    ├── StudentRestController.java
    ├── StudentNotFoundException.java
    ├── StudentErrorResponse.java
    └── StudentRestExceptionHandler.java

src/test/java/esp/irt/studenttracker/
├── StudentTrackerApplicationTests.java
└── rest/StudentRestControllerTest.java

livrables/
├── captures-curl.md
└── REPONSES.md
```

## Comportement des erreurs

- `StudentNotFoundException` → **404** avec un JSON `{status, message, timeStamp}`.
- `MethodArgumentTypeMismatchException` (ex. `/api/students/abc`) → **400** via le handler
  générique de démonstration.
- Le contrôleur ne contient aucun `@ExceptionHandler` local : tout passe par
  `StudentRestExceptionHandler` (`@ControllerAdvice`).
