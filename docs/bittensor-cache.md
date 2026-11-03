# Bittensor response cache

## Reference and storage decision

Reviewed iOS at `a8aba9c54`, especially `BittensorApiResponseCache`, `BittensorApiOperationFactory`,
`SubtensorSubnetCatalogueOperationFactory`, and `SubtensorYieldOperationFactory`.
Reuse its endpoint/query identity, separate HTTP/component freshness, request-start deadlines,
expired-cache presentation, cross-route recommendation generation ordering, and account-specific invalidation.
UI delivery was also checked against iOS commits `454f8c350`, `f975b25e0`, and `f87d5ef7f`: show cached
values before remote values and retain them on refresh failure without additional cache-status copy.
The inspected iOS response cache is process-local and removes no-store entries. Android deliberately extends
that behavior to meet offline cold-start and stale-first-response requirements.

Use the existing Room database, version **76**. `response_cache` is the single durable store for HTTP
responses, local chain snapshots and recommendation generation metadata. There is no separate snapshot
table or `BittensorSnapshotStore`.

Each row contains the normalized URL/resource URI, nullable `metaAccountId`, validated JSON, fetchedAt,
expiresAt and an owner-scoped primary key. The primary key includes both URL and owner because the same
URL must coexist for multiple wallets and SQLite nullable composite keys would not enforce that identity.
`metaAccountId` references `meta_accounts.id` with `ON DELETE CASCADE` and an index. Null is reserved for
public catalogue, yields, validators and recommendation metadata. Positions, held-subnet price snapshots,
operation history, portfolio history and dashboard snapshots always carry the operation/observation wallet's explicit ID.
The current selected wallet is never consulted later to assign ownership to an arriving response.

`ResponseCache` owns persistence, reuse, snapshot admission and flights keyed by URL **and owner**.
Typed sources provide catalogue, validators, yields, portfolio history and operation history.
They share one `BittensorBackendClient`, which delegates storage to `ResponseCache` and keeps the common
origin, generation, request and observation policies. Generic client helpers are internal to the data
implementation; repositories receive only the typed source they need.
Each source exposes domain-specific methods and owns its URL construction, Retrofit API, validation
and cache policy. Repositories keep domain mapping and calculations; operation-history pagination stays
inside its source. Source APIs return typed payloads, never `CachedResponse` or expiry metadata.
The former `BittensorValidatorBackend` interpretation lives in `BittensorValidatorSource`.
HTTP expiry schedules refresh and does not rewrite backend-reported component freshness. Backend quality,
indexer settlement and live-chain validation remain separate domain concerns.
Catalogue presentation permits saved data; `BittensorSubnetCatalogueSource.valuationPrices` requires a
reusable HTTP response or a successful refresh. An expired response still labelled FRESH by the backend
cannot suppress runtime price fallback or mark an external balance current.
Generic get/peek/observe helpers belong to the shared client and return payloads directly.
Only operation-history aggregation uses internal lifetime-aware reads to schedule refresh at its earliest
page deadline. The assembled result lives in the active shared stream, without a second payload cache.
Settlement uses an explicit uncached operation-history read with the same origin and timeout checks.
Account endpoints use the wallet captured by their request/caller, including background wallet updates;
reading the currently selected wallet here would misattribute work after a wallet switch.
Private HTTP endpoints require a non-null owner; a missing owner cannot fall back to public data.

Local positions/prices use `bittensor://runtime/...` resource URIs containing chain and hashed coldkey.
Reading these entries is a direct Room read and never resolves global config or a backend URL. They have
zero freshness and restore as stale until a live chain read succeeds. Snapshot tickets capture resource
and owner; transaction invalidation retains the last snapshot but rejects older writes for that owner.
`withSnapshotTicket` bounds admission to the load's lifetime. Normal TTL/LRU eviction cannot invalidate
an active load; completing or cancelling it releases the ticket.
No Saved data labels or refresh banners are added.

`BittensorDashboardSnapshotSource` stores dashboard summaries and claimable rewards in the same Room
cache, under explicit wallet ownership. Existing Preferences snapshots are migrated on first read for
their chain and coldkey and removed only once Room can restore them. Source-local serialization keeps
legacy migration from overwriting a concurrent live update. Rewards are observed through Room, including
owner deletion, without a separate event bus. These snapshots share the 512-row limit and cascade deletion.

The single 75→76 auto migration creates `response_cache` with its owner foreign key and index.
The intermediate cache schemas were never released, so their data transformations and schema exports
are not retained. Wallet, balance and operations tables are unchanged. The exported 75 and final 76
schemas let Room generate and validate the additive migration without custom migration SQL.
Development builds that used the intermediate schemas are not a supported upgrade path.

Global configuration now persists its last successful response, separately for each config URL. If
refresh fails after process restart, infrastructure URL resolution can use the saved config; cancellation
still propagates. Conversion to the domain model succeeds before replacing the saved response, so an
incompatible successful HTTP response cannot destroy the offline fallback. Without a previously saved
config, the original failure remains visible to the caller.

## Read and refresh behavior

- Fresh entries are reused until their HTTP deadline. `max-age` is capped at 300 seconds; `Age` and time
  spent waiting for the response consume freshness. Monotonic deadlines are used in-process; bounded
  wall-clock expiry restores them after restart.
  A fresh initial value is emitted once. An observer joining an existing background refresh receives its
  result immediately, without waiting for the retry floor. Successful transports return their validated
  body directly; JSON conversion is only for storage and runs outside the shared registry lock.
- A validated no-store/no-cache response is an **application snapshot** with zero freshness. It may render,
  persist as stale, and be replaced; it never grants a new reusable lifetime. Quotes, fees, authentication,
  and transaction preflight are outside this cache.
- Cached-first reads return expired data immediately and start a background refresh. Observed catalogues,
  dashboard yields, portfolio history and account history emit cached data followed by refreshed data.
  Refresh failures retain visible values and backend-reported metadata. No saved value still produces the existing
  recoverable loading error; unavailable data is never fabricated as an empty portfolio or zero APY.
- Observer timing follows expiry. Non-reusable responses/failures have a 30-second refresh floor.
  Foreground and relevant account invalidations wake observers; fresh entries still honour max-age.
  The shared foreground stream skips its initial state before filtering for foreground transitions;
  consumers do not skip another event.
- Cache-table read failures behave as a cache miss. Write/trim failures are logged but do not discard a
  successfully fetched response or accepted runtime snapshot. Cancellation, obsolete request tickets
  and deleted private-data owners still reject delivery. Failed writes cannot extend the lifetime of
  an older saved payload. Observers retain their last delivered value across a later refresh failure.
- Flights are shared per normalized URL and nullable meta-account ID. Invalidating a URL detaches its old flight before deleting the row;
  detached responses cannot write back. Network calls never hold the
  cache mutex. The shared `BittensorAlphaYieldSource` caps its transports at four across reads and observations;
  the common client does not identify yield endpoints by URL.
  Cancelling one screen does not cancel another screen's flight; each flight, including its queue wait, has the existing
  Bittensor request timeout. History keeps provider pagination validation; inter-page spacing applies
  only to actual network requests, not reads of saved pages.
- The manual picker renders the backend catalogue directly, without waiting for runtime catalogue or
  SubtokenEnabled reads. Live eligibility/availability and transaction terms remain checked by the
  existing preflight/submission infrastructure. Optional external market metrics do not block stale browsing.
  Runtime catalogue observers also follow backend updates when their initial response came from that
  fallback. A healthy runtime catalogue still avoids backend requests. Failure to read optional backend
  cache metadata cannot prevent runtime fallback; only one successful chain catalogue is retained.
- Like iOS `SubtensorSubnetSelectInteractor`, the picker loads logos from the shared Bittensor config
  independently of its catalogue and updates visible rows when logos arrive. All subnet screens read
  the same observable config cache, including amount entry, portfolio/position, details, and buy/sell
  review/result. Logo updates do not reselect validators or change quote/signing terms. It uses the existing Coil
  image loader and its standard memory/HTTP disk cache, not the backend-response cache. Config caching
  remains process-local, matching iOS; a new offline process is not guaranteed to have logo URLs.
  `BittensorConfigSource` retries failed config loads every 30 seconds while observed and immediately on
  foreground, so a screen opened offline can acquire logos without being reopened.
- `BittensorPortfolioFxSource` persists public historical exchange rates by price asset, currency and
  period in the same response table. Its shared observation emits saved rates immediately, refreshes
  independently of portfolio history, and retries failures while observed. The history repository combines
  the two streams; a slow non-USD price provider no longer delays saved portfolio history and saved rates.
  Missing FX observations do not become fabricated fiat values.
  `refreshOnForegroundOrInterval` schedules the next FX load five minutes after success or 30 seconds after
  failure; returning to foreground wakes that wait. A load already in progress finishes without being
  cancelled or followed by a redundant foreground-triggered request. Collection cancellation stops the loop.

## Identity and invalidation

The request URL contains the HTTPS backend origin. Changing the configured base clears Bittensor response
URLs and rejects old flights, including changing away and back. Other features' response URLs are not deleted.
Recommendation responses must pass semantic validation before advancing the persisted generation high-water
mark, including required subnet ranking items and duplicate IDs. A newer generation invalidates other
generational entries; older in-flight responses cannot restore them. Equal generations do not rewrite the watermark.
The high-water mark survives normal response eviction: durable generation metadata uses
`expiresAt = Long.MAX_VALUE` and is excluded from trimming. Only the current backend's watermark is kept.
HTTP responses and local snapshots share one 512-row limit; in-memory entry states are also capped at 512.
These are entry-count limits, not byte quotas.
Operation-history pages use the shared response cache; no assembled-history LRU is retained. History metadata keeps at most 32
inactive entries plus currently observed/loading accounts; active requests hold a lease and cannot be
evicted. Generation numbers remain unique after eviction. Pending settlement deadlines survive metadata
eviction/restart in preferences; a successful post-window refresh clears the deadline, and recreating
state removes an already-expired deadline.
One pagination algorithm assembles saved and current pages. The active stream retains the last result
across refresh failures, and waits for foreground or its next deadline only after loading completes.
Foreground during a load does not queue another request.

Account data uses the captured meta-account ID and explicit chain/account identity, not a later wallet selection. Account switching
starts another stream/key. The shared submission executor captures wallet/account/origin before handoff and
invalidates history after success or an unknown submitted outcome, including root stake/unstake/move/claim.
Signing/build failures do not invalidate. It retains coroutine cancellation and original dispatch outcomes.
Position/price invalidation uses the submitted operation's wallet and coldkey even when backend/history
configuration is unavailable. If durable invalidation fails, known flights/tickets are detached first and
the cache instance stops reading persisted entries for the rest of its lifetime; the error is reported to
the caller. This prevents a failed deletion from silently restoring invalidated data.

Durable history invalidation happens **before** publishing the new generation to subscribers. Preserve the
existing five-minute, persisted indexer-settlement window: responses fetched inside it may render but must
not seed later subscriptions. Passive block/balance updates do not invalidate cost-basis history.
Runtime snapshots remain available as stale after a transaction, but revision tickets prevent a pre-transaction
load from overwriting them or being published as a fresh load. Cached prices keep a portfolio stale until
live pricing completes, so they cannot seed a fresh external-balance update.

`BittensorPricesSource` owns restoring, refreshing, saving and invalidating runtime portfolio prices.
It calls `BittensorPortfolioPriceSource` directly for backend/runtime selection; the redundant portfolio
loader is removed, and holdings selection/valuation remain pure mapping functions.
`BittensorPositionSource` owns restoring, validating, refreshing, saving and invalidating positions;
`BittensorPositionRemoteSource` is the runtime transport adapter. Each source shares its subscriptions.
Sources use `ComputationalCache.observeSharedFlow` to bind sharing to active collectors without repeating
scope-management wrappers. Stopping the last collector releases the producer even if its parent scope lives on.
The existing shared cache implementation is unchanged. New catalogue observations materialize load
failures inside the producer and unwrap them in the consumer, preserving the existing shared-flow boundary.
`RealBittensorPositionRepository` only combines positions with prices and delegates invalidation to those
sources. It has no dependency on `ResponseCache` or `ComputationalCache`.
A failed or partial price refresh keeps saved prices visible as stale and cannot replace a complete durable
snapshot. Partial results also check their revision ticket before being published.
`BittensorCacheKeys` defines each local resource key once for reading, writing and invalidation.
The submission executor calls the position and history owners directly; there is no separate cache invalidator.
Runtime catalogue reuse belongs to `BittensorSubnetSource`, optional external market reuse to
`BittensorSubnetMarketSource`, and logo/config state to `BittensorConfigSource`. The empty catalogue
repository has been removed; callers use the typed catalogue source directly.

## Verification

`ResponseCacheTest` covers HTTP expiry/Age, no-store/no-cache, cached-then-fresh emission, per-key
coalescing and independent requests, account isolation, runtime snapshot serialization/restart, and
rejected obsolete snapshot writes, storage failures, owner deletion, eviction during delivery and foreground
transitions. Pricing tests cover expired backend valuation and late partial responses after invalidation;
FX and config tests cover saved-first delivery and retry. `BittensorBackendClientTest` exercises typed endpoints, changed origins,
account invalidation and global yield concurrency. Existing backend, history,
settlement, position, portfolio and presentation contract suites are migrated to the shared cache.

`AppDatabaseMigrationTest` discovers all exported Room schemas and uses `MigrationTestHelper` to execute
and validate every available forward version pair, including 75→76. It shares the application's manual
migration registry, and Room supplies generated auto migrations. Adding another schema adds test cases
automatically; no migration-specific verification script is needed. It runs with the standard
`core-db` instrumentation tests (`:core-db:connectedDebugAndroidTest`), which require an Android device
or emulator. The six retained schema exports (1, 2, 8, 9, 75 and 76) produce 15 migration cases. Data transformation migrations still
need focused data-preservation tests in addition to this shared schema check.

`ResponseCacheDaoTest` checks owner/public row isolation, real foreign-key cascade, rejection of late
writes after wallet deletion and reactive removal of an observed snapshot. All 15 schema-pair migrations
and these four DAO tests passed on a fresh Android 37 emulator profile via `AndroidJUnitRunner`.
The assembled test APK was run directly with `adb shell am instrument`; no wallet APK was installed.

Device acceptance still requires killing the process, disabling connectivity, and reopening dashboard,
subnet picker, and positions. Also verify recovery after foreground/reconnect,
and a real submitted trade's average purchase-price refresh. JVM and schema checks do not prove those device,
authenticated backend, or on-chain scenarios. These checks do not install the wallet APK or submit transactions.

`BittensorOperationHistorySource` owns page loading/mapping, shared history streams and indexer-settlement
state. The redundant operation-history repository has been removed.
It invalidates operation pages itself and delegates chart invalidation to `BittensorPortfolioHistorySource`;
each source owns its endpoint path. Alpha-yield reads use `BittensorAlphaYieldSource`, with no dependency
on history pagination or its inter-page delay.
