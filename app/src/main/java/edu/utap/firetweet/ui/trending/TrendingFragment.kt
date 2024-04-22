package edu.utap.firetweet.ui.trending

import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import edu.utap.firetweet.databinding.FragmentTrendingBinding

class TrendingFragment : Fragment() {

    private var _binding: FragmentTrendingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?): View {
        val trendingViewModel =
            ViewModelProvider(this).get(TrendingViewModel::class.java)
        _binding = FragmentTrendingBinding.inflate(inflater, container, false)
        val root: View = binding.root
        trendingViewModel.loadHashtags()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val trendingViewModel = ViewModelProvider(this).get(TrendingViewModel::class.java)
        trendingViewModel.hashtags.observe(viewLifecycleOwner) { hashtags ->
            val spannableBuilder = SpannableStringBuilder()
            hashtags.forEach { hashtag ->
                if (spannableBuilder.isNotEmpty()) {
                    spannableBuilder.append("\n")
                }
                val spannable = SpannableString("#${hashtag.first} (${hashtag.second})")
                spannable.setSpan(
                    ForegroundColorSpan(Color.parseColor("#FFA500")),
                    0,
                    hashtag.first.length + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                spannableBuilder.append(spannable)
            }
            binding.textTrending.text = spannableBuilder
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
