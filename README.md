# Aplikacja referencyjna — Spring Boot, WARIANT BAZOWY

Implementacja aplikacji-notatnika zgodna ze specyfikacją z rozdziału 6 pracy — odpowiednik
wariantu bazowego .NET, zbudowany idiomatycznie w Spring Boot, bez świadomego utwardzania.
Realistyczne słabości domyślne i celowe „naiwne" wzorce oznaczono w kodzie komentarzem `[OWASP Axx]`.

## Stos
- Spring Boot 3.3 (Java 17), Spring MVC + Thymeleaf
- Spring Data JPA + PostgreSQL 16
- Spring Security (form login, BCrypt)
- JWT (jjwt) dla warstwy `/api`
- Docker Compose (aplikacja + baza w osobnych kontenerach)

## Uruchomienie
Zatrzymaj wcześniejsze warianty, jeśli działają na porcie 8080 (`docker compose down`), potem:
```bash
docker compose up --build
```
Aplikacja: http://localhost:8080 (przekierowuje na /notes).

Konta testowe (jak w wariancie bazowym .NET):

| Konto        | Hasło    | Rola  |
|--------------|----------|-------|
| admin@local  | admin123 | ADMIN |
| alice@local  | alice123 | USER  |
| bob@local    | bob123   | USER  |

## Ważna różnica względem .NET (do dyskusji w pracy)
Spring Security jest „secure-by-default" mocniej niż .NET MVC. W wariancie bazowym Spring
**samoczynnie** zapewnia m.in.: włączoną ochronę CSRF dla warstwy webowej, nagłówki
X-Frame-Options (DENY) i X-Content-Type-Options (nosniff) oraz haszowanie haseł (BCrypt).
Dlatego słabości wariantu bazowego pochodzą głównie z logiki aplikacji, którą pisze programista,
a nie z braku podstawowych mechanizmów. To istotna obserwacja dla hipotezy H1 i warto ją
skontrastować z .NET, gdzie np. walidacja anty-CSRF w kontrolerach MVC nie jest domyślna.

## Mapa powierzchni ataku (do rozdziału 7)

| Kategoria OWASP 2025 | Gdzie w kodzie | Charakter w wariancie bazowym |
|---|---|---|
| A01 IDOR (poziomo)   | `NoteController.view/edit/delete`, `NoteApiController.one` | brak weryfikacji właściciela |
| A01 eskalacja (pionowo) | `SecurityConfig` (webChain), `AdminController` | `/admin` dla każdego zalogowanego (brak roli) |
| A01 SSRF             | `NoteController.importUrl` | pobieranie dowolnego URL bez walidacji |
| A02 Misconfiguration | `application.properties` | brak CSP; `include-stacktrace=always` |
| A03 Supply Chain     | `pom.xml` | miejsce na pakiet z CVE (do potwierdzenia skanerem) |
| A04 Cryptographic    | `SecurityConfig` (BCrypt) | hasła haszowane domyślnie — oczekiwany wynik: chronione |
| A05 SQL Injection    | `NoteController.search` | surowy SQL z konkatenacją (native query) |
| A05 XSS (stored)     | `templates/notes/view.html` | `th:utext` (renderowanie bez kodowania) |
| A06 Insecure Design  | brak rate limiting, brak ograniczeń importu | ocena jakościowa |
| A07 Auth Failures    | `AuthController.register`, brak lockout | brak polityki haseł i limitu prób |
| A08 Integrity (JWT)  | `JwtUtil`, `JwtAuthFilter` | brak walidacji issuer/audience; token 24 h |
| A09 Logging          | brak audytu nieudanych logowań | (Spring loguje część zdarzeń dopiero na DEBUG) |
| A10 Exceptional      | `DebugController.error` | nieobsłużony wyjątek + ślad stosu w odpowiedzi |
| CSRF                 | `SecurityConfig` (webChain) | **włączony domyślnie** przez Spring Security |

## Szybka weryfikacja (przykłady)
- IDOR: zaloguj jako alice, otwórz `/notes/3` (notatka Boba) — w wariancie bazowym się wyświetli.
- Eskalacja: jako alice wejdź na `/admin` — w wariancie bazowym dostęp jest.
- XSS: utwórz notatkę z treścią `<script>alert(1)</script>` i otwórz jej podgląd.
- SQLi: `/notes/search?q=%' OR '1'='1` — zwróci notatki spoza filtra tytułu.
- A10: `/debug/error?input=abc` — odpowiedź zawiera ślad stosu.

## Uwaga
Aplikacja jest celowo pozbawiona utwardzenia i służy wyłącznie do kontrolowanych testów
w izolowanym środowisku na potrzeby pracy magisterskiej. Nie należy jej wystawiać do sieci.
