package io.novafoundation.nova.feature_governance_impl.data.offchain.v2.request

class ReferendumPreviewV2Request {
    val query = """
        query {
            posts(
                where: {onchain_link: {onchain_referendumv2_id: {_is_null: false}}}
            ) {
                title
                onchain_link {
                    onchain_referendumv2_id
                }
            }
        }
    """.trimIndent()
}
