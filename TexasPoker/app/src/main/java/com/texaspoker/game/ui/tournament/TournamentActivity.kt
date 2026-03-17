package com.texaspoker.game.ui.tournament

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.texaspoker.game.databinding.ActivityTournamentBinding
import com.texaspoker.game.model.defaultBlindStructure

class TournamentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTournamentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTournamentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra("tournament_name") ?: "锦标赛"
        val buyIn = intent.getLongExtra("buy_in", 0L)
        val prizePool = intent.getLongExtra("prize_pool", 0L)

        binding.tvTournamentName.text = name
        binding.tvBuyIn.text = if (buyIn == 0L) "免费" else "买入: $buyIn"
        binding.tvPrizePool.text = "奖金池: ${formatChips(prizePool)}"

        // 显示盲注结构
        val structure = defaultBlindStructure()
        val sb = StringBuilder("盲注结构:\n\n")
        structure.forEach { level ->
            sb.appendLine("Level ${level.level}: ${level.smallBlind}/${level.bigBlind}  (${level.duration}分钟)")
        }
        binding.tvBlindStructure.text = sb.toString()

        binding.btnStartTournament.setOnClickListener {
            Toast.makeText(this, "锦标赛即将开始...", Toast.LENGTH_SHORT).show()
            // 跳转到游戏桌
            val intent = android.content.Intent(this, com.texaspoker.game.ui.table.GameTableActivity::class.java).apply {
                putExtra("table_id", "tournament_${System.currentTimeMillis()}")
                putExtra("table_name", name)
                putExtra("small_blind", 25L)
                putExtra("big_blind", 50L)
                putExtra("table_type", "TOURNAMENT")
                putExtra("max_players", 9)
            }
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun formatChips(amount: Long): String = when {
        amount >= 1_000_000L -> String.format("%.1fM", amount / 1_000_000.0)
        amount >= 1_000L -> String.format("%.1fK", amount / 1_000.0)
        else -> amount.toString()
    }
}
