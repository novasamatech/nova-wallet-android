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

Use the existing Room database, version **76**, with Room's generated **75→76** auto migration.
Room derives the migration from the exported schemas; there is no handwritten cache-table migration.
`bittensor_cache` stores serialized, parsed, validated DTOs plus `fetchedAt`, `expiresAt`, and optional
recommendation generation timestamp/block. Runtime holdings and the last complete portfolio prices use
separate snapshot resources in the same table. The existing dashboard summary remains a fallback;
restored holdings and prices never masquerade as fresh on-chain data. Cached cards stay visible instead of reverting to primary-sync skeletons. Like iOS, the catalogue
and dashboard do not add Saved data labels or background-refresh banners.

Room owns persistence, `BittensorResponseCache` owns HTTP reuse and flights, repositories own endpoint
validation/mapping, and existing screen flows/widgets own presentation. DI supplies one feature-scoped cache,
DAO, serializer, supervisor scope, foreground signal, and logger.

## Read and refresh behavior

- Fresh entries are reused until their HTTP deadline. `max-age` is capped at 300 seconds; `Age` and time
  spent waiting for the response consume freshness. Monotonic deadlines are used in-process; bounded
  wall-clock expiry restores them after restart.
- A validated no-store/no-cache response is an **application snapshot** with zero freshness. It may render,
  persist as stale, and be replaced; it never grants a new reusable lifetime. Quotes, fees, authentication,
  and transaction preflight are outside this cache.
- Cached-first reads return expired data immediately and start a background refresh. Observed catalogues,
  dashboard yields, portfolio history and account history emit cached data followed by refreshed data.
  Refresh failures retain visible values and stale status. No saved value still produces the existing
  recoverable loading error; unavailable data is never fabricated as an empty portfolio or zero APY.
- Observer timing follows expiry. Non-reusable responses/failures have a 30-second refresh floor.
  Foreground and relevant account invalidations wake observers; fresh entries still honour max-age.
- Flights are shared per complete request identity and account revision. Network calls never hold the
  cache mutex. A single shared semaphore caps alpha-yield transports at four across all callers.
  Cancelling one screen does not cancel another screen's flight; each flight, including its queue wait, has the existing
  Bittensor request timeout. History keeps provider pagination validation and inter-page spacing.
- The manual picker renders the backend catalogue directly, without waiting for runtime catalogue or
  SubtokenEnabled reads. Live eligibility/availability and transaction terms remain checked by the
  existing preflight/submission infrastructure. Optional external market metrics do not block stale browsing.
- Like iOS `SubtensorSubnetSelectInteractor`, the picker loads logos from the shared Bittensor config
  independently of its catalogue and updates visible rows when logos arrive. All subnet screens read
  the same observable config cache, including amount entry, portfolio/position, details, and buy/sell
  review/result. Logo updates do not reselect validators or change quote/signing terms. It uses the existing Coil
  image loader and its standard memory/HTTP disk cache, not the backend-response cache. Config caching
  remains process-local, matching iOS; a new offline process is not guaranteed to have logo URLs.

## Identity and invalidation

Namespace by the complete configured HTTPS infrastructure base URL; hash persisted request/account keys.
Changing origin clears other persisted namespaces and rejects old flights, including changing away and back.
Recommendation responses must pass semantic validation before advancing the persisted generation high-water
mark. A newer generation invalidates other generational entries; older in-flight responses cannot restore them.
The high-water mark survives normal response eviction. Regular responses are limited to 512 entries.

Account data uses the explicit chain/account identity, not the currently selected wallet ID. Account switching
starts another stream/key. The shared submission executor captures wallet/account/origin before handoff and
invalidates history after success or an unknown submitted outcome, including root stake/unstake/move/claim.
Signing/build failures do not invalidate. It retains coroutine cancellation and original dispatch outcomes.

Durable history invalidation happens **before** publishing the new generation to subscribers. Preserve the
existing five-minute, persisted indexer-settlement window: responses fetched inside it may render but must
not seed later subscriptions. Passive block/balance updates do not invalidate cost-basis history.
Runtime snapshots remain available as stale after a transaction, but revision tickets prevent a pre-transaction
load from overwriting them or being published as a fresh load. Cached prices keep a portfolio stale until
live pricing completes, so they cannot seed a fresh external-balance update.

## Verification

`BittensorResponseCacheTest` covers HTTP expiry/Age, no-store/no-cache, cached-then-fresh emission, per-key
coalescing and independent requests, global yield concurrency, changed origins, account invalidation,
runtime snapshot serialization/restart, and rejected obsolete snapshot writes. Existing backend, history,
settlement, position, portfolio and presentation contract suites are migrated to the shared cache.

`AppDatabaseMigrationTest` discovers all exported Room schemas and uses `MigrationTestHelper` to execute
and validate every available forward version pair, including 75→76. It shares the application's manual
migration registry, and Room supplies generated auto migrations. Adding another schema adds test cases
automatically; no migration-specific verification script is needed. It runs with the standard
`core-db` instrumentation tests (`:core-db:connectedDebugAndroidTest`), which require an Android device
or emulator. Schemas 75 and 76 are retained as migration fixtures. Data transformation migrations still
need focused data-preservation tests in addition to this shared schema check.

Device acceptance still requires killing the process, disabling connectivity, and reopening dashboard,
subnet picker, and positions. Also verify recovery after foreground/reconnect,
and a real submitted trade's average purchase-price refresh. JVM and schema checks do not prove those device,
authenticated backend, or on-chain scenarios. No installation or transaction is part of the automated checks.
