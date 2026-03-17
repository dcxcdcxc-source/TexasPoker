package com.texaspoker.game.ui.lobby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.texaspoker.game.databinding.FragmentLeaderboardBinding

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadLeaderboard()
    }

    private fun loadLeaderboard() {
        // 模拟排行榜数据
        val leaderData = listOf(
            Triple("🏆 德州之王", 12_580_000L, 1),
            Triple("💎 扑克高手", 9_340_000L, 2),
            Triple("🃏 神牌老司机", 7_820_000L, 3),
            Triple("♠ 暗夜猎手", 5_640_000L, 4),
            Triple("♥ 红心女王", 4_320_000L, 5),
            Triple("♦ 钻石玩家", 3_870_000L, 6),
            Triple("♣ 梅花战士", 2_950_000L, 7),
            Triple("你", loadUserChips(), 0)
        ).sortedByDescending { it.second }

        val sb = StringBuilder()
        leaderData.forEachIndexed { idx, (name, chips, _) ->
            val prefix = when (idx) {
                0 -> "🥇"
                1 -> "🥈"
                2 -> "🥉"
                else -> "${idx + 1}."
            }
            sb.appendLine("$prefix  $name")
            sb.appendLine("    筹码: ${formatChips(chips)}")
            sb.appendLine()
        }
        binding.tvLeaderboard.text = sb.toString()
    }

    private fun loadUserChips(): Long {
        val repo = com.texaspoker.game.data.GameRepository(requireContext())
        return repo.loadUser()?.chips ?: 0L
    }

    private fun formatChips(amount: Long): String = when {
        amount >= 1_000_000L -> String.format("%.2fM", amount / 1_000_000.0)
        amount >= 1_000L -> String.format("%.1fK", amount / 1_000.0)
        else -> amount.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
