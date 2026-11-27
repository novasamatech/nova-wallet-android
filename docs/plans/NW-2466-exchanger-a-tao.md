# NW-2466 — Exchanger A integration

## NW-2500 — текущий план интеграции

- [x] Подключить пары из каталога Exchanger A через `AssetExchange` к общему графу. Разрешить шаги до провайдера и завершать маршрут на нём.
- [x] Вернуть Token select на `SwapService`. Учитывать направление Pay/Receive при выборе первого актива.
- [x] Убрать отдельный роутер формы. Общий поиск и котировка должны поддерживать Asset Hub → TAO и Hydration → Asset Hub → TAO для обеих сторон ввода.
- [x] Проверить маршруты, лимиты, недоступность провайдера и обычные свапы тестами и сборкой.
- [ ] NW-2502 — Когда экран подтверждения начнёт обрабатывать `ExternalSwapQuote`, перевести форму и подтверждение на общий `quote()`. Удалить `preview()` из `SwapService` и `SwapInteractor`, затем убрать оставшийся без вызовов код временной интеграции. Проверить оба типа котировок.

Это часть NW-2500. Отдельно остаются проверка доступа по региону, транспорт создания и статуса ордера и проверка на устройстве.

Проверено локально 25 ноября: debug APK и ktlint, 241 модульный тест прошёл, существующий тест производительности графа пропущен. Проверки котировки использовали тестовые ответы провайдера.
Для ревью по порядку: `ExchangerAAssetExchange.kt` → `RealSwapService.kt` → `SwapTokenSelectionInteractor.kt`.

Agreed scope on 2026-11-24. Android implementation starts from `origin/master`
at `9c3a83db1`, in branch `feature/NW-2466-changenow-tao`.
Keep implementation changes in small, separately reviewable increments.
Leave each increment uncommitted until the user reviews and approves it.

## Release 1 — Buy TAO

Buy native TAO on Bittensor with any backend-enabled asset on Polkadot Asset Hub,
in the existing native Swaps form and shared token picker. Get TAO / Swap for
TAO can preselect the receive asset. The backend determines eligible pay assets.
Both Pay and Receive amounts are editable.
Receive remains an estimate at a floating exchange rate.

| Jira task | Release 1 | Release 2 |
| --- | --- | --- |
| [NW-2500](https://novasama-technologies.atlassian.net/browse/NW-2500) | Attested Exchanger A client, backend asset availability, Asset Hub → TAO selection, access check, service errors and country refusal at Confirm | General Swaps catalogue and routing, other supported pairs, full Unavailable group |
| [NW-2501](https://novasama-technologies.atlassian.net/browse/NW-2501) | Pay/Receive quotes, approximate output, min/max, Max with deposit network fee, rate, route, fee and duration values | Full Rate, Route and Network fees sheets |
| [NW-2502](https://novasama-technologies.atlassian.net/browse/NW-2502) | Review, paying wallet's TAO recipient, provider-specific rows, one-time terms agreement | Apply the same flow to further networks and directions |
| [NW-2503](https://novasama-technologies.atlassian.net/browse/NW-2503) | Create order, save before deposit, transfer, status tracking, process restoration and all result states | Further networks and integration with multi-step routes |
| [NW-2504](https://novasama-technologies.atlassian.net/browse/NW-2504) | Durable local order storage, simple TAO exchange list and details, unfinished indicator, history accessible during country refusal | Unified history including new on-chain swaps, filters, full operation details and Continue |
| [NW-2505](https://novasama-technologies.atlassian.net/browse/NW-2505) | Existing account requirements and applicable unsupported-wallet alerts before creating an order | Add/import account from either side of Token select and restore the selected pair |

Tasks spanning both releases remain partially implemented after release 1.
In particular, the unified on-chain history required by NW-2504 is deferred
by this release split.

## Release 2 — Broader Swaps support

- Other supported directions, including selling TAO and Ethereum pairs.
- General Exchanger A token selection, routing and availability explanations.
- Execute multi-step funding from Hydration and other supported sources through Asset Hub. The graph already exposes these paths for selection and preview.
- [NW-2523](https://novasama-technologies.atlassian.net/browse/NW-2523)
  multi-step routes. Implement Exchanger A as the final step first, then routes
  with subsequent on-chain steps. Persist progress, re-quote using the actual
  payout, request another signature and resume through Continue in History.
- Full quote-detail sheets, unified swap history and filters.
- Adding missing accounts from Token select.

## Implementation sequence

1. NW-2500 — Typed client and contract tests, starting with assets and quotes
   from either input side. Reuse Retrofit and the attested network creator.
2. NW-2504 foundation — Room storage and migration, explicit wallet identity,
   immutable order identifiers and observable operation records.
3. NW-2501 / NW-2502 — Existing form and Review adapted for DOT → TAO,
   both amount inputs, validation, terms and destination account.
4. NW-2503 — Create and persist an order, submit the deposit through the
   existing SendUseCase, associate its transaction hash and track status.
5. NW-2504 UI / NW-2505 checks — List, details, recovery after restart,
   wallet/account eligibility and country-refusal behavior.
6. Verify native and non-native Asset Hub inputs, including DOT/USDT/USDC,
   DOT fee balances, Max, input precision and all final/non-final outcomes.

Each step may use several small increments. Include related tests with the
behavior they verify. Commit only after user approval. Keep this plan's progress
section current.

## Reuse and ownership

- Exchanger A transport and DTOs belong to feature-swap-impl/data/exchanger.
- Reuse infrastructure's profile-3 attestation and configured backend origin.
- Reuse SwapTokenSelectionInteractor, the Swaps amount/form machinery,
  confirmation widgets and existing navigation payloads.
- Send deposits through the wallet API's SendUseCase and asset transfer APIs.
- Store operation records in core-db. A shared operation owner supplies
  Execution, History and Operation, with lifecycle-driven refresh.
- Preserve operation and wallet identity across navigation, signing and
  recovery. Domain mapping validates financial values before they are used.

## Invariants and verification

- A quote never creates an order. Pay and Receive requests send exactly one
  amount, as a decimal string in token units.
- API precision is at most min(asset precision, 8) fractional digits. The quote
  boundary rejects excess precision. The form must enforce this precision and
  explicitly round Max down before requesting a quote.
- A backend provider fee already included in the estimate is counted once.
  The deposit's on-chain fee is calculated separately.
- Create an order only after Confirm. Persist a successful response before
  sending funds. Never automatically repeat creation after an uncertain result.
- A repeat tap, restored screen or lost submission response cannot trigger a
  second deposit. Retain unknown submission outcomes for reconciliation.
- Refresh unfinished orders on launch and foreground entry. Poll only while
  a relevant screen is visible, stop in the background and at terminal states.
- Unknown provider statuses remain trackable. On hold is non-final.
- History remains reachable under country refusal and service failures.
- Test DTO/HTTP contracts, financial boundaries, stale quotes, wallet changes,
  cancelled signatures, lost responses and restart recovery.
- Run targeted unit tests and compilation per code increment. Run applicable
  lint and migration checks. Device, live API and real exchange verification
  are separate milestones and require their own evidence.

## Backend dependencies

The existing service and simulator are tracked in
[NW-2463](https://novasama-technologies.atlassian.net/browse/NW-2463).
Use the accepted
[client contract](https://github.com/novasamatech/nova-infrastructure/blob/main/exchanger-a/docs/contracts/client-api.md)
and OpenAPI when implementing transport.

- Obtain the dev endpoint, working app attestation and simulator scenarios.
- Verify DOT/USDT/USDC → TAO availability with the partner integration.
- [NW-2524](https://novasama-technologies.atlassian.net/browse/NW-2524) owns
  access-check and the country-refusal contract. Keep country refusal distinct
  from connectivity and authentication failures.
- [NW-2525](https://novasama-technologies.atlassian.net/browse/NW-2525) owns
  Cloudflare restrictions.
- The current client contract supplies pollAfterSeconds, while Jira mentions
  approximately 10 seconds. Proposed implementation follows server intervals;
  resolve this discrepancy before finalizing polling behavior.
- The current contract describes HTTP 451 / region_unavailable; the newer
  access-check task describes indirect Cloudflare signals. Do not guess that
  endpoint or classify every HTTP refusal as a country restriction.

## Progress

- [x] Create the task worktree and branch from updated origin/master, without upstream.
- [x] Record the agreed plan.
- [x] Assets and Pay/Receive quote transport with contract tests and attested DI.
- [x] Domain quote mapping and financial validation for Pay and Receive.
- [x] Map the backend catalogue to wallet assets and select release-1 TAO pairs.
- [x] Let reusable amount fields accept provider-specific integer/fraction limits.
- [x] Merge PAH → TAO candidates into the shared Pay and Receive token picker.
- [x] Quote the selected PAH → TAO pair in the native Swaps form.
- [ ] Durable order storage and migration.
- [ ] TAO form and Review.
- [ ] Order creation, deposit and tracking.
- [ ] Simple history and wallet checks.
- [ ] Native/non-native Asset Hub integration and release verification.

### First implementation increment

Added ExchangerAApi, typed transport models and ExchangerAQuoteSource for assets
and quotes. Swap DI provides the source through the existing attested
NetworkApiCreator and InfrastructureUrls. Each request resolves the current
configured origin. Amount factories send exactly one edited side.

The transport preserves decimal strings, optional fees/duration, limit
responses, HTTP failures and cancellation. Domain validation and UI consumption
are subsequent increments. Order creation will be added with durable storage.

Verification on 2026-11-24 passed for feature-swap-impl compilation, all 10
ExchangerAQuoteSourceTest cases, root ktlint and instruction routing checks.
The tests exercise Retrofit serialization through an in-process HTTP fixture.
Live attestation, dev service availability, device flows and real exchanges
remain unverified. Independent review remains pending.

### Second implementation increment

Added ExchangerAQuoteRepository and typed quote/out-of-range domain results.
Request mapping reuses SwapQuoteArgs, keeps the edited side and wallet identity,
and converts integer balances to exact decimal strings. Invalid amounts fail
before HTTP. Response mapping validates required fields, precision, positive
amounts, pay limits and duration ranges. A Pay quote must preserve the requested
payment. A Receive quote retains the backend's estimated output.

Provider fees carry their respective pay/receive assets and are not deducted
again. Absent fees stay absent, distinct from zero. Transport failures and
cancellation propagate. Catalogue, form and on-chain deposit fee calculation
remain subsequent increments.

ExchangerAAmountConstraints owns the request's integer digit limit and per-asset
fraction digit limit, for reuse by the amount fields. Request mapping checks
numeric precision and range before serialization; response format validation
remains at the remote boundary. Input-field wiring is still pending.

Verification on 2026-11-24 passed for feature-swap-impl compilation, all 27
Exchanger A tests (17 repository/mapping and 10 transport), and root ktlint.
No device or live-service verification was performed.

### Third implementation increment

Added ExchangerAAssetsRepository to resolve backend identifiers through the
existing ChainRegistry. It preserves send/receive flags, excludes unknown
assets and disabled chains, and propagates failures and cancellation.
Required numeric/boolean DTO fields are boxed so missing JSON fields cannot
silently become asset ID zero or false. No cache is added to the repository.
The source unwraps the assets response and filters null entries, returning a
non-null list of remote assets. A missing assets list remains an error.

ExchangerATaoCatalogue selects candidate Asset Hub → native TAO pairs by chain
identity and backend direction flags, without a paying-token allowlist.
New Asset Hub tokens become candidates when both the backend and the wallet
chain configuration include them. The general catalogue
retains other known assets for later releases. A candidate pair still requires
a successful quote; it does not establish wallet eligibility or region access.
The catalogue and interactor are wired through feature DI. UI consumption,
account checks and access-check remain pending.

Verification on 2026-11-24 passed for feature-swap-impl compilation, all 40
Exchanger A tests (11 catalogue, 17 quotes and 12 transport), and root ktlint.
Tests cover direction flags, release scope, unknown/disabled assets, refreshed
availability, malformed fields, failures and cancellation. Initial test runs
needed nullable-field assertions and a Mockito fixture fix; the final run passed.
Device and live-service verification remain pending.

### Fourth implementation increment — configurable amount input limits (NW-2501)

SwapAmountInputView accepts a sealed DecimalInputConstraints model with Unlimited,
FractionOnly and Bounded modes; nullable numbers do not encode validation modes.
Exchanger A resolves Bounded limits per asset, capping token precision at its own
8-digit request limit. Quote validation consumes these same limits. Slippage
settings carry their percent precision through navigation to both the edit
filter and the saved-value formatter. The shared DecimalInputFilter in common
checks typing, pastes, replacements and separator deletion,
and changing constraints replaces the prior swap filter while retaining other
filters. Setting limits does not rewrite an existing amount; corrective edits
can reduce excess digits after a limit change. Existing on-chain amount fields
retain their current unrestricted configuration.

Tests exercise ExchangerAAmountConstraints for assets with 6, 9 and 18 decimals,
the 15-digit integer boundary, malformed input, leading zeros, correction and
changing/resetting filters. Verification on 2026-11-25 passed for feature-swap-impl
compilation, 69 targeted tests covering input filters, slippage and Exchanger A, and root ktlint.
This prepares the shared widget; applying Exchanger A limits to a live form is
still pending. The calculated opposite field must preserve the backend estimate,
which can use more fractional digits than allowed for user requests. Max rounding
and validation when changing a selected asset also remain part of form integration.
No device or live-service verification was performed. Changes remain uncommitted.

### Jira scope check — 2026-11-25

NW-2500 remains In Progress. Its full scope includes create/status transport,
general Token select/router integration, service-failure behavior and region
restrictions. Assets/quote transport and the release-1 catalogue are implemented.
Support for additional pairs in the general router and the full Unavailable group remain release 2;
therefore the full Jira task cannot be closed with the release-1 subset alone.
NW-2524 (access-check) and NW-2525 (Cloudflare) are still To Do in Jira. Input
constraints in the fourth increment belong to NW-2501, not completion of NW-2500.

### Fifth implementation increment — shared token selection (NW-2500)

Swaps passes the selected asset, opposite asset, edited side and wallet to the
existing token picker. SwapTokenSelectionCatalogue combines on-chain availability
with directed Exchanger A pairs. The picker applies wallet balances, sections,
network filters and search to the resulting list. When the optional service is
slow or unavailable, on-chain rows remain available. Selecting either side
rechecks the ordered pair before deciding whether to clear the opposite asset.

Exchanger A currently contributes only Asset Hub → native TAO pairs. Quoting
the selected pair on the native form, country access checks and live verification
remain pending.

Verification on 2026-11-25 passed for the debug app build, root ktlint, all
28 feature-assets unit tests and the 42 targeted Exchanger A unit tests.

### Superseded prototype — separate quote routing (NW-2500)

The previous prototype requested an on-chain quote first. The shared graph integration above replaces this approach. Only an explicit
NoRoute result triggers an Exchanger A catalogue check and, for an eligible
ordered pair, a provider quote. Pay and Receive requests preserve the wallet,
edited side and entered amount. The form fills the opposite amount field and reports provider limits. Provider service errors offer
manual retry. On-chain errors and insufficient liquidity keep their original
meaning. Provider quotes cannot enter on-chain fee estimation or confirmation.

Provider-specific input filters, Max with the deposit fee, access-check,
Review and order execution remain pending. Device and live-service verification
are separate from the debug build and JVM checks.
