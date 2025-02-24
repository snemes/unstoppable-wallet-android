package io.horizontalsystems.bankwallet.modules.transactionInfo

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromBottom
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentTransactionInfoBinding
import io.horizontalsystems.bankwallet.modules.info.TransactionDoubleSpendInfoFragment
import io.horizontalsystems.bankwallet.modules.info.TransactionLockTimeInfoFragment
import io.horizontalsystems.bankwallet.modules.transactionInfo.adapters.TransactionInfoAdapter
import io.horizontalsystems.bankwallet.modules.transactionInfo.options.TransactionSpeedUpCancelFragment
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsModule
import io.horizontalsystems.bankwallet.modules.transactions.TransactionsViewModel
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ButtonPrimaryYellow
import io.horizontalsystems.bankwallet.ui.helpers.LinkHelper
import io.horizontalsystems.bankwallet.ui.helpers.TextHelper
import io.horizontalsystems.core.findNavController
import io.horizontalsystems.core.helpers.DateHelper
import io.horizontalsystems.core.helpers.HudHelper
import java.util.*

class TransactionInfoFragment : BaseFragment() {

    private val viewModelTxs by navGraphViewModels<TransactionsViewModel>(R.id.mainFragment) { TransactionsModule.Factory() }

    private var _binding: FragmentTransactionInfoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionInfoBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerView.adapter = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuClose -> {
                    findNavController().popBackStack()
                    true
                }
                else -> false
            }
        }

        val viewItem = viewModelTxs.tmpItemToShow ?: run {
            findNavController().popBackStack()
            return
        }

        val viewModel by navGraphViewModels<TransactionInfoViewModel>(R.id.transactionInfoFragment) {
            TransactionInfoModule.Factory(viewItem)
        }

        val itemsAdapter =
            TransactionInfoAdapter(
                viewModel.viewItemsLiveData,
                viewLifecycleOwner,
                object : TransactionInfoAdapter.Listener {
                    override fun onAddressClick(address: String) {
                        copyText(address)
                    }

                    override fun onActionButtonClick(actionButton: TransactionInfoActionButton) {
                        viewModel.onActionButtonClick(actionButton)
                    }

                    override fun onUrlClick(url: String) {
                        context?.let { ctx ->
                            LinkHelper.openLinkInAppBrowser(ctx, url)
                        }
                    }

                    override fun onClickStatusInfo() {
                        findNavController().slideFromBottom(R.id.statusInfoDialog)
                    }

                    override fun onLockInfoClick(lockDate: Date) {
                        context?.let {
                            val lockTime = DateHelper.getFullDate(lockDate)
                            val params = TransactionLockTimeInfoFragment.prepareParams(lockTime)

                            findNavController().slideFromBottom(
                                R.id.transactionLockTimeInfoFragment,
                                params
                            )
                        }
                    }

                    override fun onDoubleSpendInfoClick(
                        transactionHash: String,
                        conflictingHash: String
                    ) {
                        val params = TransactionDoubleSpendInfoFragment.prepareParams(
                            transactionHash,
                            conflictingHash
                        )
                        findNavController().slideFromBottom(
                            R.id.transactionDoubleSpendInfoFragment,
                            params
                        )
                    }

                    override fun onOptionButtonClick(optionType: TransactionInfoOption.Type) {
                        viewModel.onOptionButtonClick(optionType)
                    }
                })

        binding.recyclerView.adapter = ConcatAdapter(itemsAdapter)

        viewModel.showShareLiveEvent.observe(viewLifecycleOwner) { value ->
            context?.startActivity(Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, value)
                type = "text/plain"
            })
        }

        viewModel.copyRawTransactionLiveEvent.observe(viewLifecycleOwner) { rawTransaction ->
            copyText(rawTransaction)
        }

        viewModel.openTransactionOptionsModule.observe(viewLifecycleOwner) { (optionType, txHash) ->
            val params = TransactionSpeedUpCancelFragment.prepareParams(optionType, txHash)
            findNavController().slideFromRight(
                R.id.transactionSpeedUpCancelFragment,
                params
            )
        }

        binding.buttonCloseCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )

        binding.buttonCloseCompose.setContent {
            ComposeAppTheme {
                ButtonPrimaryYellow(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    title = getString(R.string.Button_Close),
                    onClick = {
                        findNavController().popBackStack()
                    }
                )
            }
        }
    }

    private fun copyText(address: String) {
        TextHelper.copyText(address)
        HudHelper.showSuccessMessage(requireView(), R.string.Hud_Text_Copied)
    }

}
