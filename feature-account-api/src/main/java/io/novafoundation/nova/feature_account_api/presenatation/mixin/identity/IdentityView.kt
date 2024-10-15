package io.novafoundation.nova.feature_account_api.presenatation.mixin.identity

import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.letOrHide
import io.novafoundation.nova.common.view.TableView
import io.novafoundation.nova.common.view.showValueOrHide
import io.novafoundation.nova.feature_account_api.R
import io.novafoundation.nova.feature_account_api.databinding.ViewIdentityBinding

class IdentityView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TableView(context, attrs, defStyleAttr) {

    private val binder = ViewIdentityBinding.inflate(inflater(), this)

    init {
        View.inflate(context, R.layout.view_identity, this)

        setTitle(context.getString(R.string.identity_title))
    }

    fun setModel(identityModel: IdentityModel) = with(identityModel) {
        binder.viewIdentityLegalName.showValueOrHide(legal)
        binder.viewIdentityEmail.showValueOrHide(email)
        binder.viewIdentityTwitter.showValueOrHide(twitter)
        binder.viewIdentityElementName.showValueOrHide(matrix)
        binder.viewIdentityWeb.showValueOrHide(web)
    }

    fun onEmailClicked(onClick: () -> Unit) {
        binder.viewIdentityEmail.setOnClickListener(onClick)
    }

    fun onWebClicked(onClick: () -> Unit) {
        binder.viewIdentityWeb.setOnClickListener(onClick)
    }

    fun onTwitterClicked(onClick: () -> Unit) {
        binder.viewIdentityTwitter.setOnClickListener(onClick)
    }

    private fun View.setOnClickListener(onClick: () -> Unit) {
        setOnClickListener { onClick() }
    }
}

fun IdentityView.setModelOrHide(identityModel: IdentityModel?) = letOrHide(identityModel, ::setModel)
