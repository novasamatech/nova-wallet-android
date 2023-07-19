package io.novafoundation.nova.core_db.dao

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.novafoundation.nova.core_db.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokenDaoTest : DaoTest<TokenDao>(AppDatabase::tokenDao) {

    private val currencyDao by dao<CurrencyDao>()

    private val tokenSymbol = "$"

    @Test
    fun getTokenWhenCurrencySelected() = runBlocking {
        currencyDao.insert(createCurrency(tokenSymbol, true))

        val tokenWithCurrency = dao.getTokenWithCurrency(tokenSymbol)

        assert(tokenWithCurrency != null)
        assert(tokenWithCurrency?.token == null)
    }

    @Test
    fun getTokenWhenCurrencyNotSelected() = runBlocking {
        currencyDao.insert(createCurrency(tokenSymbol, false))

        val token = dao.getTokenWithCurrency(tokenSymbol)

        assert(token == null)
    }

    @Test
    fun getTokensWhenCurrencySelected() = runBlocking {
        currencyDao.insert(createCurrency(tokenSymbol, true))

        val tokensWithCurrencies = dao.getTokensWithCurrency(listOf(tokenSymbol))

        assert(tokensWithCurrencies.isNotEmpty())
    }

    @Test
    fun getTokensWhenCurrencyNotSelected() = runBlocking {
        currencyDao.insert(createCurrency(tokenSymbol, false))

        val tokensWithCurrencies = dao.getTokensWithCurrency(listOf(tokenSymbol))

        assert(tokensWithCurrencies.isEmpty())
    }

    @Test
    fun shouldInsertTokenWithDefaultCurrency() = runBlocking {
        currencyDao.insert(createCurrency(tokenSymbol, true))

        dao.insertTokenWithSelectedCurrency(tokenSymbol)
        val tokenWithCurrency = dao.getTokenWithCurrency(tokenSymbol)
        assert(tokenWithCurrency != null)
    }

    @Test
    fun shouldInsertTokenWithoutCurrency() = runBlocking {
        currencyDao.insert(createCurrency(tokenSymbol, false))

        dao.insertTokenWithSelectedCurrency(tokenSymbol)
        val tokenWithCurrency = dao.getTokenWithCurrency(tokenSymbol)
        assert(tokenWithCurrency == null)
    }
}
