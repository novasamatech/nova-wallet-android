# Infrastructure authentication

These are RC protocol constraints, not conventions inferred from master. Read for auth/attestation changes.

Nova infrastructure uses the explicitly selected profile-3 bearer transport. Profile-2 implementations and vectors are legacy compatibility code, not a fallback for profile-3 errors. Keep registration identity, key aliases and preferences separate between profiles and bind every bootstrap operation to one exact configured HTTPS origin.

Persist opaque profile-3 bearers encrypted with an Android Keystore AES key under no-backup storage before first use. Derive deadlines from issuance-start elapsed realtime and server lifetime, with a safety margin and boot identity; never revive a token using the calendar clock. Coalesce issuance in a supervised owner scope so one cancelled waiter cannot cancel other requests. Bootstrap uses its own dispatcher, without credentials/body logging, redirects or proof retries.

Renew a rejected bearer only on the contract's complete invalid-token signal, with at most one replay of a replayable business request. Compare token, scope and generation before invalidation; never delete a newer bearer after a late rejection. Honor rate/backoff cooldowns without resetting registration or switching protocols. Profile 3 does not bypass Play Integrity device/app/certificate checks.
