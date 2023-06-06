package io.novafoundation.nova.feature_wallet_connect_impl.presentation.sessions.approve

import com.walletconnect.web3.wallet.client.Wallet.Model.SessionProposal
import io.novafoundation.nova.common.navigation.InterScreenRequester
import io.novafoundation.nova.common.navigation.InterScreenResponder

interface ApproveSessionRequester : InterScreenRequester<SessionProposal, Unit>

interface ApproveSessionResponder : InterScreenResponder<SessionProposal, Unit>

interface ApproveSessionCommunicator : ApproveSessionRequester, ApproveSessionResponder
