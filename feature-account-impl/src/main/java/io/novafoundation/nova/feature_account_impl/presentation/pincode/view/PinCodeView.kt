package io.novafoundation.nova.feature_account_impl.presentation.pincode.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.appcompat.widget.AppCompatButton
import io.novafoundation.nova.common.utils.setVisible
import io.novafoundation.nova.feature_account_impl.R
import kotlinx.android.synthetic.main.pincode_view.view.btn0
import kotlinx.android.synthetic.main.pincode_view.view.btn1
import kotlinx.android.synthetic.main.pincode_view.view.btn2
import kotlinx.android.synthetic.main.pincode_view.view.btn3
import kotlinx.android.synthetic.main.pincode_view.view.btn4
import kotlinx.android.synthetic.main.pincode_view.view.btn5
import kotlinx.android.synthetic.main.pincode_view.view.btn6
import kotlinx.android.synthetic.main.pincode_view.view.btn7
import kotlinx.android.synthetic.main.pincode_view.view.btn8
import kotlinx.android.synthetic.main.pincode_view.view.btn9
import kotlinx.android.synthetic.main.pincode_view.view.btnDelete
import kotlinx.android.synthetic.main.pincode_view.view.biometricBtn

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

    init {
        View.inflate(context, R.layout.pincode_view, this)

        orientation = VERTICAL

        btn1.setOnClickListener(pinCodeNumberClickListener)
        btn2.setOnClickListener(pinCodeNumberClickListener)
        btn3.setOnClickListener(pinCodeNumberClickListener)
        btn4.setOnClickListener(pinCodeNumberClickListener)
        btn5.setOnClickListener(pinCodeNumberClickListener)
        btn6.setOnClickListener(pinCodeNumberClickListener)
        btn7.setOnClickListener(pinCodeNumberClickListener)
        btn8.setOnClickListener(pinCodeNumberClickListener)
        btn9.setOnClickListener(pinCodeNumberClickListener)
        btn0.setOnClickListener(pinCodeNumberClickListener)

        btnDelete.setOnClickListener(pinCodeDeleteClickListener)

        biometricBtn.setOnClickListener(pinCodeFingerprintClickListener)

        updateProgress()
    }

    fun changeBimometricButtonVisibility(isVisible: Boolean) {
        biometricBtn.setVisible(isVisible, falseState = View.INVISIBLE)
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

        btnDelete.isEnabled = currentProgress != 0
    }

    private fun shakeDotsAnimation() {
        val animation = AnimationUtils.loadAnimation(context, R.anim.shake)
        progressView?.startAnimation(animation)
    }
}
