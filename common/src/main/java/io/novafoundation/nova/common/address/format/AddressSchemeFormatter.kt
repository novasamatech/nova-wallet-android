package io.novafoundation.nova.common.address.format

import io.novafoundation.nova.common.R
import io.novafoundation.nova.common.di.scope.ApplicationScope
import io.novafoundation.nova.common.resources.ResourceManager
import javax.inject.Inject

interface AddressSchemeFormatter {

    fun addressLabel(addressScheme: AddressScheme): String

    fun accountsLabel(addressScheme: AddressScheme): String
}

@ApplicationScope
internal class RealAddressSchemeFormatter @Inject constructor(
    private val resourceManager: ResourceManager
) : AddressSchemeFormatter {

    override fun addressLabel(addressScheme: AddressScheme): String {
        return when (addressScheme) {
            AddressScheme.SUBSTRATE -> resourceManager.getString(R.string.common_substrate_address)
            AddressScheme.EVM -> resourceManager.getString(R.string.common_evm_address)
        }
    }

    override fun accountsLabel(addressScheme: AddressScheme): String {
        return when (addressScheme) {
            AddressScheme.SUBSTRATE -> resourceManager.getString(R.string.account_substrate_accounts)
            AddressScheme.EVM -> resourceManager.getString(R.string.account_evm_accounts)
        }
    }
}
