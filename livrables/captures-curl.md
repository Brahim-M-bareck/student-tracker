# Captures curl — TP04 Student Tracker

- **Date** : 2026-11-09T22:04:05+00:00
- **Application** : Spring Boot 4.0.9 — Java 25
- **URL de base** : `http://localhost:8080`

## 1. GET /api/hello

```bash
curl http://localhost:8080/api/hello
```

**Réponse :**

```text
Hello World
```

## 2. GET /api/students

```bash
curl http://localhost:8080/api/students
```

**Réponse :**

```json
[{"firstName":"Poornima","lastName":"Patel"},{"firstName":"Mario","lastName":"Rossi"},{"firstName":"Mary","lastName":"Smith"}]
```

## 3. GET /api/students/2

```bash
curl http://localhost:8080/api/students/2
```

**Réponse :**

```json
{"firstName":"Mary","lastName":"Smith"}
```

## 4. GET /api/students/999 (erreur 404)

```bash
curl -i http://localhost:8080/api/students/999
```

**Réponse complète (en-têtes + corps) :**

```http
HTTP/1.1 404 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 09 Nov 2026 22:04:05 GMT

{"status":404,"message":"Student id not found - 999","timeStamp":1794261845175}
```

## 5. GET /api/students/abc (erreur 400)

```bash
curl -i http://localhost:8080/api/students/abc
```

**Réponse complète (en-têtes + corps) :**

```http
HTTP/1.1 400 
Content-Type: application/json
Transfer-Encoding: chunked
Date: Mon, 09 Nov 2026 22:04:05 GMT
Connection: close

{"status":400,"message":"Method parameter 'studentId': Failed to convert value of type 'java.lang.String' to required type 'int'; For input string: \"abc\"","timeStamp":1794261845187}
```
