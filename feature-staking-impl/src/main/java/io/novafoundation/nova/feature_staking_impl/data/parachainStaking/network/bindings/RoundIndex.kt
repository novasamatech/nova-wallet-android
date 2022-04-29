package io.novafoundation.nova.feature_staking_impl.data.parachainStaking.network.bindings

import io.novafoundation.nova.common.data.network.runtime.binding.bindNumber
import io.novafoundation.nova.feature_staking_api.domain.model.parachain.RoundIndex

fun bindRoundIndex(instance: Any?): RoundIndex = bindNumber(instance)
