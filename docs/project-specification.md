# Travel Memory Map

## Full-Stack Portfolio Project Specification

---

# 1. Концепция

Да се разработи модерно full-stack web приложение за записване, визуализиране и споделяне на пътувания.

Приложението не трябва да бъде обикновен CRUD travel diary.

Основната идея е потребителят да може да превърне едно свое пътуване в дигитална интерактивна история чрез:

* карта;
* маршрут;
* timeline;
* снимки;
* разходи;
* оценки;
* статистики;
* Travel DNA;
* споделени пътувания;
* интерактивно възпроизвеждане на пътуването.

Основната отличителна функционалност е:

**Trip Replay — интерактивно възпроизвеждане на цялото пътуване върху картата.**

---

# 2. Цел на проекта

Проектът е предназначен за portfolio и трябва да демонстрира реални умения по:

* React;
* TypeScript;
* Java;
* Spring Boot;
* REST API;
* PostgreSQL;
* Supabase;
* Authentication;
* Authorization;
* relational database design;
* работа с карти и географски координати;
* обработка и съхранение на изображения;
* clean backend architecture;
* frontend architecture;
* security;
* testing;
* Docker;
* CI/CD;
* deployment.

Кодът трябва да бъде структуриран така, че repository-то да изглежда като реален software project, а не като tutorial clone.

---

# 3. Technology Stack

## Frontend

* React;
* TypeScript;
* Vite;
* React Router;
* TanStack Query;
* React Hook Form;
* Zod;
* MapLibre GL JS;
* Tailwind CSS или CSS Modules.

## Backend

* Java;
* Spring Boot;
* Spring Web;
* Spring Data JPA;
* Spring Security;
* Bean Validation;
* PostgreSQL Driver;
* Flyway;
* MapStruct;
* JUnit 5;
* Mockito;
* Testcontainers.

Lombok може да бъде използван само там, където действително намалява boilerplate, без да скрива важна логика.

## Supabase

Supabase предоставя:

* PostgreSQL database;
* Supabase Auth;
* Supabase Storage.

## Deployment

Frontend:

```text
GitHub Pages
```

Backend:

```text
Spring Boot API
→ отделен hosting provider
```

Data:

```text
Supabase
├── PostgreSQL
├── Auth
└── Storage
```

---

# 4. High-Level Architecture

```text
┌──────────────────────────────┐
│         GitHub Pages         │
│                              │
│   React + TypeScript + Vite  │
└──────────────┬───────────────┘
               │
               │ HTTPS / REST
               ▼
┌──────────────────────────────┐
│       Spring Boot API        │
│                              │
│ Controllers                  │
│ Services                     │
│ Domain Logic                 │
│ Authorization                │
│ Repositories                 │
└──────────────┬───────────────┘
               │
               ▼
┌──────────────────────────────┐
│          Supabase            │
│                              │
│ PostgreSQL                   │
│ Authentication               │
│ Storage                      │
└──────────────────────────────┘
```

Frontend-ът не трябва да съдържа business logic, която принадлежи на backend-а.

Backend-ът е authoritative layer за:

* permissions;
* invitations;
* expenses;
* statistics;
* Trip DNA;
* memberships;
* business rules.

---

# 5. Основни Domain Objects

Системата трябва да съдържа поне:

```text
UserProfile

Trip
TripMember
TripInvite
TripDay
TripStop

Photo

Expense
ExpenseParticipant

TripRating
```

Derived concepts:

```text
TripReplay
TripStatistics
TripDNA
TravelProfile
ExpenseSettlement
```

не е задължително да бъдат database entities.

---

# 6. Authentication

Authentication се реализира чрез Supabase Auth.

Потребителят трябва да може да:

* създаде акаунт;
* влезе;
* излезе;
* редактира профила си;
* добави профилна снимка;
* вижда своята travel статистика.

Spring Boot backend-ът трябва да валидира authentication token-а.

Backend-ът никога не трябва да приема:

```text
userId
```

от frontend-а като доказателство за идентичност.

Текущият потребител трябва да бъде определян от валидирания authentication token.

---

# 7. Dashboard

След login потребителят вижда:

```text
Welcome back, Iliya 👋

12 Countries
31 Cities
8 Trips
427 Photos

────────────────────

Recent Trips

🇮🇹 Rome
12–16 Sep 2026

🇭🇺 Budapest
03–06 May 2026

🇬🇷 Corfu
17–23 Aug 2025
```

Dashboard трябва да съдържа:

* последни пътувания;
* upcoming trips;
* shared trips;
* държави;
* градове;
* посетени места;
* снимки;
* общи разходи;
* World Scratch Map;
* On This Day.

---

# 8. Trips

Потребителят трябва да може да:

* създава;
* редактира;
* архивира;
* изтрива;
* разглежда пътувания.

Trip съдържа:

```text
id

ownerId

title
description

country
city

startDate
endDate

coverImage

visibility

createdAt
updatedAt
```

---

# 9. Trip Visibility

Visibility трябва да бъде отделена от membership системата.

Възможни стойности:

```text
PRIVATE
PUBLIC
```

## PRIVATE

Private trip се вижда само от:

* owner;
* потребители, които са официални members на trip-а.

Наличието на invite link **не прави пътуването public**.

## PUBLIC

Public trip може да бъде разглеждан чрез public share page.

Public visitor няма автоматично право да редактира пътуването.

---

# 10. Private Shared Trips

Private trip може да има повече от един участник.

Пример:

```text
Italy Road Trip

Visibility:
🔒 PRIVATE

Members:

Iliya
OWNER

Maria
EDITOR

Ivan
VIEWER
```

Това означава:

```text
PRIVATE ≠ only one user
```

а:

```text
PRIVATE = visible only to authorized members
```

---

# 11. Trip Roles

Всеки TripMember има роля.

```text
OWNER
EDITOR
VIEWER
```

## OWNER

Може да:

* редактира trip settings;
* добавя и премахва members;
* създава invite links;
* revoke-ва invite links;
* добавя места;
* редактира места;
* добавя снимки;
* управлява expenses;
* променя visibility;
* изтрива trip.

## EDITOR

Може да:

* добавя места;
* редактира места;
* качва снимки;
* добавя expenses;
* редактира собствено съдържание.

Не може да:

* изтрива trip;
* сменя owner;
* променя критични permissions.

## VIEWER

Може само да разглежда съдържанието, до което има достъп.

---

# 12. Invite Links

Owner трябва да може да покани приятел към PRIVATE trip чрез специален invite link.

Пример:

```text
https://travelmemory.app/join/7Kp2nAx91
```

или при GitHub Pages:

```text
https://username.github.io/travel-memory-map/join/7Kp2nAx91
```

Invite link трябва да съдържа secure random token.

Не трябва да използва:

```text
/join/trip/123
```

като механизъм за authorization.

---

# 13. Generate Invite

В Trip Settings:

```text
Invite people

Role:
[ EDITOR ▼ ]

Expires:
[ 7 days ▼ ]

Maximum uses:
[ 1 ▼ ]

[ Generate Invite Link ]
```

Резултат:

```text
Invite link created

https://.../join/7Kp2nAx91

Role:
EDITOR

Expires:
18 Aug 2026

Uses:
0 / 1

[ Copy Link ]

[ Revoke ]
```

---

# 14. TripInvite

TripInvite трябва концептуално да съдържа:

```text
id

tripId
createdByUserId

tokenHash

role

expiresAt

maxUses
useCount

revokedAt

createdAt
```

Самият raw invite token не трябва задължително да се пази директно в базата.

Предпочитан вариант:

```text
link contains raw token
        ↓
backend hashes token
        ↓
database stores token hash
```

Подобно на password reset tokens.

---

# 15. Invite Security

Invite token трябва да бъде:

* достатъчно дълъг;
* random;
* unpredictable;
* URL-safe.

Не трябва да съдържа:

* trip ID като единствена защита;
* user ID;
* последователни числа.

Backend трябва да проверява:

```text
invite exists
AND
not revoked
AND
not expired
AND
useCount < maxUses
```

преди join операция.

---

# 16. Joining Through Invite

Получателят отваря:

```text
/join/{inviteToken}
```

## Ако вече е логнат

Вижда:

```text
Iliya invited you to join

🇮🇹 Italy Road Trip

12–18 September 2026

You will join as:
EDITOR

[ Join Trip ]

[ Cancel ]
```

След приемане:

```text
TripMember
```

се създава.

След това потребителят се пренасочва към trip-а.

---

# 17. Invite без акаунт

Ако потребителят няма акаунт:

```text
You've been invited to join
Italy Road Trip

Create an account or sign in
to accept the invitation.
```

След register/login invite token-ът не трябва да се губи.

Flow:

```text
Invite Link
    ↓
Not authenticated
    ↓
Login / Register
    ↓
Return to invitation
    ↓
Accept
    ↓
TripMember created
```

---

# 18. Already Joined

Ако потребителят отвори invite link, но вече е member:

```text
You're already a member of this trip.

[ Open Trip ]
```

Не трябва да се създава duplicate membership.

Database трябва също да защитава това чрез unique constraint:

```text
UNIQUE(trip_id, user_id)
```

---

# 19. Expired Invite

При изтекъл invite:

```text
This invitation has expired.

Ask the trip owner for a new invite link.
```

---

# 20. Revoked Invite

Ако owner revoke-не линка:

```text
This invitation is no longer valid.
```

Това не трябва да премахва members, които вече са се присъединили чрез него.

---

# 21. Invite Management

Owner трябва да има екран:

```text
Trip Settings
→ Invitations
```

Пример:

```text
ACTIVE INVITES

Editor invite
Expires 18 Aug
0 / 1 uses
[Copy] [Revoke]


Viewer invite
Expires 20 Aug
2 / 5 uses
[Copy] [Revoke]
```

---

# 22. Member Management

Owner трябва да вижда:

```text
Members

Iliya
OWNER

Maria
EDITOR
[Change Role] [Remove]

Ivan
VIEWER
[Change Role] [Remove]
```

Owner не трябва да може случайно да премахне единствения OWNER, без ownership transfer.

---

# 23. Leave Trip

EDITOR и VIEWER трябва да могат да напуснат trip.

```text
Leave Trip
```

OWNER не може да напусне, ако преди това не:

* transfer-не ownership;
  или
* изтрие trip-а.

---

# 24. Interactive Trip Map

Всяко пътуване трябва да има интерактивна карта.

Пример:

```text
Airport
   ↓
Hotel
   ↓
Colosseum
   ↓
Restaurant
   ↓
Trevi Fountain
```

Всеки marker съдържа:

* име;
* категория;
* дата;
* час;
* снимки;
* бележка;
* rating.

---

# 25. Trip Stops

TripStop:

```text
id
tripId

name
description

latitude
longitude

arrivalTime
departureTime

category
rating

position
```

Категории:

```text
LANDMARK
RESTAURANT
HOTEL
AIRPORT
BEACH
MUSEUM
BAR
SHOP
NATURE
TRANSPORT
OTHER
```

---

# 26. Timeline

Trip трябва да има timeline view.

```text
DAY 1 — 12 September

09:15
✈️ Fiumicino Airport

11:40
🏨 Hotel

14:15
🏛 Colosseum

17:30
🍝 Dinner

21:10
⛲ Trevi Fountain
```

Timeline се генерира от trip stops.

---

# 27. Trip Replay

Това е flagship feature на приложението.

```text
▶ Replay Trip
```

При стартиране:

```text
DAY 1

09:15
Fiumicino Airport
```

Картата премества камерата към съответната точка.

След това:

```text
11:40
Hotel
```

Камерата преминава към следващата точка.

Маршрутът постепенно се визуализира.

---

# 28. Replay Controls

```text
Play
Pause

Previous
Next

1x
2x
4x
```

Когато има снимки от TripStop, те могат да бъдат показвани синхронизирано с replay-а.

---

# 29. Photos

Снимките се съхраняват в:

```text
Supabase Storage
```

PostgreSQL пази metadata:

```text
id

tripId
tripStopId

uploadedByUserId

storagePath

takenAt

latitude
longitude

caption

createdAt
```

---

# 30. Photo Permissions

Само members с подходяща роля могат да upload-ват снимки.

PRIVATE trip снимките не трябва да бъдат публично достъпни само защото някой знае storage URL.

Storage access трябва да бъде съобразен с privacy модела на приложението.

---

# 31. EXIF Detection

При upload системата трябва да проверява EXIF metadata.

Ако съществуват:

```text
GPS coordinates
date
time
```

потребителят получава предложение:

```text
Location detected

Rome, Italy

41.8902
12.4922

14 Sep 2026
17:42

[ Add to Trip ]
```

Потребителят потвърждава преди запис.

---

# 32. Expenses

Всяко пътуване има expense tracker.

Категории:

```text
FLIGHT
ACCOMMODATION
FOOD
TRANSPORT
ACTIVITY
SHOPPING
OTHER
```

Expense:

```text
id
tripId

title
amount
currency

category
date

paidByUserId
createdByUserId
```

---

# 33. Expense Participants

Expense може да принадлежи на няколко участници.

Пример:

```text
Dinner

€120

Paid by:
Iliya

Split between:

Iliya
Maria
Ivan
```

---

# 34. Expense Settlement

Backend трябва да изчислява кой на кого дължи пари.

Пример:

```text
Maria → Iliya €90

Ivan → Iliya €30
```

Settlement algorithm трябва да бъде отделен от controller и persistence логика.

Пример:

```text
ExpenseSettlementCalculator
```

---

# 35. Expense Dashboard

```text
ROME

Total
€742

Flights
€180

Accommodation
€280

Food
€141

Transport
€61

Activities
€80
```

Допълнително:

* cost per day;
* cost per person;
* most expensive day;
* largest expense.

---

# 36. Trip Rating

След приключване:

```text
Food
Nightlife
Culture
Nature
Walkability
Value for Money
Crowds
Relaxation
```

Rating:

```text
1–10
```

Допълнително:

```text
Would you return?

YES
MAYBE
NO
```

---

# 37. Trip Comparison

Потребителят може да сравнява свои пътувания.

```text
             Rome    Barcelona

Food          9          8
Price         6          7
Nightlife     7          9
Culture      10          8
Walkability   9          8
```

---

# 38. Trip DNA

Системата анализира характера на пътуването.

```text
ROME TRIP DNA

Explorer       82%
Foodie         71%
Culture        64%
Nightlife      20%
Relaxation     12%
```

Първоначално да не се използва AI.

Trip DNA да бъде детерминистичен scoring algorithm.

---

# 39. Travel Personality

След достатъчно приключени пътувания:

```text
YOUR TRAVEL PERSONALITY

Urban Explorer
```

Примерни типове:

```text
Urban Explorer
Food Hunter
Culture Seeker
Nature Wanderer
Relaxed Traveler
Night Owl
Adventure Traveler
```

---

# 40. World Scratch Map

Профилът съдържа карта на посетените държави.

```text
Countries visited

12 / 195
```

При избор:

```text
ITALY

3 trips
7 cities
41 places
124 photos
€1,843 spent
```

---

# 41. Travel Statistics

Да бъдат изчислявани:

```text
Countries visited
Cities visited

Trips completed
Places visited

Photos uploaded

Total travel days

Total spent

Average cost per trip
Average cost per day

Favourite country
Favourite city

Most visited country

Most expensive trip
Cheapest trip

Longest trip
Shortest trip
```

---

# 42. On This Day

Dashboard проверява за минали пътувания на същата календарна дата.

```text
ON THIS DAY

3 years ago you were in Budapest 🇭🇺

6 places visited
18 photos
€82 spent
```

---

# 43. Search

Търсене по:

```text
country
city
trip
place
year
```

---

# 44. Filters

Trips могат да бъдат филтрирани по:

* година;
* държава;
* град;
* rating;
* price;
* duration;
* Trip DNA;
* owner/shared status.

---

# 45. Public Trip Page

Само PUBLIC trips могат да имат anonymous public page.

Пример:

```text
/trips/public/{publicSlug}
```

Public page показва:

* cover image;
* карта;
* timeline;
* selected photos;
* statistics;
* rating.

Да не се показват:

* expense settlements;
* private member information;
* invite links;
* permissions;
* чувствителни internal данни.

---

# 46. Invite Link ≠ Public Share Link

Това разграничение е задължително.

## Invite Link

```text
/join/{inviteToken}
```

Цел:

```text
Become a member
```

Изисква:

```text
authentication
+
valid invitation
```

## Public Link

```text
/trips/public/{slug}
```

Цел:

```text
View public trip
```

Не предоставя membership или edit permissions.

---

# 47. Backend Package Structure

Препоръчителен feature-oriented подход:

```text
com.travelmemory

├── auth
│
├── user
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
│
├── trip
│   ├── controller
│   ├── service
│   ├── repository
│   ├── entity
│   ├── dto
│   └── mapper
│
├── membership
│
├── invitation
│
├── expense
│
├── photo
│
├── replay
│
├── statistics
│
├── travelprofile
│
├── security
│
├── storage
│
├── exception
│
└── config
```

---

# 48. Service Responsibilities

Да не се създава един огромен:

```text
TripService
```

който прави всичко.

Разделяне:

```text
TripService

TripPermissionService

TripMemberService

TripInvitationService

TripStatisticsService

TripReplayService

ExpenseService

ExpenseSettlementCalculator

PhotoService

PhotoMetadataExtractor

TravelProfileCalculator
```

---

# 49. TripInvitationService

Примерни responsibilities:

```text
createInvite()

findValidInvite()

acceptInvite()

revokeInvite()

validateInvite()

incrementUsage()
```

Методите трябва да имат ясна и конкретна отговорност.

---

# 50. TripPermissionService

Authorization логиката трябва да бъде централизирана.

Пример:

```text
canViewTrip()

canEditTrip()

canManageMembers()

canManageInvites()

canDeleteTrip()

requireOwner()

requireEditorOrOwner()
```

Да не се разхвърлят хаотични:

```java
if (user.getId().equals(...))
```

проверки из целия проект.

---

# 51. Controllers

Controllers трябва да бъдат тънки.

Не:

```text
Controller

authentication
database queries
permissions
business logic
mapping
calculations
```

А:

```text
HTTP Request
     ↓
Controller
     ↓
Service
     ↓
Domain / Repository
```

---

# 52. DTOs

Entities не трябва директно да се връщат през REST API.

Пример:

```text
CreateTripRequest
UpdateTripRequest

TripSummaryResponse
TripDetailsResponse

CreateInviteRequest
InviteResponse
InvitePreviewResponse

MemberResponse
UpdateMemberRoleRequest
```

---

# 53. REST API

Примерни endpoints:

```text
POST   /api/v1/trips

GET    /api/v1/trips
GET    /api/v1/trips/{tripId}

PUT    /api/v1/trips/{tripId}
DELETE /api/v1/trips/{tripId}
```

Members:

```text
GET    /api/v1/trips/{tripId}/members

PATCH  /api/v1/trips/{tripId}/members/{memberId}

DELETE /api/v1/trips/{tripId}/members/{memberId}
```

Invites:

```text
POST   /api/v1/trips/{tripId}/invites

GET    /api/v1/trips/{tripId}/invites

DELETE /api/v1/trips/{tripId}/invites/{inviteId}
```

Join:

```text
GET  /api/v1/invites/{token}

POST /api/v1/invites/{token}/accept
```

---

# 54. Naming Conventions

Да се избягват vague класове:

```text
Helper
Utils
Manager
Processor
Data
Thing
Stuff
CommonService
```

когато няма много ясна причина.

Предпочитани:

```text
ExpenseSettlementCalculator

TripInvitationService

TripPermissionService

PhotoMetadataExtractor

TravelProfileCalculator
```

---

# 55. Function Naming

Не:

```text
process()
handle()
doStuff()
manageData()
```

А:

```text
createTrip()

generateInvite()

acceptInvitation()

calculateExpenseSettlement()

calculateTripCost()

extractPhotoCoordinates()

generateTravelProfile()
```

---

# 56. Frontend Structure

```text
src/

├── app/
│   ├── router/
│   └── providers/
│
├── features/
│   ├── auth/
│   ├── trips/
│   ├── invitations/
│   ├── members/
│   ├── map/
│   ├── replay/
│   ├── expenses/
│   ├── photos/
│   └── statistics/
│
├── components/
│
├── hooks/
│
├── services/
│
├── types/
│
├── utils/
│
└── pages/
```

Feature-specific кодът трябва да стои максимално близо до feature-а.

---

# 57. API Client

HTTP комуникацията трябва да бъде централизирана.

```text
services/apiClient.ts
```

Feature APIs:

```text
features/trips/api/tripsApi.ts

features/invitations/api/invitationsApi.ts

features/expenses/api/expensesApi.ts

features/photos/api/photosApi.ts
```

Да няма arbitrary `fetch()` заявки във всеки React component.

---

# 58. Error Handling

Global backend exception handler.

Примерни exceptions:

```text
TripNotFoundException

TripAccessDeniedException

InviteNotFoundException

InviteExpiredException

InviteRevokedException

InviteUsageLimitReachedException

AlreadyTripMemberException

InvalidTripDateException
```

Response:

```json
{
  "code": "INVITE_EXPIRED",
  "message": "This invitation has expired.",
  "timestamp": "..."
}
```

---

# 59. Database Tables

Минимално:

```text
profiles

trips

trip_members

trip_invites

trip_days

trip_stops

photos

expenses

expense_participants

trip_ratings
```

---

# 60. Important Database Constraints

Примери:

```text
trip_members:

UNIQUE(trip_id, user_id)
```

TripInvite:

```text
use_count >= 0
max_uses > 0
```

Trip:

```text
end_date >= start_date
```

Expense:

```text
amount > 0
```

Database constraints трябва да допълват backend validation-а, а не да разчитаме само на Java код.

---

# 61. Database Migrations

Flyway:

```text
V1__create_profiles.sql

V2__create_trips.sql

V3__create_trip_members.sql

V4__create_trip_invites.sql

V5__create_trip_stops.sql

V6__create_photos.sql

V7__create_expenses.sql

V8__create_trip_ratings.sql
```

---

# 62. Security Rules

Трябва да бъдат защитени:

* private trips;
* member list;
* invite management;
* photos;
* expenses;
* expense settlements;
* profile information.

Никога да не се разчита само на това frontend-ът да скрие бутон.

Например:

```text
frontend hides Delete Trip
```

не е security.

Backend задължително проверява:

```text
current user == OWNER
```

преди delete операция.

---

# 63. Invite Security Tests

Задължителни тестове:

```text
valid invite can be accepted

expired invite is rejected

revoked invite is rejected

invite exceeding max uses is rejected

duplicate member is rejected

viewer cannot create invites

editor cannot delete trip

owner can revoke invite
```

---

# 64. Unit Tests

Особено важни:

```text
TripInvitationServiceTest

TripPermissionServiceTest

ExpenseSettlementCalculatorTest

TripStatisticsServiceTest

TravelProfileCalculatorTest

TripReplayServiceTest
```

---

# 65. Integration Tests

Да има integration tests за:

```text
authentication

trip creation

private trip permissions

invitation creation

invitation acceptance

membership

expenses

public/private access
```

Database testing:

```text
Testcontainers PostgreSQL
```

---

# 66. README

Repository README:

```text
Project Title

Demo

Screenshots / GIF

What makes it different

Features

Trip Replay

Private Trip Invitations

Architecture

Tech Stack

Database Model

Security Model

API

Running Locally

Environment Variables

Tests

Deployment

Future Improvements
```

---

# 67. Architecture Documentation

В:

```text
/docs
```

да има поне:

```text
architecture.md

database-schema.md

authentication-flow.md

trip-invitation-flow.md
```

Invite flow може да бъде показан:

```text
Friend receives invite
       ↓
Open link
       ↓
Authenticated?
   ↙          ↘
 No           Yes
 ↓             ↓
Login        Preview
 ↓             ↓
Return       Accept
   ↘          ↙
   Membership
       ↓
     Trip
```

---

# 68. GitHub Repository Structure

```text
travel-memory-map/

├── frontend/
│
├── backend/
│
├── docs/
│   ├── architecture/
│   ├── database/
│   └── screenshots/
│
├── docker-compose.yml
│
├── README.md
│
└── .gitignore
```

---

# 69. Environment Variables

Secrets никога не се commit-ват.

Ignore:

```text
.env
application-local.properties
```

Repository съдържа:

```text
.env.example
application-example.properties
```

---

# 70. CI

При Pull Request:

```text
Frontend

install
lint
test
build
```

и:

```text
Backend

compile
unit tests
integration tests
```

---

# 71. GitHub Pages Deployment

При merge към main:

```text
React
   ↓
Vite Build
   ↓
dist/
   ↓
GitHub Pages
```

Routing трябва да бъде съобразен с GitHub Pages deployment.

---

# 72. Responsive Design

Приложението трябва да е usable на:

```text
Desktop
Tablet
Mobile
```

Особено внимание:

* Trip Map;
* Timeline;
* Replay;
* Expenses;
* Invite acceptance page.

---

# 73. UX States

Да има:

```text
Loading
Empty
Error
Success
Confirmation
```

states.

Пример:

```text
No trips yet.

Your next adventure starts here.

[ Create Trip ]
```

---

# 74. MVP — Phase 1

Първо се изгражда стабилна основа.

```text
Authentication

User profile

Create Trip

Edit Trip

Delete Trip

Private Trip

Trip Stops

Interactive Map

Timeline
```

---

# 75. MVP — Phase 2

След това:

```text
Trip Members

Invite Link Generation

Accept Invite

Invite Expiration

Invite Revocation

Roles

Permission System
```

Тази функционалност трябва да бъде завършена преди advanced social features.

---

# 76. MVP — Phase 3

```text
Photo Upload

Supabase Storage

EXIF Detection

Expenses

Shared Expenses

Expense Settlement
```

---

# 77. MVP — Phase 4

Изграждане на основния portfolio feature:

```text
Trip Replay
```

Когато това работи, вече съществува силна portfolio версия на приложението.

---

# 78. Phase 5

```text
World Scratch Map

Travel Statistics

Trip Ratings

Trip DNA

Travel Personality

Trip Comparison
```

---

# 79. Phase 6

```text
On This Day

Public Trips

Search

Advanced Filters

Polished Dashboard
```

---

# 80. Future Features

Възможни бъдещи additions:

```text
Automatic route calculation

Historical weather

Currency conversion

Import from Google Photos

Trip export

Shareable trip cards

PWA / offline mode

Achievements

Travel streaks
```

---

# 81. Не се допуска

Проектът не трябва да бъде:

```text
Login
Create Trip
Add Marker
Delete Trip

DONE
```

Това е CRUD demo.

Трябва да демонстрира реална business logic и system design.

---

# 82. Основни Portfolio Features

Ако recruiter разгледа проекта за 30 секунди, трябва да забележи поне три неща:

### 1. Trip Replay

```text
Your journey replayed on an interactive map.
```

### 2. Private Collaborative Trips

```text
Private trips can be shared securely through expiring invitation links.
```

### 3. Travel Intelligence

```text
Travel DNA
Statistics
Expenses
World Map
Travel Personality
```

---

# 83. Definition of Done

Проектът се счита за завършен, когато:

* потребител може да се регистрира;
* може да влезе;
* може да създаде trip;
* trip може да остане PRIVATE;
* owner може да генерира secure invite link;
* приятел може да отвори линка;
* ако няма акаунт, може да се регистрира и после да продължи invite flow;
* invite може да има expiration;
* invite може да има maximum uses;
* owner може да revoke-не invite;
* user може да стане member;
* permissions се проверяват от backend;
* private trip не става public чрез invite link;
* може да се добавят места;
* има карта;
* има timeline;
* могат да се качват снимки;
* има expenses;
* работят shared expenses;
* работи Trip Replay;
* има statistics;
* има Trip DNA;
* има World Scratch Map;
* има automated tests;
* frontend е deploy-нат;
* backend е deploy-нат;
* database е Supabase;
* repository има професионален README;
* repository може директно да бъде показан на интервю.

---

# 84. CV Description

Примерно описание:

> Developed a full-stack collaborative travel platform using React, TypeScript, Spring Boot, PostgreSQL and Supabase. Implemented interactive trip maps and replay, secure private-trip invitation links, role-based access control, photo storage, shared expense settlement, travel analytics and personalized travel profiles.

---

# 85. Основен принцип

Целта не е просто:

```text
"работи"
```

а repository-то да демонстрира:

```text
clean architecture

readable code

clear responsibilities

API design

authentication

authorization

database modelling

security

business logic

testing

deployment

maintainability
```

Всеки по-голям feature трябва да има ясна отговорност и да бъде достатъчно добре структуриран, че друг разработчик да може да отвори проекта и бързо да разбере как работи.
