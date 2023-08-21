package io.novafoundation.nova.feature_staking_impl.presentation.nominationPools.common

import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.utils.images.asIcon
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_staking_impl.domain.nominationPools.model.PoolDisplay
import io.novafoundation.nova.runtime.ext.addressOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

interface PoolDisplayFormatter {

    suspend fun format(poolDisplay: PoolDisplay, chain: Chain): PoolDisplayModel

    fun formatTitle(poolDisplay: PoolDisplay, chain: Chain): String
}

class RealPoolDisplayFormatter(
    private val addressIconGenerator: AddressIconGenerator,
) : PoolDisplayFormatter {

    override suspend fun format(poolDisplay: PoolDisplay, chain: Chain): PoolDisplayModel {
        val title = poolDisplay.metadata?.title
        val poolAccount = addressIconGenerator.createAccountAddressModel(chain, poolDisplay.stashAccountId, title)

        return PoolDisplayModel(
            icon = poolDisplay.icon ?: poolAccount.image.asIcon(),
            title = poolAccount.nameOrAddress
        )
    }

    override fun formatTitle(poolDisplay: PoolDisplay, chain: Chain): String {
        return poolDisplay.metadata?.title ?: chain.addressOf(poolDisplay.stashAccountId)
    }
}
