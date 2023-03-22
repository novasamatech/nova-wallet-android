package io.novafoundation.nova.feature_governance_impl.data.offchain.referendum.polkassembly.v1.request

class ReferendumPreviewRequest {
    val query = """
        query {
            posts(
                where: {type: {id: {_eq: 2}}, onchain_link: {onchain_referendum_id: {_is_null: false}}}
            ) {
                title
                onchain_link {
                    onchain_referendum_id
                }
            }
        }
    """.trimIndent()
}
