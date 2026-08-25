# StockPulse Implementation Plan (2.5-Hour Hackathon)

==================================================
PART 1 — HACKATHON STRATEGY
==================================================
**150-Minute Strategy & Allocation**
- **0–10m**: Scaffold
- **10–30m**: Entities + DB
- **30–50m**: APIs
- **50–70m**: Commerce rules
- **70–90m**: AI + fallback
- **90–110m**: Agentic loop
- **110–140m**: React UI
- **140–150m**: Tests + ADR + demo

**Priority Ordering:**
- **P0 (Absolute Required)**: Domain model, H2 DB, Agentic loop events, Rules fallback, API endpoints, Accept/Reject logic, Floor UI, minimal JUnit tests for rule logic/triggers.
- **P1 (Important)**: Idempotency protections, robust error handling, ADR documentation, AI Prompts & LLM Gateway.
- **P2 (Bonus/Only if time)**: React UI ceiling features (charts), SSE streaming API.

**What we will NOT build:**
- SSE streaming token endpoints.
- Full catalog CRUD UI or storefront carts.
- Competitor scraping or automated POs.
- Complex React state management (Redux) or routing.

==================================================
PART 2 — FINAL TECH STACK
==================================================
**Backend**:
- **Java 21**: LTS release, clean syntax (Records, pattern matching).
- **Spring Boot 3.x**: Rapid API scaffolding, built-in async/event support.
- **Spring Data JPA**: Fast database mapping without writing SQL.
- **Maven**: Standard build tool, well understood.
- **H2 database**: In-memory, zero-setup, perfect for hackathons/demos.

**Frontend**:
- **React 18 + Vite**: Lightning-fast local dev server.
- **JavaScript (ES6)**: Simpler than TS for a 2.5-hour sprint, avoids typing overhead.

**AI**:
- **Gemini API**: Highly capable, fast. Optional if the key is missing (falls back gracefully to rules).
- **LLM Gateway Abstraction**: Allows easy swapping/fallback and isolates external dependencies.

==================================================
PART 3 — SYSTEM ARCHITECTURE
==================================================
```text
[ React UI (Merchandiser Console) ]
        ↓ (HTTP REST)
[ REST Controllers (Products, Suggestions) ]
        ↓
[ Services (ProductService, SuggestionService) ]  <-- (Dispatches ApplicationEvent)
        ↓
[ ApplicationEventPublisher ]
        ↓ (@Async @EventListener)
[ AgenticRecommendationListener ]
        ↓
[ Commerce Advisor (Strategy Selector) ]
        ↓
   +----+----+
   |         |
[ AI ]    [ Rules ]
   ↓         ↓
[ Suggestion Generation & Validation ]
        ↓
[ Spring Data JPA Repositories ]
        ↓
[ H2 Database ]
```

**Responsibilities**:
- **React UI**: Polling backend, showing suggestions, firing simulate sale/accept actions.
- **Controllers**: Thin wrappers translating HTTP to Service calls.
- **Services**: Business logic, DB persistence, event publishing.
- **Events/Listeners**: Decouples API response from the background AI agentic loop.
- **Commerce Advisor**: Routes the suggestion request to the active strategy (AI or Rules).
- **AI/Rules**: Returns standardized price/reorder objects.

==================================================
PART 4 — PROJECT STRUCTURE
==================================================
```text
/
├── backend/
│   └── src/main/java/com/stockpulse/
│       ├── controller/   (ProductController, SuggestionController)
│       ├── service/      (ProductService, SuggestionService)
│       ├── domain/       (Product, PricingSuggestion, ReorderSuggestion)
│       ├── repository/   (ProductRepository, SuggestionRepositories)
│       ├── commerce/     (CommerceAdvisor, PricingStrategy, ReorderStrategy)
│       ├── ai/           (LLMGateway, GeminiClient, DTOs)
│       ├── event/        (InventoryLowEvent, DemandSpikeEvent, AgenticListener)
│       └── config/       (AsyncConfig, RestTemplateConfig)
├── frontend/
│   ├── src/              (App.jsx, ProductList.jsx, SuggestionCard.jsx)
│   └── package.json
├── ADR.md
├── README.md
└── PLAN.md
```

==================================================
PART 5 — DOMAIN MODEL
==================================================
**1. Product**
- `id` (String, PK)
- `sku` (String, unique)
- `name` (String)
- `category` (Enum: ELECTRONICS, APPAREL, HOME)
- `currentPrice` (BigDecimal)
- `stockLevel` (Integer)
- `reorderThreshold` (Integer)
- `demandVelocity` (Integer)
- `status` (Enum: ACTIVE, PRICE_REVIEW_PENDING, OUT_OF_STOCK)
- *Extension (Sprint 2)*: `costPrice` (BigDecimal, nullable)

**2. InventorySnapshot**
- `id` (Long, PK)
- `productId` (String, FK)
- `timestamp` (Instant)
- `stockLevel` (Integer)
- `demandVelocity` (Integer)
- `triggerReason` (Enum: INVENTORY_LOW, DEMAND_SPIKE, MANUAL)

**3. PricingSuggestion**
- `id` (Long, PK)
- `productId` (String, FK)
- `currentPrice` (BigDecimal)
- `recommendedPrice` (BigDecimal)
- `direction` (Enum: INCREASE, DECREASE, HOLD)
- `confidence` (Double)
- `reasoning` (String, length 1000)
- `status` (Enum: PENDING, ACCEPTED, REJECTED)
- `triggerReason` (Enum: INVENTORY_LOW, DEMAND_SPIKE, MANUAL)

**4. ReorderSuggestion**
- `id` (Long, PK)
- `productId` (String, FK)
- `currentStock` (Integer)
- `recommendedQuantity` (Integer)
- `confidence` (Double)
- `reasoning` (String, length 1000)
- `status` (Enum: PENDING, ACCEPTED, REJECTED)
- `triggerReason` (Enum: INVENTORY_LOW, DEMAND_SPIKE, MANUAL)

==================================================
PART 6 — STATE MACHINES
==================================================
**Product Status**:
- `ACTIVE` → `PRICE_REVIEW_PENDING`: Caused by Agentic Loop generating a PENDING PricingSuggestion.
- `PRICE_REVIEW_PENDING` → `ACTIVE`: Explicitly triggered when there are NO MORE `PENDING` pricing suggestions for the product (checked upon any suggestion Accept/Reject).
- `ACTIVE` → `OUT_OF_STOCK`: Caused by simulated sale dropping stock to 0.

**PricingSuggestion / ReorderSuggestion Status**:
- `PENDING` → `ACCEPTED`: Human clicks Accept. Triggers side effects (price change or stock increase). Terminal state.
- `PENDING` → `REJECTED`: Human clicks Reject. No side effects. Terminal state.
*Forbidden*: ACCEPTED → REJECTED.

==================================================
PART 7 — API CONTRACT
==================================================
- **POST /api/products**: Create product (returns created Product).
- **GET /api/products**: Returns all products. Query parameters `status` and `category` applied if present.
- **GET /api/suggestions**: Returns suggestions. Query parameter `status=PENDING` used by UI for polling.
- **POST /api/products/{id}/orders**: 
  - *Purpose*: Simulate sale.
  - *Side Effect*: stock--, velocity++. Fires agentic events if thresholds crossed.
- **PATCH /api/products/{id}/stock**: 
  - *Purpose*: Manual stock fix. 
  - *Side Effect*: Updates stock. Fires event if low.
- **POST /api/products/{id}/suggest-pricing**:
  - *Purpose*: Manual trigger. Returns PricingSuggestion sync.
- **POST /api/products/{id}/suggest-reorder**:
  - *Purpose*: Manual trigger. Returns ReorderSuggestion sync.
- **PATCH /api/pricing-suggestions/{id}**:
  - *Body*: `{"status": "ACCEPTED"}`
  - *Side Effect*: Updates product `currentPrice`, re-evaluates product status (back to ACTIVE if no pending pricing suggestions).
- **PATCH /api/reorder-suggestions/{id}**:
  - *Body*: `{"status": "ACCEPTED"}`
  - *Side Effect*: Updates product `stockLevel`, re-evaluates product status.

==================================================
PART 8 — COMMERCE ENGINE DESIGN
==================================================
**Unified vs Split (Chosen: Unified CommerceAdvisor)**
- *Why Unified*: Faster execution (one LLM prompt returns both price/reorder), aligns perfectly with the event loop that needs both suggestions simultaneously. Easier to build in 2.5 hours.
- *Interface*: `CommerceStrategy.java`
  - `SuggestionPair generateSuggestions(Product p, String triggerReason, int categoryAvgVelocity)`
- *RuleBasedStrategy*: 
  - *Pricing*: If stock < threshold → currentPrice * 1.10. If velocity > 2x avg → currentPrice * 1.05. Else HOLD.
  - *Reorder*: Max((threshold * 3) - stock, 1).
- *AIStrategy*: Calls LLM Gateway, parses JSON into `SuggestionPair`.
- *Switching*: Application property `commerce.strategy=RULES` by default (safe if no API key). Can be flipped to `AI`.

==================================================
PART 9 — AI ARCHITECTURE
==================================================
**LLMGateway**: Thin RestClient wrapper targeting Gemini API.
**Context sent to Gemini**: JSON containing Product details, Category Average Velocity, and Trigger Reason.

**Prompt 1: INVENTORY_LOW**
- *Objective*: Decide between raising price to slow velocity (protect stock) vs clearance. Recommend reorder.
- *Context*: Product payload.
- *Output Schema*: Strict JSON containing `recommendedPrice`, `direction`, `confidence`, `reasoning`, `recommendedQuantity`.

**Prompt 2: DEMAND_SPIKE**
- *Objective*: Capitalize on velocity spike with modest price increase, preemptive reorder.
- *Context*: Product payload.
- *Output Schema*: Same JSON.

**Validation Rules**: 
- Price > 0, Quantity > 0, Confidence 0.0-1.0. Price change < 50% limit.
- JSON parsed using Jackson.

==================================================
PART 10 — FALLBACK DESIGN
==================================================
If `LLMGateway` throws exception (Timeout, 429 Quota, Malformed JSON, bounds failure, missing API key):
- Catch block in `AIStrategy` logs error.
- Immediately delegates to `RuleBasedStrategy.generateSuggestions(...)`.
- The rule-based suggestions are returned to the Agentic Loop.
- *Result*: The system NEVER silently drops the request. UI shows a suggestion with `confidence: 0.9` (or clearly labeled Rule-based confidence) and reasoning: "Rule-based fallback applied."

==================================================
PART 11 — AGENTIC LOOP
==================================================
**Flow**: OBSERVE → REASON → ACT → HUMAN CHECKPOINT
1. *Trigger A (Low Stock)*: `ProductService` detects stock < threshold after an order/patch.
   - Publishes `InventoryLowEvent`.
2. *Trigger B (Demand Spike)*: `ProductService` detects velocity > 3x average.
   - Publishes `DemandSpikeEvent`.
3. *Async Listener*: `@Async @EventListener` in `AgenticListener` catches event.
   - HTTP thread returns 200 OK immediately.
4. *Reason & Act*: Listener calls `CommerceAdvisor`. Suggestions created in DB as `PENDING`.
5. *Human Checkpoint*: Suggestions wait in DB. UI polls and displays them.

==================================================
PART 12 — IDEMPOTENCY
==================================================
- **Protection**: Before generating new async suggestions, `SuggestionService` checks DB:
  `repository.existsByProductIdAndStatusAndTriggerReasonAndType(id, PENDING, reason, type)`.
- Explicit check matches: Product ID + Trigger Reason + Suggestion Type + Status=PENDING.
- If true, abort loop safely.

==================================================
PART 13 — HUMAN CHECKPOINT
==================================================
- Live price/stock NEVER change during the Async loop.
- Only the `PATCH /api/pricing-suggestions/{id}` endpoint calling `SuggestionService.accept(...)` mutates `Product.currentPrice`.
- Only `PATCH /api/reorder-suggestions/{id}` calling `SuggestionService.accept(...)` mutates `Product.stockLevel`.

==================================================
PART 14 — FRONTEND PLAN
==================================================
- **React (Vite) + basic CSS**: 
  - `App.jsx`: Main container, `setInterval` polling every 5s for `GET /products` and `GET /suggestions`.
  - `ProductList.jsx`: Table showing SKU, Stock, Velocity, Price. Includes a "Simulate Sale" button.
  - `PendingSuggestions.jsx`: Renders cards for PENDING suggestions.
    - Shows badges (`INVENTORY_LOW` in red, `DEMAND_SPIKE` in orange).
    - Shows AI/Rule Reasoning text.
    - Two buttons: `[Accept]` `[Reject]`.

==================================================
PART 15 — SEED DATA
==================================================
- `data.sql` embedded in Spring Boot (H2 init).
- Use PS Addendum A.
- **INVENTORY_LOW demo**: `PRD-003` (Cotton T-Shirt, stock 8, threshold 15).
- **DEMAND_SPIKE demo**: `PRD-008` (Hoodie, stock 11, threshold 12, velocity 15). Sending 1-2 orders here will spike it past category averages.

==================================================
PART 16 — TESTING PLAN
==================================================
- *P0 Tests (JUnit)*:
  - `RuleBasedStrategyTest`: Verify pricing and reorder logic calculations.
  - `ProductServiceTest`: Verify `PATCH /stock` and `POST /orders` properly publish `InventoryLowEvent` / `DemandSpikeEvent`.
  - `SuggestionServiceTest`: Verify Accept/Reject logic updates product state and transitions back to `ACTIVE` cleanly.
- *P1 Tests*: Async loop execution flow.
- *Manual Testing*: Verify API responses via cURL/Swagger, ensure UI handles everything.

==================================================
PART 17 — ADR.md PLAN
==================================================
1. **Commerce Logic Location**: Option: Service vs Strategy. Decision: Pluggable Strategy pattern to allow runtime hot-swaps.
2. **Unified vs Separate Calls**: Option: 2 LLM calls vs 1. Decision: Unified `CommerceAdvisor` returning both price/reorder for latency reduction and context coherence.
3. **AI Fallback**: Option: Silent fail vs Rules. Decision: Rule-based fallback ensures agentic loop always acts.
4. **Agentic Decoupling**: Option: Sync vs Async events. Decision: `@Async ApplicationEventPublisher` to prevent blocking the checkout/order API.
5. **Idempotency**: Prevent redundant LLM calls using DB-level PENDING checks.

==================================================
PART 18 — README PLAN
==================================================
- Setup instructions (Java 21, npm run dev).
- Environment Variables (`GEMINI_API_KEY`). Defaults to Rule-based if missing.
- Exact 4-step Demo walkthrough script.

==================================================
PART 19 — EXACT IMPLEMENTATION ORDER
==================================================
- **PHASE 1 (10m)**: Spring Initializr + Vite React scaffold. `data.sql` seed.
- **PHASE 2 (20m)**: Entities (`Product`, `PricingSuggestion`, `ReorderSuggestion`, `InventorySnapshot`) + Repositories.
- **PHASE 3 (20m)**: Services & APIs for basic CRUD and Simulate Order.
- **PHASE 4 (15m)**: Commerce Strategy Interface + RuleBased Strategy + Minimal JUnit Tests.
- **PHASE 5 (20m)**: Agentic Event Loop (`@Async` listeners + Idempotency checks).
- **PHASE 6 (15m)**: Accept/Reject side-effect logic + status transitions.
- **PHASE 7 (20m)**: AI Gateway & Prompts (Fallback handling).
- **PHASE 8 (30m)**: React UI (Polling, Lists, Accept/Reject buttons).
- **PHASE 9 (10m)**: Documentation (ADR.md, README.md).

==================================================
PART 20 — 150-MINUTE EMERGENCY PLAN
==================================================
- **If behind at 60m**: Stop all AI implementation. The app will rely purely on the `RuleBasedStrategy`. The agentic loop still proves asynchronous reasoning + UI. Focus exclusively on finishing the backend APIs and React frontend.
- **If behind at 90m (UI incomplete)**: Drop badges, drop reasoning display, just show basic text and an Accept button.

==================================================
PART 21 — DEMO PLAN
==================================================
1. **Show UI**: Explain dashboard displaying products (PRD-003 and PRD-008).
2. **Simulate Event**: Click "Simulate Sale" on PRD-008.
3. **Observe Loop**: Wait 5 seconds (polling). Explain backend is firing `@Async` event.
4. **Suggestions Appear**: Show the new PENDING suggestions with badges and reasoning.
5. **Accept**: Click "Accept" on Pricing. Show the Product list updating the live price, and product returning to ACTIVE.
6. **Explain Architecture**: Point out that this is an agent proposing actions, not automating them silently, and handles rules gracefully when AI is unavailable.

==================================================
PART 22 — INTERVIEW / WALKTHROUGH PREPARATION
==================================================
- **Spring Boot**: Auto-configured framework providing dependency injection and embedded Tomcat.
- **JPA / Repositories**: ORM layer mapping Java objects to H2 relational tables automatically.
- **Strategy Pattern**: Interface `CommerceStrategy` allowing hot-swapping between `RuleBased` and `AI` implementations without touching caller code.
- **@Async / Events**: Decouples the order request thread from the slow generation thread.
- **LLM Fallback**: Wrapping LLM calls in try-catch to guarantee a rule-based response.
- **Idempotency**: Preventing duplicate async loops by checking DB state before acting.

==================================================
PART 23 — FINAL FILE-BY-FILE BLUEPRINT
==================================================
- `backend/.../domain/Product.java` (Entity, state enums)
- `backend/.../domain/InventorySnapshot.java` (Entity for tracking stock changes)
- `backend/.../domain/Suggestion.java` (Base mapped superclass or separate entities)
- `backend/.../repository/ProductRepository.java` (Spring Data interface)
- `backend/.../repository/InventorySnapshotRepository.java` (Spring Data interface)
- `backend/.../service/ProductService.java` (Handles stock patch, publishes events)
- `backend/.../event/InventoryLowEvent.java` (Simple POJO)
- `backend/.../event/AgenticListener.java` (@Async event handlers)
- `backend/.../commerce/CommerceAdvisor.java` (Interface)
- `backend/.../commerce/AIStrategy.java` (Calls LLMGateway)
- `backend/.../commerce/RuleStrategy.java` (Fallback logic)
- `backend/.../controller/ProductController.java` (REST endpoints)
- `backend/src/main/resources/data.sql` (Seed data)
- `frontend/src/App.jsx` (Main UI loop)
- `ADR.md` (Design records)
- `README.md` (Setup instructions)
