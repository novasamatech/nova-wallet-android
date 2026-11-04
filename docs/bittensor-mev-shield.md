# Bittensor MEV protection

Behavior reference: [iOS PR 1890](https://github.com/novasamatech/nova-wallet-ios/pull/1890),
[order walkthrough](https://claude.ai/artifact/F4aGgfwd6NpACfgKKC5RBj).

## Scope and submission

- Subnet buys and sales, including grouped sales, from secrets wallets.
- Root operations, claims and external/nested signers use the existing path.
- Each review starts with protection requested; the preference is not persisted.
- Unlike the iOS reference, protection can be switched off and on while review data or fees are loading. A selection change reloads the fee; confirmation waits for the matching estimate. Validation and submission freeze the selection.
- Subnet buy/sale reviews show protection in a separate rounded card after order details (and grouped validators), before Wallet / Account, matching iOS.
- A shield-only quote failure retries three times and then disables protection for that review.
- Missing pallet/key, oversized payload or insufficient native funds makes protection unavailable.
- Review and validation use the selected fee. Setup and Max also reserve the carrier fee when available.
- Readiness is rechecked before authorization. Failure after selecting protection never broadcasts a clear order.

The inner order and the carrier share one finalized anchor and an eight-block mortal era.
The inner uses nonce N+1; the top-level `MevShield.submit_encrypted` carrier uses N. Both tips are zero.
A secrets signer authorizes at most two signatures with one PIN request, before fetching fresh submission timing.
The key is read again after the inner has been signed. A finality lag above five blocks prevents submission.

## Wire format

ML-KEM-768 (Bouncy Castle 1.82) establishes the XChaCha20-Poly1305 key (Tink Android 1.8.0).
The complete signed inner, including its compact length prefix, is encrypted.

`twox128(NextKey) | u16le(1088) | KEM ciphertext | nonce(24) | encrypted inner | tag(16)`

The overhead is 1146 bytes; the payload cap is 8192 bytes. The SDK vector copied from the iOS PR
includes its upstream source revision and is checked byte for byte.

## Outcome safety

The carrier watch is collected, but does not itself prove order execution. The tracker inspects raw extrinsic
hashes in finalized canonical blocks B through B+7 and resolves the inner's dispatch events using the existing
Bittensor resolver. It never treats a failed lookup or undecodable inner as absence.

A complete window without an inner proves non-execution. Carrier inclusion determines whether its fee was
charged; unresolved carrier fees remain estimates. If the window cannot be completed, the outcome stays
unconfirmed and offers no retry. At the confirmation cap the screen can be closed; a late finalized result
still replaces uncertainty while that screen remains alive. No durable journal or wallet-wide lock is added.

## Verification

Tests cover SDK sealing vectors, payload bounds, nonce/timing/signing order, post-signing key rotation,
readiness failure, cancelled PIN, unsupported wallets, fee/Max boundaries, quote fallback, carrier-only inclusion,
finalized absence, failed lookups and late outcomes. The normal Bittensor submission regression tests remain applicable.

Local JVM/static checks and APK assembly do not verify device UI, process restoration or live-chain execution.
No live transaction is required or submitted as part of this implementation. Signing and cryptography changes
still need human review before release.
