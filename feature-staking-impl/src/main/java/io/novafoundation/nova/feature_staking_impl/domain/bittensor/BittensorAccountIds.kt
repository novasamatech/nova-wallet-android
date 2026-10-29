package io.novafoundation.nova.feature_staking_impl.domain.bittensor

import io.novasama.substrate_sdk_android.runtime.AccountId

/** Public account ID of the owner of funds and stake. Raw bytes, not a private coldkey or an SS58 address string. */
typealias BittensorColdkey = AccountId

/** Public account ID of a miner's or validator's operational identity. Raw bytes, not a private hotkey or an SS58 address string. */
typealias BittensorHotkey = AccountId

// These aliases document roles only; the compiler still treats both as the SDK's AccountId.
