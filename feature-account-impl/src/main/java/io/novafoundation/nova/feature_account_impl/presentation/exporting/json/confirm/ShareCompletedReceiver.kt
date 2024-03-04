package io.novafoundation.nova.feature_account_impl.presentation.exporting.json.confirm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.novafoundation.nova.common.di.FeatureUtils
import io.novafoundation.nova.feature_account_api.di.AccountFeatureApi
import io.novafoundation.nova.feature_account_impl.di.AccountFeatureComponent
import io.novafoundation.nova.feature_account_impl.presentation.AccountRouter
import javax.inject.Inject

class ShareCompletedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var router: AccountRouter

    override fun onReceive(context: Context, intent: Intent) {
        FeatureUtils.getFeature<AccountFeatureComponent>(context, AccountFeatureApi::class.java)
            .inject(this)

        router.finishExportFlow()
    }
}
