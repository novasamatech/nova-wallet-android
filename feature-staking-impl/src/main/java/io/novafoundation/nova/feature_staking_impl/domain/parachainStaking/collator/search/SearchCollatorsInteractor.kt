package io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.collator.search

import android.annotation.SuppressLint
import io.novafoundation.nova.feature_staking_impl.domain.parachainStaking.common.model.Collator

class SearchCollatorsInteractor {

    @SuppressLint("DefaultLocale")
    fun searchValidator(query: String, localValidators: Collection<Collator>): List<Collator> {
        val queryLower = query.toLowerCase()

        return localValidators.filter {
            val foundInIdentity = it.identity?.display?.toLowerCase()?.contains(queryLower) ?: false
            val foundInAddress = it.address.toLowerCase().startsWith(queryLower)

            foundInIdentity || foundInAddress
        }
    }
}
