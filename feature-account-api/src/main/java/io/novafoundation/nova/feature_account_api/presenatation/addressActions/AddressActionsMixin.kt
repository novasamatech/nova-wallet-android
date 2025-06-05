package io.novafoundation.nova.feature_account_api.presenatation.addressActions

import androidx.lifecycle.LiveData
import io.novafoundation.nova.common.address.AddressModel
import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.address.format.asAddress
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.feature_account_api.presenatation.actions.ExternalActions
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope

/**
 * Simplified version of [ExternalActions] that does not require [Chain] to show address info but relies on [AddressFormat] instead
 * Note that this mixin just allows to copy the address and does not work with Unified Address Format (which is tied to a particular chain) since it requires to have
 * chain in the context
 */
interface AddressActionsMixin {

    interface Factory {

        fun create(coroutineScope: CoroutineScope): Presentation
    }

    class Payload(
        val addressModel: AddressModel,
        val addressTypeLabel: ChipLabelModel,
    )

    val showAddressActionsEvent: LiveData<Event<Payload>>

    fun copyValue(payload: Payload)

    interface Presentation : AddressActionsMixin {

        fun showAddressActions(accountId: AccountId, addressFormat: AddressFormat)
    }
}

fun AddressActionsMixin.Presentation.showAddressActions(address: String, addressFormat: AddressFormat) {
    val accountId = addressFormat.accountIdOf(address.asAddress()).value
    return showAddressActions(accountId, addressFormat)
}
