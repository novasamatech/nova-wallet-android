package io.novafoundation.nova.feature_multisig_operations.presentation.common

import io.novafoundation.nova.common.address.toHex
import io.novafoundation.nova.common.di.scope.FeatureScope
import io.novafoundation.nova.common.resources.ResourceManager
import io.novafoundation.nova.feature_account_api.data.multisig.model.PendingMultisigOperation
import io.novafoundation.nova.feature_multisig_operations.R
import javax.inject.Inject

interface MultisigOperationFormatter {

    fun formatTitle(operation: PendingMultisigOperation): String
}

@FeatureScope
class RealMultisigOperationFormatter @Inject constructor(
    private val resourceManager: ResourceManager,
) : MultisigOperationFormatter {

    override fun formatTitle(operation: PendingMultisigOperation): String {
        val call = operation.call

        return if (call != null) {
            "${call.module.name}.${call.function.name}"
        } else {
            val hexHash = operation.callHash.toHex()
            val trimmedHash = hexHash.substring(0, 4) + "..." + hexHash.substring(hexHash.length - 4, hexHash.length)

            resourceManager.getString(R.string.multisig_operations_unknown_calldata, trimmedHash)
        }
    }
}
