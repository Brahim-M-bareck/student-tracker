# Réponses — TP 04 : API REST avec Spring Boot

Projet : `student-tracker` — package `esp.irt.studenttracker` — Java 25 — Spring Boot 4.0.9.

Les sorties réelles des appels HTTP sont dans `livrables/captures-curl.txt`. Les tests
automatisés sont dans `src/test/java/esp/irt/studenttracker/rest/StudentRestControllerTest.java`.

## Récapitulatif des endpoints

| Méthode | URL | Réponse attendue | Statut |
|---|---|---|---|
| GET | `/api/hello` | `Hello World!` | 200 |
| GET | `/api/students` | liste des 3 étudiants | 200 |
| GET | `/api/students/{studentId}` | l'étudiant à l'index demandé | 200 |
| GET | `/api/students/999` | erreur JSON `Student id not found - 999` | 404 |
| GET | `/api/students/abc` | erreur JSON de conversion | 400 |

---

## Question de l'exercice 5.2 — Pourquoi recréer la liste à chaque appel est une mauvaise pratique

Version naïve :

```java
@GetMapping("/students")
public List<Student> getStudents() {
    List<Student> students = new ArrayList<>();
    students.add(new Student("Poornima", "Patel"));
    students.add(new Student("Mario", "Rossi"));
    students.add(new Student("Mary", "Smith"));
    return students;
}
```

Cette version « fonctionne » pour un simple GET, mais elle est mauvaise pour plusieurs raisons :

1. **Aucun état persistant.** Chaque requête repart de zéro. Si un futur endpoint ajoute,
   modifie ou supprime un étudiant, la modification est perdue à la requête suivante. Une API
   REST doit exposer une ressource dont l'état est stable, pas une vue recalculée à chaque appel.
2. **Aucune source de vérité unique.** Les données sont codées en dur dans le contrôleur. Si un
   autre endpoint a besoin de la même liste, il la duplique : les deux copies peuvent diverger.
3. **Coût inutile.** On alloue une nouvelle `ArrayList` et de nouveaux objets à chaque requête,
   ce qui augmente la pression sur le garbage collector. Pour 3 étudiants c'est négligeable,
   mais le réflexe est mauvais et ne passe pas à l'échelle.
4. **Mauvaise séparation des responsabilités.** Le contrôleur doit orchestrer HTTP, pas stocker
   les données. La liste devrait venir d'un service, d'un repository ou d'une base de données.
5. **Faux sentiment de simplicité.** Le code paraît plus court, mais il ne prépare pas du tout
   l'ajout d'un `POST`, d'un `PUT` ou d'un `DELETE`.

Version corrigée avec `@PostConstruct` (contrôleur singleton, liste chargée une seule fois) :

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

`@PostConstruct` est appelé **une seule fois**, après l'injection des dépendances et avant que
le bean ne serve les requêtes. Comme le contrôleur est un singleton, la même liste est partagée
par tous les appels : les données sont cohérentes et on évite les allocations répétées.

Nuance importante : garder une liste mutable dans un contrôleur reste une solution de
démonstration. Dans une vraie application, le contrôleur est **sans état** et délègue à un
service / repository / base de données. Si plusieurs threads modifient la liste, il faut aussi
gérer la concurrence (`CopyOnWriteArrayList`, synchronisation, ou mieux : la base de données).

---

## 1. Qu'est-ce qu'une API REST ?

REST (*Representational State Transfer*) est un **style d'architecture** défini par Roy
Fielding, pas un protocole ni un format de données. Une API REST expose des **ressources**
identifiées par des URI et manipule leur **représentation** (souvent du JSON) avec les verbes
HTTP.

Principes appliqués dans ce TP :

- **Ressource** : `/api/students` représente la collection des étudiants ;
  `/api/students/2` représente un étudiant précis.
- **Verbe HTTP** : `GET` pour lire ; on ajouterait `POST` pour créer, `PUT`/`PATCH` pour
  modifier, `DELETE` pour supprimer.
- **Sans état côté serveur** : chaque requête contient tout ce qu'il faut pour être traitée ;
  le serveur ne conserve pas de session client entre deux appels.
- **Représentation** : le serveur renvoie du JSON, le client peut demander un autre format via
  l'en-tête `Accept`.
- **Interface uniforme** : mêmes conventions d'URI, de méthodes et de codes HTTP pour toutes
  les ressources.

REST n'impose pas JSON : on pourrait renvoyer du XML. Spring MVC choisit le format via la
négociation de contenu. Une API REST n'est pas synonyme de SOAP : SOAP est un protocole fondé
sur XML avec un contrat WSDL, alors que REST s'appuie sur HTTP et ses ressources.

---

## 2. Quelle est la différence entre `@Controller` et `@RestController` ?

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

Pour renvoyer directement des données, il faut ajouter `@ResponseBody` sur la méthode (ou sur la
classe).

`@RestController` est une **méta-annotation** qui combine `@Controller` et `@ResponseBody` :

```java
@RestController
@RequestMapping("/api")
public class StudentRestController {
    @GetMapping("/hello")
    public String sayHello() {
        return "Hello World!"; // → corps de la réponse, pas une vue
    }
}
```

Avec `@RestController`, chaque valeur de retour est écrite dans le corps de la réponse HTTP.
Pour un objet Java, Spring le sérialise en JSON grâce à Jackson. C'est pour cela qu'un
contrôleur d'API utilise `@RestController` et non `@Controller`.

---

## 3. À quoi servent `@RequestMapping` et `@GetMapping` ? Comment Spring choisit-il la méthode ?

`@RequestMapping` associe une URL (et éventuellement une méthode HTTP, des paramètres ou des
en-têtes) à une classe ou une méthode de contrôleur. Placé sur la classe, il définit un
**préfixe commun** :

```java
@RestController
@RequestMapping("/api")
public class StudentRestController { ... }
```

`@GetMapping` est un raccourci de `@RequestMapping(method = RequestMethod.GET)`. Il existe les
équivalents `@PostMapping`, `@PutMapping`, `@PatchMapping` et `@DeleteMapping`.

```java
@GetMapping("/students/{studentId}")
public Student getStudent(@PathVariable int studentId) { ... }
```

Au démarrage, Spring scanne les contrôleurs et construit une table de routage
(`RequestMappingHandlerMapping`) : pour chaque combinaison **URL + méthode HTTP + paramètres +
en-têtes**, il retient la méthode Java à appeler. À chaque requête, le `DispatcherServlet`
consulte cette table, appelle la méthode choisie, puis sérialise le résultat.

Si l'URL existe mais pas avec le bon verbe HTTP, Spring renvoie généralement **405 Method Not
Allowed** ; si aucune URL ne correspond, **404 Not Found**.

---

## 4. Comment Spring convertit-il un objet Java en JSON ?

Spring Boot configure automatiquement un `HttpMessageConverter` capable de lire et d'écrire du
JSON. Avec Spring Boot 4, c'est **Jackson 3** (`tools.jackson.*`) qui est utilisé par défaut.

Le mécanisme est le suivant :

1. la méthode du contrôleur retourne un `Student` ou une `List<Student>` ;
2. `@RestController` indique que la valeur est un corps de réponse ;
3. Spring choisit un convertisseur compatible avec le type retourné et le type MIME demandé
   (`Accept: application/json`) ;
4. Jackson sérialise l'objet : chaque getter public devient une propriété JSON.

Exemple :

```java
public class Student {
    private String firstName;
    private String lastName;

    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
}
```

produit :

```json
{"firstName":"Mary","lastName":"Smith"}
```

Pour la désérialisation (un futur `POST` avec un corps JSON), Jackson a besoin d'un
constructeur sans argument et de setters, ou d'un constructeur annoté. C'est pourquoi le POJO
du TP possède un constructeur vide, un constructeur complet et les getters/setters.

Le format peut être négocié avec l'en-tête `Accept` ; si le client demande du XML et qu'aucun
convertisseur XML n'est présent, Spring renvoie **406 Not Acceptable**.

---

## 5. À quoi sert `@PathVariable` ? Que se passe-t-il avec `/api/students/abc` ?

`@PathVariable` extrait une variable présente dans le **chemin** de l'URL et l'injecte dans le
paramètre de la méthode :

```java
@GetMapping("/students/{studentId}")
public Student getStudent(@PathVariable int studentId) {
    if (studentId < 0 || studentId >= theStudents.size()) {
        throw new StudentNotFoundException("Student id not found - " + studentId);
    }
    return theStudents.get(studentId);
}
```

- `/api/students/2` → `studentId = 2` → renvoie Mary Smith.
- `/api/students/999` → l'index est hors de la liste → `StudentNotFoundException` → **404**.
- `/api/students/abc` → Spring tente de convertir `"abc"` en `int` ; la conversion échoue avec
  `MethodArgumentTypeMismatchException` → notre handler global renvoie **400**.

Ne pas confondre avec `@RequestParam`, qui lit un paramètre de requête :
`/api/students?id=2` s'écrirait `@RequestParam int id`. `@PathVariable` fait partie de l'URI et
identifie la ressource ; `@RequestParam` filtre ou optionnalise la requête.

En production, on peut aussi valider l'identifiant avec `@Min(0)` et `@Validated`, ou utiliser
un identifiant métier (email, UUID) plutôt qu'un index de liste.

---

## 6. À quoi sert `@PostConstruct` ? Pourquoi le contrôleur est-il un singleton ?

`@PostConstruct` (package `jakarta.annotation`) marque une méthode appelée **une seule fois**
après la construction du bean et l'injection de ses dépendances, mais avant qu'il ne soit mis à
disposition pour traiter des requêtes. C'est l'endroit idéal pour initialiser des données en
mémoire dans ce TP.

```java
@PostConstruct
public void loadData() {
    theStudents = new ArrayList<>();
    theStudents.add(new Student("Poornima", "Patel"));
    theStudents.add(new Student("Mario", "Rossi"));
    theStudents.add(new Student("Mary", "Smith"));
}
```

Par défaut, un bean Spring est **singleton** : le conteneur crée **une seule instance** de
`StudentRestController` pour toute l'application. Toutes les requêtes HTTP passent donc par le
même objet, et la liste initialisée dans `@PostConstruct` est partagée.

Conséquences :

- la liste n'est pas recréée à chaque appel : moins d'allocations, données cohérentes ;
- si la liste est modifiée par plusieurs requêtes simultanées, il faut penser à la
  **thread-safety** (le contrôleur n'est pas magiquement synchronisé) ;
- dans une application réelle, on évite l'état mutable dans le contrôleur : on délègue à un
  service ou à un repository, et la base de données garantit la persistance et la concurrence.

Cycle de vie simplifié d'un bean singleton :
constructeur → injection des dépendances → `@PostConstruct` → utilisation → `@PreDestroy`.

---

## 7. Pourquoi créer une `StudentNotFoundException` plutôt que retourner `null` ?

Retourner `null` pour un étudiant absent est ambigu :

```java
// Mauvaise approche
return theStudents.get(studentId); // null si absent
```

Avec `null`, Spring renvoie souvent **200 OK** avec un corps vide (ou `null`). Le client ne peut
pas distinguer « étudiant inexistant » d'une réponse vide légitime, et il n'a aucun message
utile. Cela mélange aussi la logique métier (« cet étudiant n'existe pas ») et la logique HTTP
(« quel statut renvoyer ? »).

Une exception métier dédiée rend l'intention explicite :

```java
public class StudentNotFoundException extends RuntimeException {
    public StudentNotFoundException(String message) {
        super(message);
    }
}
```

```java
if (studentId < 0 || studentId >= theStudents.size()) {
    throw new StudentNotFoundException("Student id not found - " + studentId);
}
```

Le contrôleur n'a plus à décider du format d'erreur : il signale le problème, et le
`@ControllerAdvice` le traduit en **404** avec un JSON structuré. Avantages :

- statut HTTP correct et explicite ;
- message et format d'erreur homogènes pour tous les endpoints ;
- code métier testable indépendamment de HTTP ;
- pas de `null` à propager et à vérifier partout.

On choisit une exception **non vérifiée** (`RuntimeException`) pour ne pas polluer les
signatures de méthodes et pour bénéficier du rollback automatique de Spring sur les transactions
en cas d'erreur.

---

## 8. `@ExceptionHandler` local ou `@ControllerAdvice` global ? Pourquoi le catch-all `Exception` → 400 est-il dangereux ?

Un `@ExceptionHandler` placé dans le contrôleur ne gère que les exceptions levées par **ce
contrôleur** :

```java
@RestController
public class StudentRestController {
    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<StudentErrorResponse> handle(StudentNotFoundException exc) { ... }
}
```

Si un deuxième contrôleur est ajouté, il faut dupliquer le handler. Le format d'erreur peut
diverger.

`@ControllerAdvice` centralise la gestion pour **tous les contrôleurs** :

```java
@ControllerAdvice
public class StudentRestExceptionHandler {

    @ExceptionHandler(StudentNotFoundException.class)
    public ResponseEntity<StudentErrorResponse> handleStudentNotFoundException(
            StudentNotFoundException exc) {
        StudentErrorResponse error = new StudentErrorResponse();
        error.setStatus(HttpStatus.NOT_FOUND.value());
        error.setMessage(exc.getMessage());
        error.setTimeStamp(System.currentTimeMillis());
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StudentErrorResponse> handleException(Exception exc) {
        // Filet de sécurité du TP : renvoie 400
        ...
    }
}
```

C'est l'état final de l'exercice 5.7 : le contrôleur ne contient plus aucun `@ExceptionHandler`.
Un seul endroit définit le format `StudentErrorResponse` (`status`, `message`, `timeStamp`).

**Le catch-all `Exception` → 400 est dangereux en production.** Il attrape *toutes* les
exceptions non prévues : `NullPointerException`, panne de base de données, bug de sérialisation,
etc. Une vraie erreur **serveur** serait alors annoncée au client comme une erreur **client**
(400), ce qui :

- masque le bug et fausse le diagnostic ;
- trompe le client, qui risque de corriger une requête pourtant valide ;
- pollue les métriques et les alertes (on ne voit plus les 5xx) ;
- peut exposer un message interne (`exc.getMessage()`) contenant des détails sensibles.

Bonne pratique :

- gérer précisément les exceptions attendues (`StudentNotFoundException` → 404,
  `MethodArgumentTypeMismatchException` → 400, erreurs de validation → 400) ;
- garder un handler générique `Exception` → **500**, avec un message neutre et un log complet
  côté serveur ;
- ne jamais renvoyer la stacktrace au client ;
- envisager `ProblemDetail` (RFC 9457) ou `ResponseEntityExceptionHandler` pour un format
  standard.

Exemple plus sûr :

```java
@ExceptionHandler(MethodArgumentTypeMismatchException.class)
public ResponseEntity<StudentErrorResponse> handleTypeMismatch(
        MethodArgumentTypeMismatchException exc) {
    StudentErrorResponse error = new StudentErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "Valeur invalide pour le paramètre : " + exc.getName(),
            System.currentTimeMillis());
    return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
}

@ExceptionHandler(Exception.class)
public ResponseEntity<StudentErrorResponse> handleUnexpected(Exception exc) {
    log.error("Erreur inattendue", exc);
    StudentErrorResponse error = new StudentErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Une erreur interne est survenue",
            System.currentTimeMillis());
    return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
}
```

Dans ce TP, le catch-all 400 est conservé car il permet de démontrer simplement la gestion de
`/api/students/abc`. Il faudrait le remplacer par un handler 500 dans une application réelle.

---

## Preuves

- `livrables/captures-curl.txt` : sorties complètes des 5 appels `curl`, avec en-têtes HTTP pour
  les erreurs 404 et 400.
- `mvn test` : 6 tests, 0 échec (5 tests MockMvc + le test de chargement du contexte).
- `livrables/REPONSES.md` : ce document.
