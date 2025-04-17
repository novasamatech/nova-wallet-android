package io.novafoundation.nova.feature_governance_impl.presentation.common.share

import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.common.utils.shareText

fun BaseFragment<*, *>.setupReferendumSharing(mixin: ShareReferendumMixin) {
    mixin.shareEvent.observeEvent { referendumLink ->
        requireContext().shareText(referendumLink.toString())
    }
}
