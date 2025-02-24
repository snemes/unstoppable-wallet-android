package io.horizontalsystems.bankwallet.modules.settings.guides

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.ConcatAdapter
import io.horizontalsystems.bankwallet.R
import io.horizontalsystems.bankwallet.core.BaseFragment
import io.horizontalsystems.bankwallet.core.slideFromRight
import io.horizontalsystems.bankwallet.databinding.FragmentGuidesBinding
import io.horizontalsystems.bankwallet.entities.Guide
import io.horizontalsystems.bankwallet.entities.GuideCategory
import io.horizontalsystems.bankwallet.modules.markdown.MarkdownFragment
import io.horizontalsystems.bankwallet.ui.compose.ComposeAppTheme
import io.horizontalsystems.bankwallet.ui.compose.components.ScrollableTabs
import io.horizontalsystems.bankwallet.ui.compose.components.TabItem
import io.horizontalsystems.core.findNavController

class GuidesFragment : BaseFragment(), GuidesAdapter.Listener {

    private val viewModel by viewModels<GuidesViewModel> { GuidesModule.Factory() }
    private val errorAdapter = ErrorAdapter()
    private val guidesAdapter = GuidesAdapter(this)

    private var _binding: FragmentGuidesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGuidesBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.recyclerGuides.adapter = null
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().popBackStack()
        }

        binding.recyclerGuides.adapter = ConcatAdapter(errorAdapter, guidesAdapter)

        observeLiveData()

        binding.tabsCompose.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnLifecycleDestroyed(viewLifecycleOwner)
        )
    }

    override fun onItemClick(guide: Guide) {
        val arguments = bundleOf(
            MarkdownFragment.markdownUrlKey to guide.fileUrl,
            MarkdownFragment.handleRelativeUrlKey to true
        )
        findNavController().slideFromRight(
            R.id.markdownFragment,
            arguments
        )
    }

    private fun observeLiveData() {
        viewModel.guides.observe(viewLifecycleOwner, Observer {
            guidesAdapter.items = it
            guidesAdapter.notifyDataSetChanged()
        })

        viewModel.selectedCategory.observe(viewLifecycleOwner, Observer { selectedCategory ->
            setTabs(selectedCategory)
        })

        viewModel.loading.observe(viewLifecycleOwner, Observer {
            binding.toolbarSpinner.isVisible = it
        })

        viewModel.error.observe(viewLifecycleOwner, Observer {
            errorAdapter.error = it
        })
    }

    private fun setTabs(selectedCategory: GuideCategory) {
        val tabItems = viewModel.categories.map { TabItem(it.category, it == selectedCategory, it) }
        binding.tabsCompose.setContent {
            ComposeAppTheme {
                ScrollableTabs(tabItems) { tab -> viewModel.onSelectCategory(tab) }
            }
        }
    }

}
