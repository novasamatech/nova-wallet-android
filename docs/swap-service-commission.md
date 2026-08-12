# Nova service commission on swaps

> **Status.** The implementation lives on `feature/swap-nova-fee` (PR #2274). This document was merged
> separately, so the files it references exist only once that branch lands.

Nova charges **0.85 % on Hydration swaps**. AssetHub (AssetConversion) swaps and cross-chain transfers charge
nothing. The commission is a separate transfer batched with the swap, not a pool fee.

## Product rules

- **The commission is baked into the displayed rate, never shown as a line item.** The rate detail sheet says
  "The best rate found, including a 0.85 % Nova Wallet fee" — `SwapRateDescriptionMode.IncludesFee`, declared
  in `feature-swap-api/.../presentation/view/bottomSheet/description/DescriptionBottomSheetLauncherExt.kt`.
- **The amount the user enters is final, in both directions.**
  - `SPECIFIED_OUT` — entered *You receive* is what lands. The pool is asked for `entered + commission`.
  - `SPECIFIED_IN` — entered *You pay* is what leaves. The commission is taken out of the pool output.
- **Shown == charged.** The number on screen and the number in the extrinsic come from the same base.

## Two commission functions, and why both exist

`feature-swap-api/.../domain/model/NovaSwapCommission.kt`, rate `85 / 10000`:

| Function | Question it answers | Formula |
|---|---|---|
| `commissionToAddOnTop(net)` | "User must end up with `net` — how much extra to buy?" | `net × 85 / 10000` |
| `commissionIncludedIn(gross)` | "Pool produced `gross` — how much of it is ours?" | `gross × 85 / 10085` |

They are consistent: `commissionIncludedIn(net + commissionToAddOnTop(net)) == commissionToAddOnTop(net)`.
Using `commissionIncludedIn` on a grossed-up amount is what keeps quote-time and execution-time figures equal.

## Quote time — `RealSwapService.applyServiceFee`

The charging operation is found from the **prototypes**, not the raw path:

```
quotedTrade.path.constructAtomicOperationPrototypes().lastOrNull { it.chargesServiceFee }
```

`lastOrNull` matters: on a route like `xcm → hydra → hydra → xcm` the commission belongs to the **last
Hydration operation**, which is not the last route segment. `RealSwapService` stays source-agnostic —
`chargesServiceFee` / `serviceCommissionToAddOnTop` / `serviceCommissionIncludedIn` are declared on
`AtomicSwapOperationPrototype` (default: no fee) and overridden by Hydra only.

- `SPECIFIED_OUT` re-quotes the whole path for `entered + commissionToAddOnTop(entered)` so `amountIn` is
  accurate, then reports `amountOut = entered`.
- `SPECIFIED_IN` keeps the trade and reports `amountOut = gross − commissionIncludedIn(gross)`.

**Price impact is computed from `grossAmountOut`, not the net amount** (`ServiceFeeAdjustedTrade` carries
both). Price impact measures pool slippage; charging our commission into it would double-count the fee and
show a scary number on small swaps.

## Execution time — the net flow chain

Operations are built from the **gross** quoted path: every `estimatedSwapLimit` holds pre-commission amounts,
because the pool really does trade the gross amount and the commission is a separate transfer afterwards.
Therefore a segment cannot know, on its own, how much actually reaches it — the previous segment's output is
reduced by the commission, and that cut propagates down the chain.

`buildSwapSegments(fees, operations)` in `feature-swap-api/.../domain/model/SwapFee.kt` resolves this **once**
and stores the result on each `SwapFee.SwapSegment.netFlow`:

- `amountIn` — what actually enters (the previous segment's `amountOut`)
- `amountOut` — expected output, net of the commission → what the UI shows
- `amountOutMin` — worst-case output (slippage floor), reduced by the same proportion → what ED validations check

Everything downstream reads `segment.netFlow` and nothing recomputes the commission:

- `SwapRouteViewModel`, `SwapFeeViewModel`, `RealSwapService.swap()` → `operation.constructDisplayData(netFlow)`
- `SwapSufficientAmountOutToStayAboveEDValidation` (last segment), `SwapIntermediateReceivesMeetEDValidation`
  (all but last) → `netFlow.amountOutMin`

> **Do not "fix" `SwapLimit` to hold net amounts.** Limits drive execution; downstream limits are rebuilt at
> runtime from `SwapExecutionCorrection.actualReceivedAmount`, which is already net. `netFlow` is a derived
> value for display and validation only.

`constructDisplayData(netFlow)` is deliberately dumb — the operation supplies its assets and its shape
(`Swap` vs `Transfer`); all amounts come from the chain.

## On-chain shape

`HydraDxOperation` submits `batchAll(swap, commissionTransfer)`. The commission amount is
`commissionIncludedIn(estimatedSwapLimit.estimatedAmountOut)`, computed inside the operation, so `estimateFee`
and `submit` build identical calls (`appendSwapCalls(limit, chargesCommission)`) and the fee estimate includes
the transfer's weight.

The transfer call is not hardcoded — it comes from
`assetSourceRegistry.sourceFor(assetOut).transfers.constructTransferCall(...)`, so it depends on the asset out:

- native asset → `balances.transferKeepAlive` (`NativeAssetTransfers`)
- ORML asset → `currencies`/`tokens` `transfer` (`OrmlAssetTransfers`)

On the native path `transferKeepAlive` reverts the whole `batchAll` if the sender would drop below the
existential deposit — which is exactly why the ED validations must use `netFlow.amountOutMin`, not the gross
floor. The worst case is a small swap into a native asset the wallet does not hold yet.

## Traps found the hard way

- **Double gross-up in re-quotes.** `SharedQuoteValidationRetriever` must re-quote by the amount the *user
  entered* (`value.amountIn/amountOut`), never `quotedPath.quotedAmount` — for `SPECIFIED_OUT` the latter is
  already the grossed-up target, so re-quoting inflates `amountIn` and triggers a bogus
  "Swap rate was updated" slippage warning for any slippage below 0.85 %.
- **ED checks on downstream segments.** Before the net flow chain existed, only the charging segment
  subtracted its commission; segments *after* it validated against gross amounts that would never arrive.
- **Detecting "route involves a Hydra swap"** must go by edge type (`HydraDxQuotableEdge`), not chain id —
  chain-id matching also catches cross-chain transfer edges to/from Hydration, which are not swaps
  (`SwapQuoteNovaFee.involvesHydraSwap`).
- **History noise.** The commission transfer and the Hydration router account (`modlrouterex`) show up as
  separate transfers in indexed history; `RealHydrationSwapTransferFilterFactory` filters both so the user
  sees only the swap.

## File map

| File | Role |
|---|---|
| `feature-swap-api/.../domain/model/NovaSwapCommission.kt` | Rate, both math helpers, fee account id |
| `feature-swap-api/.../domain/model/AtomicSwapOperationPrototype.kt` | `chargesServiceFee` + quote-time math hooks |
| `feature-swap-api/.../domain/model/AtomicSwapOperation.kt` | `chargesServiceCommission`, `constructDisplayData(netFlow)` |
| `feature-swap-api/.../domain/model/SwapFee.kt` | `SegmentNetFlow`, `buildSwapSegments` |
| `feature-swap-impl/.../domain/swap/RealSwapService.kt` | `applyServiceFee`, price impact, segment assembly |
| `feature-swap-impl/.../domain/swap/SwapQuoteNovaFee.kt` | Hydra-in-route detection, rate disclaimer mode |
| `feature-swap-impl/.../assetExchange/hydraDx/HydraDxAssetExchange.kt` | `batchAll` + commission transfer |
| `feature-swap-impl/.../data/history/RealHydrationSwapTransferFilterFactory.kt` | Hides fee/router transfers |
