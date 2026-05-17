# Module 1 — Auth Backend · Guide de mise en place

## Structure des fichiers à créer

```
src/main/
├── resources/
│   ├── application.yml
│   └── db/migration/
│       └── V1__init_auth.sql
└── java/ma/iaprice/backend/
    ├── shared/
    │   ├── BaseEntity.java
    │   ├── ApiResponse.java
    │   ├── GlobalExceptionHandler.java
    │   └── exception/
    │       ├── EmailAlreadyExistsException.java
    │       └── InvalidCredentialsException.java
    ├── entity/
    │   ├── Plan.java
    │   ├── Organization.java
    │   ├── User.java
    │   ├── OrgMember.java
    │   └── Subscription.java
    ├── repository/
    │   ├── UserRepository.java
    │   ├── OrganizationRepository.java
    │   ├── OrgMemberRepository.java
    │   ├── PlanRepository.java
    │   └── SubscriptionRepository.java
    ├── security/
    │   ├── TenantContext.java
    │   ├── UserPrincipal.java
    │   ├── JwtTokenProvider.java
    │   └── JwtAuthFilter.java
    ├── config/
    │   ├── JwtConfig.java
    │   └── SecurityConfig.java
    └── module/auth/
        ├── AuthDto.java
        ├── AuthService.java
        └── AuthController.java
```

## Ordre de mise en place

1. `application.yml` → remplace YOUR_SUPABASE_HOST, YOUR_SUPABASE_PASSWORD, le secret JWT
2. `V1__init_auth.sql` → dans `resources/db/migration/`
3. `shared/` → BaseEntity, ApiResponse, exceptions, GlobalExceptionHandler
4. `entity/` → dans cet ordre : Plan → Organization → User → OrgMember → Subscription
5. `repository/` → les 5 interfaces
6. `security/` → TenantContext → UserPrincipal → JwtTokenProvider → JwtAuthFilter
7. `config/` → JwtConfig → SecurityConfig
8. `module/auth/` → AuthDto → AuthService → AuthController

## Test Postman

### 1. Register
POST http://localhost:8080/auth/register
```json
{
  "email": "test@iaprice.ma",
  "password": "MonMotDePasse123!",
  "firstName": "Charaf",
  "organizationName": "Parapharmacie Atlas",
  "sector": "parapharmacie"
}
```
→ Attendu : 201 + token + plan: "free"

### 2. Login
POST http://localhost:8080/auth/login
```json
{
  "email": "test@iaprice.ma",
  "password": "MonMotDePasse123!"
}
```
→ Attendu : 200 + token

### 3. Me
GET http://localhost:8080/auth/me
Header: Authorization: Bearer <token>
→ Attendu : 200 + user + org + role + plan

## À faire après (hors Module 1)
- [ ] Ajouter ConfigurationPropertiesScan dans la classe principale (pour JwtConfig)
- [ ] Ajouter @EnableConfigurationProperties si besoin
- [ ] Mettre la vraie secret key JWT (min 32 chars) dans une variable d'env Railway



# Module 2 — Catalogue · Fichiers générés
#
## Points d'attention

1. **multi-tenant** : `TenantContext.getOrgId()` utilisé dans chaque service — alimenté par `JwtAuthFilter` existant. Rien à modifier côté sécurité.

2. **Migration Flyway** : s'exécute automatiquement au démarrage.

3. **`Product.setActive()`** : Le champ `isActive` est un `boolean` primitif dans Lombok. Le setter généré est `setActive(boolean)` — c'est normal, utilisez `p.setActive(true)`.

4. **Tri** : `ProductRepository.findByFilters` délègue le tri au `Pageable` Spring. Colonnes triables API → JPA : `name`, `brand`, `myPrice`.

5. **Import synchrone** : La spec dit traitement synchrone (HTTP 200 direct). Pour des fichiers >1000 lignes en prod, envisager un job asynchrone plus tard.

