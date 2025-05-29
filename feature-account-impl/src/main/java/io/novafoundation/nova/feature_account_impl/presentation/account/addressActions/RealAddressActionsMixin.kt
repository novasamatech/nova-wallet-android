package io.novafoundation.nova.feature_account_impl.presentation.account.addressActions

import androidx.lifecycle.MutableLiveData
import io.novafoundation.nova.common.address.AddressIconGenerator
import io.novafoundation.nova.common.address.format.AddressFormat
import io.novafoundation.nova.common.address.format.AddressScheme
import io.novafoundation.nova.common.address.format.AddressSchemeFormatter
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.utils.CopyValueMixin
import io.novafoundation.nova.common.utils.Event
import io.novafoundation.nova.common.utils.event
import io.novafoundation.nova.common.utils.launchUnit
import io.novafoundation.nova.common.view.ChipLabelModel
import io.novafoundation.nova.feature_account_api.presenatation.account.icon.createAccountAddressModel
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.AddressActionsMixin
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.AddressActionsMixin.Payload
import io.novafoundation.nova.feature_account_api.presenatation.addressActions.AddressActionsMixin.Presentation
import io.novasama.substrate_sdk_android.runtime.AccountId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

@FeatureScope
class AddressActionsMixinFactory @Inject constructor(
    private val copyValueMixin: CopyValueMixin,
    private val iconGenerator: AddressIconGenerator,
    private val addressSchemeFormatter: AddressSchemeFormatter
) : AddressActionsMixin.Factory {

    override fun create(coroutineScope: CoroutineScope): Presentation {
        return RealAddressActionsMixin(
            coroutineScope = coroutineScope,
            copyValueMixin = copyValueMixin,
            iconGenerator = iconGenerator,
            addressSchemeFormatter = addressSchemeFormatter
        )
    }
}

private class RealAddressActionsMixin(
    coroutineScope: CoroutineScope,
    private val copyValueMixin: CopyValueMixin,
    private val iconGenerator: AddressIconGenerator,
    private val addressSchemeFormatter: AddressSchemeFormatter
) : Presentation, CoroutineScope by coroutineScope {

    override val showAddressActionsEvent = MutableLiveData<Event<Payload>>()

    override fun showAddressActions(accountId: AccountId, addressFormat: AddressFormat) = launchUnit(Dispatchers.Default) {
        val addressModel = iconGenerator.createAccountAddressModel(addressFormat, accountId)
        val payload = Payload(addressModel, addressFormat.scheme.addressLabel())

        showAddressActionsEvent.postValue(payload.event())
    }

    override fun copyValue(payload: Payload) {
        copyValueMixin.copyValue(payload.addressModel.address)
    }

    private fun AddressScheme.addressLabel(): ChipLabelModel {
        val label = addressSchemeFormatter.addressLabel(this)
        return ChipLabelModel(label)
    }
}
