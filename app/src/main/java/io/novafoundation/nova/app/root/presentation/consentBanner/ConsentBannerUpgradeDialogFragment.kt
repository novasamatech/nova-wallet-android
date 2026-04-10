package io.novafoundation.nova.app.root.presentation.consentBanner

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.text.style.UnderlineSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.CheckBox
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import io.novafoundation.nova.app.R
import io.novafoundation.nova.common.utils.clickableSpan
import io.novafoundation.nova.common.utils.colorSpan
import io.novafoundation.nova.common.utils.formatting.spannable.SpannableFormatter
import io.novafoundation.nova.common.utils.setFullSpan
import io.novafoundation.nova.common.utils.showBrowser
import io.novafoundation.nova.common.utils.toSpannable
import io.novafoundation.nova.common.data.preferences.ConsentBannerConstants
import io.novafoundation.nova.common.view.PrimaryButton

class ConsentBannerUpgradeDialogFragment : DialogFragment() {

    companion object {
        const val TAG = "ConsentBannerUpgradeDialogFragment"
    }

    /**
     * Set by RootActivity before showing the dialog. Invoked when the user
     * checks the box and taps Accept. RootActivity persists the acceptance
     * via RootViewModel.consentBannerUpgradeAccepted().
     */
    var onAccepted: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Block ALL dismissal paths — the consent must be acquired via the
        // explicit Accept button, never bypassed. This blocks back-press,
        // tap-outside, and any swipe gestures.
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.dialog_consent_banner_upgrade, container, false)
    }

    override fun onResume() {
        super.onResume()

        // Default Android dialog width is theme-dependent and typically much
        // narrower than the screen, which causes the consent text to wrap
        // awkwardly. Stretch the dialog window to ~92% of the screen width
        // so the consent paragraph reads naturally.
        val window = dialog?.window ?: return
        val widthPixels = (resources.displayMetrics.widthPixels * 0.92f).toInt()
        window.setLayout(widthPixels, WindowManager.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // isCancelable on the DialogFragment also blocks the back press.
        isCancelable = false

        val titleTextView = view.findViewById<TextView>(R.id.consentUpgradeTitle)
        val descriptionTextView = view.findViewById<TextView>(R.id.consentUpgradeDescription)
        val consentTextView = view.findViewById<TextView>(R.id.consentUpgradeText)
        val checkBox = view.findViewById<CheckBox>(R.id.consentUpgradeCheckbox)
        val acceptButton = view.findViewById<PrimaryButton>(R.id.consentUpgradeAccept)

        titleTextView.setText(io.novafoundation.nova.common.R.string.consent_banner_upgrade_title)
        descriptionTextView.setText(io.novafoundation.nova.common.R.string.consent_banner_upgrade_description)
        acceptButton.setText(getString(io.novafoundation.nova.common.R.string.consent_banner_upgrade_accept))
        acceptButton.isEnabled = false

        configureConsentText(consentTextView)

        checkBox.setOnCheckedChangeListener { _, isChecked ->
            acceptButton.isEnabled = isChecked
        }

        acceptButton.setOnClickListener {
            onAccepted?.invoke()
            dismiss()
        }
    }

    private fun configureConsentText(textView: TextView) {
        val sourceText = getString(io.novafoundation.nova.common.R.string.consent_banner_text_template)
        val termsText = getString(io.novafoundation.nova.common.R.string.consent_banner_terms_of_service)
        val privacyText = getString(io.novafoundation.nova.common.R.string.consent_banner_privacy_notice)
        val clickableColor = requireContext().getColor(io.novafoundation.nova.common.R.color.text_primary)

        textView.text = SpannableFormatter.format(
            sourceText,
            termsText.toSpannable(colorSpan(clickableColor))
                .setFullSpan(clickableSpan { requireContext().showBrowser(ConsentBannerConstants.TERMS_OF_SERVICE_URL) })
                .setFullSpan(UnderlineSpan()),
            privacyText.toSpannable(colorSpan(clickableColor))
                .setFullSpan(clickableSpan { requireContext().showBrowser(ConsentBannerConstants.PRIVACY_NOTICE_URL) })
                .setFullSpan(UnderlineSpan()),
        )
        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.highlightColor = Color.TRANSPARENT
    }
}
