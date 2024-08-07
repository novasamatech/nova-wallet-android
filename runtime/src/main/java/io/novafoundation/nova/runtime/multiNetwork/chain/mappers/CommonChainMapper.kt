package io.novafoundation.nova.runtime.multiNetwork.chain.mappers

import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain


fun mapSwapListToLocal(swap: List<Chain.Swap>) = swap.joinToString(separator = ",", transform = Chain.Swap::name)

fun mapGovernanceListToLocal(governance: List<Chain.Governance>) = governance.joinToString(separator = ",", transform = Chain.Governance::name)

fun mapCustomFeeToLocal(customFees: List<Chain.CustomFee>) = customFees.joinToString(separator = ",", transform = Chain.CustomFee::name)
