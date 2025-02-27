package io.novafoundation.nova.feature_governance_impl.presentation.common.share

import android.content.Intent
import io.novafoundation.nova.common.base.BaseFragment

fun BaseFragment<*>.setupReferendumSharing(mixin: ShareReferendumMixin) {
    mixin.shareEvent.observeEvent { referendumLink ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, referendumLink.toString())
        }

        startActivity(Intent.createChooser(intent, null))
    }
}
