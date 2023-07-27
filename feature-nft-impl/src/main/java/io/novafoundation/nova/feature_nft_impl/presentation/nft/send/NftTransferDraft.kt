package io.novafoundation.nova.feature_nft_impl.presentation.nft.send

import android.os.Parcelable
import io.novafoundation.nova.feature_nft_api.data.model.Nft
import io.novafoundation.nova.runtime.multiNetwork.chain.model.ChainId
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal

@Parcelize
class NftTransferDraft(
    val originFee: BigDecimal,
    val nftId: String,
    val nftType: Nft.Type,
    val recipientAddress: String,
    val chainId: ChainId,
    val name: String,
    val issuance: String
) : Parcelable
