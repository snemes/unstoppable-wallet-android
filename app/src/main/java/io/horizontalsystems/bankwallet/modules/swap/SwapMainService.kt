package io.horizontalsystems.bankwallet.modules.swap

import io.horizontalsystems.bankwallet.core.ILocalStorage
import io.horizontalsystems.bankwallet.entities.EvmBlockchain
import io.horizontalsystems.marketkit.models.CoinType
import io.horizontalsystems.marketkit.models.PlatformCoin
import io.reactivex.subjects.PublishSubject

class SwapMainService(
    coinFrom: PlatformCoin?,
    private val providers: List<SwapMainModule.ISwapProvider>,
    private val localStorage: ILocalStorage
) {
    var dex: SwapMainModule.Dex = getDex(coinFrom)
        private set

    val currentProvider: SwapMainModule.ISwapProvider
        get() = dex.provider
    val providerObservable = PublishSubject.create<SwapMainModule.ISwapProvider>()

    var providerState = SwapMainModule.SwapProviderState(coinFrom = coinFrom)

    val availableProviders: List<SwapMainModule.ISwapProvider>
        get() = providers.filter { it.supports(dex.blockchain) }

    val blockchainTitle: String
        get() = dex.blockchain.name

    fun setProvider(provider: SwapMainModule.ISwapProvider) {
        if (dex.provider.id != provider.id) {
            dex = SwapMainModule.Dex(dex.blockchain, provider)
            providerObservable.onNext(provider)

            localStorage.setSwapProviderId(dex.blockchain, provider.id)
        }
    }

    private fun getDex(coinFrom: PlatformCoin?): SwapMainModule.Dex {
        val blockchain = getBlockchainForCoin(coinFrom)
        val provider = getSwapProvider(blockchain)
            ?: throw IllegalStateException("No provider found for ${blockchain.name}")

        return SwapMainModule.Dex(blockchain, provider)
    }

    private fun getSwapProvider(blockchain: EvmBlockchain): SwapMainModule.ISwapProvider? {
        val providerId = localStorage.getSwapProviderId(blockchain)
            ?: SwapMainModule.OneInchProvider.id

        return providers.firstOrNull { it.id == providerId }
    }

    private fun getBlockchainForCoin(coin: PlatformCoin?) =
        when (coin?.coinType) {
            CoinType.Ethereum, is CoinType.Erc20, null -> EvmBlockchain.Ethereum
            CoinType.BinanceSmartChain, is CoinType.Bep20 -> EvmBlockchain.BinanceSmartChain
            CoinType.Polygon, is CoinType.Mrc20 -> EvmBlockchain.Polygon
            CoinType.EthereumOptimism, is CoinType.OptimismErc20 -> EvmBlockchain.Optimism
            CoinType.EthereumArbitrumOne, is CoinType.ArbitrumOneErc20 -> EvmBlockchain.ArbitrumOne
            else -> throw IllegalStateException("Swap not supported for ${coin.coinType}")
        }

}
