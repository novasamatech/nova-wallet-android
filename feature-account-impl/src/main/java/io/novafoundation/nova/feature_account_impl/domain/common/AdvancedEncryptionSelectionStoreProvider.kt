package io.novafoundation.nova.feature_account_impl.domain.common

import io.novafoundation.nova.common.data.memory.ComputationalCache
import io.novafoundation.nova.common.utils.selectionStore.ComputationalCacheSelectionStoreProvider

private const val KEY = "advanced_encryption_selection_store"

class AdvancedEncryptionSelectionStoreProvider(
    computationalCache: ComputationalCache
) : ComputationalCacheSelectionStoreProvider<AdvancedEncryptionSelectionStore>(computationalCache, KEY) {

    protected override fun initSelectionStore(): AdvancedEncryptionSelectionStore {
        return AdvancedEncryptionSelectionStore()
    }
}
