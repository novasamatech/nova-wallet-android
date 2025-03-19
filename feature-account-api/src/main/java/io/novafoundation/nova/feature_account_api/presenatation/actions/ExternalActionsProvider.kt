package io.novafoundation.nova.feature_account_api.presenatation.actions

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.common.utils.CopyValueMixin
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.feature_account_api.data.mappers.mapChainToUi
import io.novafoundation.nova.feature_account_api.domain.account.common.ChainWithAccountId
import io.novafoundation.nova.feature_account_api.presenatation.account.copyAddress.CopyAddressMixin
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createOptionalAccountAddressIcon
import io.novafoundation.nova.runtime.ext.accountIdOf
import io.novafoundation.nova.runtime.ext.accountUrlOf
import io.novafoundation.nova.runtime.ext.eventUrlOf
import io.novafoundation.nova.runtime.ext.extrinsicUrlOf
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain

class ExternalActionsProvider(
    private val resourceManager: ResourceManager,
    private val addressIconGenerator: AddressIconGenerator,
    private val copyAddressMixin: CopyAddressMixin,
    private val copyValueMixin: CopyValueMixin
) : ExternalActions.Presentation {

    override val openBrowserEvent = MutableLiveData<Event<String>>()

    override val showExternalActionsEvent = MutableLiveData<Event<ExternalActions.Payload>>()

    override fun viewExternalClicked(explorer: Chain.Explorer, type: ExternalActions.Type) {
        val url = when (type) {
            ExternalActions.Type.EmptyAccount -> null
            is ExternalActions.Type.Address -> type.address.let { explorer.accountUrlOf(it) }
            is ExternalActions.Type.Event -> explorer.eventUrlOf(type.id)
            is ExternalActions.Type.Extrinsic -> explorer.extrinsicUrlOf(type.hash)
        }

        url?.let { showBrowser(url) }
    }

    override fun showBrowser(url: String) {
        openBrowserEvent.value = Event(url)
    }

    override suspend fun showExternalActions(type: ExternalActions.Type, chain: Chain) {
        val copyLabelRes = when (type) {
            is ExternalActions.Type.Address -> R.string.common_copy_address
            is ExternalActions.Type.Event -> R.string.common_copy_id
            is ExternalActions.Type.Extrinsic -> R.string.transaction_details_copy_hash
            ExternalActions.Type.EmptyAccount -> null
        }

        // only show chain button for address as for now
        val chainUi = when (type) {
            is ExternalActions.Type.Address -> mapChainToUi(chain)
            else -> null
        }

        // only show icon for address as for now
        val icon = when (type) {
            is ExternalActions.Type.Address -> addressIconGenerator.createOptionalAccountAddressIcon(chain, type.address)
                ?: resourceManager.getDrawable(R.drawable.ic_identicon_placeholder)

            is ExternalActions.Type.EmptyAccount -> resourceManager.getDrawable(R.drawable.ic_identicon_placeholder)

            is ExternalActions.Type.Event,
            is ExternalActions.Type.Extrinsic -> null
        }

        val payload = ExternalActions.Payload(
            type = type,
            chain = chain,
            chainUi = chainUi,
            icon = icon,
            copyLabelRes = copyLabelRes
        )

        showExternalActionsEvent.value = Event(payload)
    }

    override fun copyValue(payload: ExternalActions.Payload) {
        when (val type = payload.type) {
            is ExternalActions.Type.Address -> {
                val accountId = payload.chain.accountIdOf(type.address)
                val chainWithAccountId = ChainWithAccountId(payload.chain, accountId)
                copyAddressMixin.copyAddressOrOpenSelector(chainWithAccountId)
            }

            is ExternalActions.Type.Event -> copyValueMixin.copyValue(type.id)
            is ExternalActions.Type.Extrinsic -> copyValueMixin.copyValue(type.hash)

            ExternalActions.Type.EmptyAccount -> {}
        }
    }
}
