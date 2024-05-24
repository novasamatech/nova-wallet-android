package io.novafoundation.nova.feature_account_impl.presentation.exporting

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.CallSuper
import io.novafoundation.nova.common.base.BaseFragment
import io.novafoundation.nova.feature_account_impl.presentation.exporting.json.ShareCompletedReceiver

abstract class ExportFragment<V : ExportViewModel> : BaseFragment<V>() {

    @CallSuper
    override fun subscribe(viewModel: V) {
        viewModel.exportEvent.observeEvent(::shareTextWithCallback)
    }

    private fun shareTextWithCallback(text: String) {
        val title = getString(io.novafoundation.nova.feature_account_impl.R.string.common_share)

        val intent = Intent(Intent.ACTION_SEND)
            .putExtra(Intent.EXTRA_TEXT, text)
            .setType("text/plain")

        val receiver = Intent(requireContext(), ShareCompletedReceiver::class.java)

        val pendingIntent = PendingIntent.getBroadcast(requireContext(), 0, receiver, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val chooser = Intent.createChooser(intent, title, pendingIntent.intentSender)

        startActivity(chooser)
    }
}
