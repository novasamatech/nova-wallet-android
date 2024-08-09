package io.novafoundation.nova.feature_swap_impl.data.assetExchange.hydraDx.referrals

interface HydraDxNovaReferral {

    fun getNovaReferralCode(): String
}

class RealHydraDxNovaReferral : HydraDxNovaReferral {

    override fun getNovaReferralCode(): String {
        return "NOVA"
    }
}
