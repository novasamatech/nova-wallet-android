package io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption

import android.content.Context
import android.os.Bundle
import androidx.recyclerview.widget.DiffUtil
import io.novafoundation.nova.common.utils.inflater
import io.novafoundation.nova.common.utils.makeInvisible
import io.novafoundation.nova.common.utils.makeVisible
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.ClickHandler
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListBottomSheet
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.DynamicListSheetAdapter
import io.novafoundation.nova.common.view.bottomSheet.list.dynamic.HolderCreator
import io.novafoundation.nova.feature_account_impl.R
import io.novafoundation.nova.feature_account_impl.databinding.ItemEncryptionTypeBinding
import io.novafoundation.nova.feature_account_impl.presentation.view.advanced.encryption.model.CryptoTypeModel

class EncryptionTypeChooserBottomSheetDialog(
    context: Context,
    payload: Payload<CryptoTypeModel>,
    onClicked: ClickHandler<CryptoTypeModel>
) : DynamicListBottomSheet<CryptoTypeModel>(context, payload, CryptoModelCallback, onClicked) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setTitle(R.string.common_crypto_type)
    }

    override fun holderCreator(): HolderCreator<CryptoTypeModel> = {
        EncryptionTypeViewHolder(ItemEncryptionTypeBinding.inflate(it.inflater(), it, false))
    }
}

class EncryptionTypeViewHolder(
    private val binder: ItemEncryptionTypeBinding
) : DynamicListSheetAdapter.Holder<CryptoTypeModel>(binder.root) {

    override fun bind(item: CryptoTypeModel, isSelected: Boolean, handler: DynamicListSheetAdapter.Handler<CryptoTypeModel>) {
        super.bind(item, isSelected, handler)

        with(binder) {
            if (isSelected) {
                rightIcon.makeVisible()
            } else {
                rightIcon.makeInvisible()
            }

            encryptionTv.text = item.name
        }
    }
}

private object CryptoModelCallback : DiffUtil.ItemCallback<CryptoTypeModel>() {
    override fun areItemsTheSame(oldItem: CryptoTypeModel, newItem: CryptoTypeModel): Boolean {
        return oldItem.cryptoType == newItem.cryptoType
    }

    override fun areContentsTheSame(oldItem: CryptoTypeModel, newItem: CryptoTypeModel): Boolean {
        return oldItem == newItem
    }
}
