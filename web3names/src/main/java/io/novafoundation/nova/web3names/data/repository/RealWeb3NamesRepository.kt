package io.novafoundation.nova.web3names.data.repository

import com.google.gson.Gson
import io.novafoundation.nova.caip.caip19.Caip19MatcherFactory
import io.novafoundation.nova.caip.caip19.Caip19Parser
import io.novafoundation.nova.caip.caip19.matchers.Caip19Matcher
import io.novafoundation.nova.common.data.network.runtime.binding.bindList
import io.novafoundation.nova.common.data.network.runtime.binding.bindString
import io.novafoundation.nova.common.data.network.runtime.binding.castToStruct
import io.novafoundation.nova.common.utils.fromJson
import io.novafoundation.nova.runtime.ext.isValidAddress
import io.novafoundation.nova.runtime.multiNetwork.chain.model.Chain
import io.novafoundation.nova.runtime.storage.source.StorageDataSource
import io.novafoundation.nova.web3names.data.endpoints.TransferRecipientsApi
import io.novafoundation.nova.web3names.data.endpoints.model.TransferRecipientRemote
import io.novafoundation.nova.web3names.data.integrity.Web3NamesIntegrityVerifier
import io.novafoundation.nova.web3names.data.provider.Web3NamesServiceChainIdProvider
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException.ChainProviderNotFoundException
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException.IntegrityCheckFailed
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException.UnsupportedAsset
import io.novafoundation.nova.web3names.domain.exceptions.Web3NamesException.ValidAccountNotFoundException
import io.novafoundation.nova.web3names.domain.models.Web3NameAccount
import jp.co.soramitsu.fearless_utils.runtime.AccountId
import jp.co.soramitsu.fearless_utils.runtime.metadata.module
import jp.co.soramitsu.fearless_utils.runtime.metadata.storage

private const val TRANSFER_ASSETS_PROVIDER_DID_SERVICE_TYPE = "KiltTransferAssetRecipientV1"

private typealias RecipientsByChain = Map<String, List<TransferRecipientRemote>>

class ServiceEndpoint(
    val id: String,
    val serviceTypes: List<String>,
    val urls: List<String>
)

class RealWeb3NamesRepository(
    private val remoteStorageSource: StorageDataSource,
    private val web3NamesServiceChainIdProvider: Web3NamesServiceChainIdProvider,
    private val transferRecipientApi: TransferRecipientsApi,
    private val caip19MatcherFactory: Caip19MatcherFactory,
    private val caip19Parser: Caip19Parser,
    private val web3NamesIntegrityVerifier: Web3NamesIntegrityVerifier,
    private val gson: Gson,
) : Web3NamesRepository {

    override suspend fun queryWeb3NameAccount(web3Name: String, chain: Chain, chainAsset: Chain.Asset): List<Web3NameAccount> {
        val caip19Matcher = caip19MatcherFactory.getCaip19Matcher(chain, chainAsset)
        if (caip19Matcher.isUnsupported()) throw UnsupportedAsset(web3Name, chainAsset)

        val owner = getWeb3NameAccountOwner(web3Name) ?: throw ChainProviderNotFoundException(web3Name)
        val serviceEndpoints = getDidServiceEndpoints(owner)
        val transferRecipientEndpoint = serviceEndpoints.firstTransferRecipientsEndpoint() ?: throw ValidAccountNotFoundException(web3Name, chain.name)

        val recipients = getRecipientsByChain(web3Name, transferRecipientEndpoint, chain)

        return findChainRecipients(recipients, web3Name, chain, caip19Matcher)
    }

    private suspend fun getWeb3NameAccountOwner(web3Name: String): AccountId? {
        return remoteStorageSource.query(web3NamesServiceChainIdProvider.getChainId()) {
            runtime.metadata
                .module("Web3Names")
                .storage("Owner")
                .query(web3Name.toByteArray(), binding = ::bindOwners)
        }
    }

    private suspend fun getDidServiceEndpoints(accountId: AccountId): List<ServiceEndpoint> {
        val serviceEndpoints = remoteStorageSource.query(web3NamesServiceChainIdProvider.getChainId()) {
            runtime.metadata.module("Did")
                .storage("ServiceEndpoints")
                .entries(
                    accountId,
                    keyExtractor = { it },
                    binding = { data, _ -> bindEndpoint(data) }
                )
        }

        return serviceEndpoints.values.toList()
    }

    private suspend fun getRecipientsByChain(
        w3nIdentifier: String,
        endpoint: ServiceEndpoint,
        chain: Chain
    ): RecipientsByChain {
        val url = endpoint.urls.firstOrNull() ?: throw ValidAccountNotFoundException(w3nIdentifier, chain.name)
        val recipientsContent = transferRecipientApi.getTransferRecipientsRaw(url)

        if (!web3NamesIntegrityVerifier.verifyIntegrity(serviceEndpointId = endpoint.id, serviceEndpointContent = recipientsContent)) {
            throw IntegrityCheckFailed(w3nIdentifier)
        }

        return parseRecipients(recipientsContent)
    }

    private fun parseRecipients(content: String): RecipientsByChain {
        return gson.fromJson(content)
    }

    private fun findChainRecipients(
        recipientsByChain: RecipientsByChain,
        w3nIdentifier: String,
        chain: Chain,
        caip19Matcher: Caip19Matcher
    ): List<Web3NameAccount> {
        val matchingRecipients = recipientsByChain.filterKeys {
            val caip19Identifier = caip19Parser.parseCaip19(it).getOrNull() ?: return@filterKeys false

            caip19Matcher.match(caip19Identifier)
        }

        if (matchingRecipients.isEmpty()) {
            throw ValidAccountNotFoundException(w3nIdentifier, chain.name)
        }

        val web3NameAccounts = matchingRecipients.flatMap { (_, chainRecipients) -> chainRecipients }
            .map {
                Web3NameAccount(
                    address = it.account,
                    description = it.description,
                    isValid = chain.isValidAddress(it.account)
                )
            }

        if (web3NameAccounts.none(Web3NameAccount::isValid)) {
            throw ValidAccountNotFoundException(w3nIdentifier, chain.name)
        }

        return web3NameAccounts
    }

    private fun bindOwners(data: Any?): AccountId? {
        if (data == null) return null

        val ownerStruct = data.castToStruct()
        return ownerStruct.get<AccountId>("owner")
    }

    private fun bindEndpoint(data: Any?): ServiceEndpoint {
        val endpointStruct = data.castToStruct()

        val serviceTypes = bindList(endpointStruct["serviceTypes"]) { bindString(it) }
        val urls = bindList(endpointStruct["urls"]) { bindString(it) }
        val id = bindString(endpointStruct["id"])

        return ServiceEndpoint(id, serviceTypes, urls)
    }

    private fun List<ServiceEndpoint>.firstTransferRecipientsEndpoint(): ServiceEndpoint? {
        return firstOrNull { TRANSFER_ASSETS_PROVIDER_DID_SERVICE_TYPE in it.serviceTypes }
    }
}
