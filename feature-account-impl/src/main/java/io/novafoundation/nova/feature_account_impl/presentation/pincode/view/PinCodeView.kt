package io.novafoundation.nova.feature_account_impl.presentation.pincode.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.PincodeViewBinding

class PinCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : LinearLayout(context, attrs, defStyle) {

    var pinCodeEnteredListener: (String) -> Unit = {}
    var fingerprintClickListener: () -> Unit = {}

    private val pinCodeNumberClickListener = OnClickListener {
        pinNumberAdded((it as AppCompatButton).text.toString())
    }

    private val pinCodeDeleteClickListener = OnClickListener {
        deleteClicked()
    }

    private val pinCodeFingerprintClickListener = OnClickListener {
        fingerprintClickListener()
    }

    private var inputCode: String = ""

    private var progressView: DotsProgressView? = null

    private val binder = PincodeViewBinding.inflate(inflater(), this)

    init {
        orientation = VERTICAL

        binder.btn1.setOnClickListener(pinCodeNumberClickListener)
        binder.btn2.setOnClickListener(pinCodeNumberClickListener)
        binder.btn3.setOnClickListener(pinCodeNumberClickListener)
        binder.btn4.setOnClickListener(pinCodeNumberClickListener)
        binder.btn5.setOnClickListener(pinCodeNumberClickListener)
        binder.btn6.setOnClickListener(pinCodeNumberClickListener)
        binder.btn7.setOnClickListener(pinCodeNumberClickListener)
        binder.btn8.setOnClickListener(pinCodeNumberClickListener)
        binder.btn9.setOnClickListener(pinCodeNumberClickListener)
        binder.btn0.setOnClickListener(pinCodeNumberClickListener)

        binder.btnDelete.setOnClickListener(pinCodeDeleteClickListener)

        binder.biometricBtn.setOnClickListener(pinCodeFingerprintClickListener)

        updateProgress()
    }

    fun changeBimometricButtonVisibility(isVisible: Boolean) {
        binder.biometricBtn.setVisible(isVisible, falseState = View.INVISIBLE)
    }

    fun resetInput() {
        inputCode = ""
        updateProgress()
    }

    fun bindProgressView(view: DotsProgressView) {
        progressView = view

        updateProgress()
    }

    fun pinCodeMatchingError() {
        resetInput()
        shakeDotsAnimation()
    }

    private fun pinNumberAdded(number: String) {
        if (inputCode.length >= DotsProgressView.MAX_PROGRESS) {
            return
        } else {
            inputCode += number
            updateProgress()
        }
        if (inputCode.length == DotsProgressView.MAX_PROGRESS) {
            pinCodeEnteredListener(inputCode)
        }
    }

    private fun deleteClicked() {
        if (inputCode.isEmpty()) {
            return
        }
        inputCode = inputCode.substring(0, inputCode.length - 1)
        updateProgress()
    }

    private fun updateProgress() {
        val currentProgress = inputCode.length
        progressView?.setProgress(currentProgress)

        binder.btnDelete.isEnabled = currentProgress != 0
    }

    private fun shakeDotsAnimation() {
        val animation = AnimationUtils.loadAnimation(context, R.anim.shake)
        progressView?.startAnimation(animation)
    }
}
