package com.texaspoker.game.ui.lobby

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.texaspoker.game.databinding.ItemTournamentBinding
import com.texaspoker.game.model.TournamentInfo
import com.texaspoker.game.model.TournamentStatus
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class TournamentAdapter(
    private val tournaments: List<TournamentInfo>,
    private val onJoin: (TournamentInfo) -> Unit
) : RecyclerView.Adapter<TournamentAdapter.TournamentVH>() {

    inner class TournamentVH(private val binding: ItemTournamentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(t: TournamentInfo) {
            binding.tvName.text = t.name
            binding.tvBuyIn.text = if (t.buyIn == 0L) "免费" else "买入: ${formatChips(t.buyIn)}"
            binding.tvPrizePool.text = "奖金池: ${formatChips(t.prizePool)}"
            binding.tvPlayers.text = "${t.currentPlayers}/${t.maxPlayers} 人"

            val now = System.currentTimeMillis()
            when (t.status) {
                TournamentStatus.REGISTERING -> {
                    val diff = t.startTime - now
                    binding.tvStatus.text = if (diff > 0) {
                        "报名中 | ${formatCountdown(diff)}后开始"
                    } else "即将开始"
                    binding.tvStatus.setTextColor(0xFF00CC44.toInt())
                    binding.btnJoin.text = "报名"
                    binding.btnJoin.isEnabled = true
                }
                TournamentStatus.RUNNING -> {
                    binding.tvStatus.text = "进行中"
                    binding.tvStatus.setTextColor(0xFFFF6600.toInt())
                    binding.btnJoin.text = "观战"
                    binding.btnJoin.isEnabled = true
                }
                TournamentStatus.FINISHED -> {
                    binding.tvStatus.text = "已结束"
                    binding.tvStatus.setTextColor(0xFF888888.toInt())
                    binding.btnJoin.text = "查看"
                    binding.btnJoin.isEnabled = false
                }
            }

            binding.btnJoin.setOnClickListener { onJoin(t) }
            binding.root.setOnClickListener { onJoin(t) }
        }

        private fun formatCountdown(ms: Long): String {
            val hours = TimeUnit.MILLISECONDS.toHours(ms)
            val minutes = TimeUnit.MILLISECONDS.toMinutes(ms) % 60
            return if (hours > 0) "${hours}h${minutes}m" else "${minutes}分钟"
        }

        private fun formatChips(amount: Long): String = when {
            amount >= 1_000_000L -> "${amount / 1_000_000}M"
            amount >= 1_000L -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TournamentVH {
        val binding = ItemTournamentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TournamentVH(binding)
    }

    override fun onBindViewHolder(holder: TournamentVH, position: Int) = holder.bind(tournaments[position])
    override fun getItemCount() = tournaments.size
}
