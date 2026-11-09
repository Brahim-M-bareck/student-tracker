# Réponses — TP 04 : API REST & gestion des exceptions

Projet : `student-tracker` — package `esp.irt.studenttracker` — Java 25 — Spring Boot 4.0.9.

Les sorties réelles des appels HTTP sont dans `livrables/captures-curl.txt`. Les tests
automatisés sont dans `src/test/java/esp/irt/studenttracker/rest/StudentRestControllerTest.java`.

## Récapitulatif des endpoints

| Méthode | URL | Réponse attendue | Statut |
|---|---|---|---|
| GET | `/api/hello` | `Hello World` | 200 |
| GET | `/api/students` | liste des 3 étudiants | 200 |
| GET | `/api/students/{studentId}` | l'étudiant à l'index demandé | 200 |
| GET | `/api/students/999` | erreur JSON `Student id not found - 999` | 404 |
| GET | `/api/students/abc` | erreur JSON de conversion | 400 |

---

## 1. Différence entre `@Controller` et `@RestController`. Que fait concrètement `@ResponseBody` ?

`@Controller` est le stéréotype Spring MVC historique. Par défaut, la valeur de retour d'une
méthode est interprétée comme un **nom de vue** (JSP, Thymeleaf, etc.) :

```java
@Controller
public class PageController {
    @GetMapping("/page")
    public String page() {
        return "students"; // → students.html ou students.jsp
    }
}
```

`@ResponseBody` indique à Spring que la valeur de retour doit être écrite **directement dans le
corps de la réponse HTTP**, et non interprétée comme une vue. Pour un objet Java, Spring utilise
un `HttpMessageConverter` (Jackson) pour le sérialiser en JSON :

```java
@Controller
public class StudentController {
    @GetMapping("/api/students/2")
    @ResponseBody
    public Student getStudent() {
        return new Student("Mary", "Smith");
    }
}
```

`@RestController` est une **méta-annotation** qui combine `@Controller` et `@ResponseBody` :

```java
@RestController
@RequestMapping("/api")
public class StudentRestController { ... }
```

Avec `@RestController`, chaque méthode est donc automatiquement sérialisée dans le corps de la
réponse. C'est le choix naturel pour une API REST, alors que `@Controller` reste utile pour une
application qui rend des pages HTML.

---

## 2. Qu'est-ce qu'un verbe HTTP idempotent ? Exemple idempotent et exemple non idempotent.

Un verbe HTTP est **idempotent** si appeler la même requête plusieurs fois produit le **même
état final** sur le serveur qu'un seul appel. L'idempotence concerne l'effet sur la ressource,
pas nécessairement le code de réponse.

| Verbe | Idempotent ? | Exemple |
|---|---|---|
| `GET` | Oui | `GET /api/students/2` ne modifie rien ; on peut le répéter sans effet de bord. |
| `PUT` | Oui | `PUT /api/students/2` avec le même corps remplace l'étudiant par les mêmes valeurs ; le résultat final est identique. |
| `DELETE` | Oui | `DELETE /api/students/2` supprime l'étudiant ; le refaire ne recrée rien (la ressource reste absente). |
| `POST` | Non | `POST /api/students` crée un **nouvel** étudiant à chaque appel : deux appels créent deux ressources. |
| `PATCH` | Généralement non | Un `PATCH` qui fait `solde = solde + 10` n'est pas idempotent ; un `PATCH` qui remplace un champ par une valeur fixe peut l'être. |

Exemple non idempotent concret :

```http
POST /api/students
{"firstName":"Ada","lastName":"Lovelace"}
```

Appelé deux fois, ce `POST` crée deux étudiants Ada Lovelace. C'est pour cela qu'on ne doit pas
utiliser `POST` pour une opération que l'on veut pouvoir rejouer sans risque.

---

## 3. Pourquoi initialiser les données avec `@PostConstruct` plutôt que dans `getStudents()` ? Quel est le coût de la mauvaise version ?

`@PostConstruct` (package `jakarta.annotation`) est appelée **une seule fois**, après la
construction du bean et l'injection de ses dépendances, avant que le contrôleur ne traite des
requêtes. Comme le contrôleur est un singleton, la liste est construite une fois et partagée :

```java
private List<Student> theStudents;

@PostConstruct
public void loadData() {
    theStudents = new ArrayList<>();
    theStudents.add(new Student("Poornima", "Patel"));
    theStudents.add(new Student("Mario", "Rossi"));
    theStudents.add(new Student("Mary", "Smith"));
}

@GetMapping("/students")
public List<Student> getStudents() {
    return theStudents;
}
```

La mauvaise version construit la liste **dans la méthode** :

```java
@GetMapping("/students")
public List<Student> getStudents() {
    List<Student> theStudents = new ArrayList<>();
    theStudents.add(new Student("Poornima", "Patel"));
    theStudents.add(new Student("Mario", "Rossi"));
    theStudents.add(new Student("Mary", "Smith"));
    return theStudents;
}
```

Coût et inconvénients :

- **allocations répétées** : une nouvelle `ArrayList` et trois nouveaux `Student` à chaque
  requête, donc pression sur le garbage collector et latence inutile ;
- **aucun état persistant** : si un `POST`, `PUT` ou `DELETE` est ajouté plus tard, la
  modification est perdue à l'appel suivant ;
- **source de vérité dispersée** : les données sont codées en dur dans le contrôleur ; un autre
  endpoint qui a besoin de la même liste doit la dupliquer ;
- **incohérence possible** : deux endpoints peuvent renvoyer des listes différentes si les
  données changent ;
- **mauvaise séparation des responsabilités** : le contrôleur doit gérer HTTP, pas stocker les
  données. Une vraie application délègue à un service / repository / base de données.

Pour trois étudiants, le coût est négligeable, mais le réflexe ne passe pas à l'échelle : avec
`N` étudiants et `R` requêtes, la mauvaise version fait `O(N × R)` allocations au lieu de `O(N)`.

---

## 4. Rôle de Jackson. Que change Spring Boot 4 par rapport à Spring Boot 2/3 ?

Jackson est la bibliothèque qui assure la **(dé)sérialisation JSON ⇄ objets Java** :

- objet Java → JSON lors de l'écriture de la réponse (`@RestController`, `ResponseEntity`) ;
- JSON → objet Java lors de la lecture du corps d'une requête (`@RequestBody`).

Spring Boot configure automatiquement un `ObjectMapper` et les `HttpMessageConverter` adaptés.
Dans ce TP, `List<Student>` est convertie en tableau JSON sans code manuel :

```json
[{"firstName":"Poornima","lastName":"Patel"}, ...]
```

Spring Boot 4 utilise **Jackson 3** au lieu de Jackson 2 :

| Élément | Spring Boot 2/3 (Jackson 2) | Spring Boot 4 (Jackson 3) |
|---|---|---|
| Package du databind | `com.fasterxml.jackson.databind.*` | `tools.jackson.databind.*` |
| Package du core | `com.fasterxml.jackson.core.*` | `tools.jackson.core.*` |
| `ObjectMapper` | `com.fasterxml.jackson.databind.ObjectMapper` | `tools.jackson.databind.ObjectMapper` |
| Annotations | `com.fasterxml.jackson.annotation.*` | `com.fasterxml.jackson.annotation.*` (conservées) |
| Dépendance | starter web | starter web (Jackson 3 fourni) |

Le code du TP n'importe aucune classe Jackson : il se contente de retourner des POJO, et Spring
Boot fait la sérialisation. Si l'on avait besoin de personnaliser le JSON, on utiliserait
`tools.jackson.databind.ObjectMapper` ou les annotations `@JsonProperty`, `@JsonIgnore`, etc.

---

## 5. À quoi sert `ResponseEntity<T>` par rapport à un simple retour d'objet ? Trois choses qu'il permet de contrôler.

Un simple retour d'objet laisse Spring décider du statut (généralement **200 OK**) et des
en-têtes :

```java
@GetMapping("/students/{id}")
public Student getStudent(@PathVariable int id) {
    return theStudents.get(id); // 200 + corps JSON, pas de contrôle du statut
}
```

`ResponseEntity<T>` est un wrapper qui permet de contrôler **trois choses** :

1. **le corps** de la réponse (`T` : objet, liste, `StudentErrorResponse`, etc.) ;
2. **le code HTTP** (`HttpStatus.NOT_FOUND`, `BAD_REQUEST`, `CREATED`, etc.) ;
3. **les en-têtes HTTP** (`Location`, `Content-Type`, en-têtes personnalisés, etc.).

Exemple :

```java
StudentErrorResponse error = new StudentErrorResponse();
error.setStatus(HttpStatus.NOT_FOUND.value());
error.setMessage("Student id not found - 999");
error.setTimeStamp(System.currentTimeMillis());

return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
```

On peut aussi construire la réponse avec les builders :

```java
return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
return ResponseEntity.ok(student);
return ResponseEntity.created(location).body(student);
```

C'est indispensable pour une API REST : un `404`, un `400` ou un `201` ne peuvent pas être
exprimés par un simple retour d'objet.

---

## 6. Comment Spring choisit-il quel `@ExceptionHandler` invoquer lorsqu'une exception est levée ?

Spring utilise `ExceptionHandlerExceptionResolver`. Pour une exception donnée, il cherche le
handler dont le **type d'exception est le plus spécifique** :

1. d'abord les méthodes `@ExceptionHandler` **locales** au contrôleur concerné ;
2. ensuite les handlers des classes `@ControllerAdvice` applicables ;
3. parmi les candidats, la correspondance la plus proche dans la hiérarchie l'emporte :
   `StudentNotFoundException` est préféré à `RuntimeException`, qui est préféré à `Exception`.

Exemple :

```java
@ExceptionHandler(StudentNotFoundException.class) // prioritaire pour cette exception
public ResponseEntity<StudentErrorResponse> handleNotFound(StudentNotFoundException exc) { ... }

@ExceptionHandler(Exception.class) // filet de sécurité pour tout le reste
public ResponseEntity<StudentErrorResponse> handleOther(Exception exc) { ... }
```

Si aucun handler ne correspond, Spring laisse remonter l'exception et applique les résolveurs
suivants (`ResponseStatusExceptionResolver` pour `@ResponseStatus`, puis le résolveur par défaut
qui produit une réponse 500). C'est pour cela qu'un handler `Exception.class` trop large peut
intercepter des erreurs inattendues : il faut le garder comme **dernier recours**.

---

## 7. Quel problème résout `@ControllerAdvice` par rapport à un `@ExceptionHandler` local ? Quand restreindre sa portée ?

Un `@ExceptionHandler` local ne s'applique qu'au contrôleur dans lequel il est déclaré. Avec
plusieurs contrôleurs, il faudrait dupliquer le même code dans chacun :

```java
@RestController
public class StudentRestController {
    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<StudentErrorResponse> handle(...) { ... }
}
```

`@ControllerAdvice` est un **gestionnaire global** : Spring l'applique à tous les contrôleurs.
On centralise le format d'erreur, on évite la duplication et on garantit une réponse homogène :

```java
@ControllerAdvice
public class StudentRestExceptionHandler {
    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<StudentErrorResponse> handleNotFound(...) { ... }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StudentErrorResponse> handleOther(...) { ... }
}
```

C'est un *cross-cutting concern* : la gestion des erreurs ne pollue plus les contrôleurs. On
peut aussi y ajouter `@InitBinder` ou `@ModelAttribute` pour des traitements communs.

On restreint sa portée quand plusieurs APIs ou modules ont des besoins différents :

```java
@ControllerAdvice(basePackages = "esp.irt.studenttracker.rest")
// ou
@ControllerAdvice(assignableTypes = StudentRestController.class)
// ou
@ControllerAdvice(annotations = RestController.class)
```

Cela évite qu'un handler global attrape par erreur les exceptions d'un contrôleur HTML ou d'un
autre module, et permet d'avoir des formats d'erreur distincts (API JSON vs pages web).

---

## 8. Pourquoi renvoyer un 404 avec un corps JSON structuré est-il préférable à laisser remonter une `IndexOutOfBoundsException` (500) ?

Laisser remonter `theStudents.get(999)` produit une `IndexOutOfBoundsException`. Spring renvoie
alors **500 Internal Server Error** avec, selon la configuration, une trace ou un message interne.
Pour le consommateur de l'API, c'est problématique :

- **sémantique incorrecte** : un 500 signifie « le serveur a un bug », alors que la ressource
  demandée n'existe simplement pas. Le client risque de réessayer ou d'alerter inutilement ;
- **contrat instable** : le corps est une page d'erreur ou une trace, pas un JSON documenté.
  Le client ne peut pas parser `status`, `message` et `timeStamp` de façon fiable ;
- **fuite d'information** : la trace peut exposer des noms de classes, des chemins, voire des
  données internes ;
- **expérience client dégradée** : le front ou l'application mobile ne peut pas afficher un
  message clair (« étudiant introuvable ») ni proposer une action adaptée ;
- **observabilité brouillée** : les vrais bugs serveur (500) sont noyés parmi les demandes
  d'identifiants inexistants.

Avec une exception métier et un handler global, la réponse devient explicite :

```http
HTTP/1.1 404
Content-Type: application/json

{"status":404,"message":"Student id not found - 999","timeStamp":1794261574093}
```

Le client sait immédiatement que la ressource est absente, peut afficher un message, et le
serveur garde les 500 pour les véritables erreurs internes. C'est un contrat clair, stable et
testable.

---

## Question complémentaire — Exercice 5.2 : pourquoi recréer la liste à chaque appel est une mauvaise pratique ?

La version demandée à l'exercice 5.2 construit la liste **à l'intérieur de `getStudents()`** :

```java
@GetMapping("/students")
public List<Student> getStudents() {
    List<Student> theStudents = new ArrayList<>();
    theStudents.add(new Student("Poornima", "Patel"));
    theStudents.add(new Student("Mario", "Rossi"));
    theStudents.add(new Student("Mary", "Smith"));
    return theStudents;
}
```

À chaque appel HTTP, une nouvelle liste et de nouveaux objets sont créés, puis oubliés. Cette
version est une mauvaise pratique car :

1. **elle ne conserve aucun état** : un ajout, une modification ou une suppression serait perdu
   à l'appel suivant ;
2. **elle n'a pas de source de vérité** : les données sont codées en dur dans le contrôleur ;
3. **elle gaspille des ressources** : allocations et garbage collection à chaque requête ;
4. **elle ne passe pas à l'échelle** : le coût augmente avec le nombre d'étudiants et de
   requêtes ;
5. **elle mélange les responsabilités** : le contrôleur ne devrait pas fabriquer les données.

La correction consiste à charger la liste une seule fois avec `@PostConstruct`, puis à la
retourner. Dans une application réelle, on va plus loin : le contrôleur appelle un service, qui
appelle un repository, qui lit la base de données. Le contrôleur reste sans état et la base
garantit la persistance et la concurrence.

---

## Preuves

- `livrables/captures-curl.txt` : sorties complètes des 5 appels `curl`, avec en-têtes HTTP
  pour les erreurs 404 et 400.
- `mvn clean test` : 6 tests, 0 échec (5 tests MockMvc + le test de chargement du contexte).
- `livrables/REPONSES.md` : ce document.
