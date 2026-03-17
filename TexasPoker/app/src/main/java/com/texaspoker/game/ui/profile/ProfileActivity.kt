package com.texaspoker.game.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.texaspoker.game.data.GameRepository
import com.texaspoker.game.databinding.ActivityProfileBinding
import com.texaspoker.game.ui.auth.AuthActivity

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GameRepository(this)
        loadProfile()
        setupButtons()
    }

    private fun loadProfile() {
        val user = repository.loadUser() ?: return
        val settings = repository.loadSettings()

        binding.tvUsername.text = user.username
        binding.tvLevel.text = "等级: Lv.${user.level}"
        binding.tvChips.text = "筹码: ${formatChips(user.chips)}"
        binding.tvTotalGames.text = "总场次: ${user.totalGames}"
        binding.tvWinRate.text = if (user.totalGames > 0) {
            "胜率: ${(user.totalWins * 100f / user.totalGames).toInt()}%"
        } else "胜率: --"
        binding.tvBestHand.text = "最佳手牌: ${user.bestHand.ifEmpty { "暂无" }}"

        binding.switchSound.isChecked = settings.soundEnabled
        binding.switchMusic.isChecked = settings.musicEnabled
        binding.switchVibration.isChecked = settings.vibrationEnabled

        // AI难度
        binding.spinnerDifficulty.setSelection(settings.aiDifficultyIndex)
    }

    private fun setupButtons() {
        binding.btnSaveSettings.setOnClickListener {
            val settings = GameRepository.GameSettings(
                soundEnabled = binding.switchSound.isChecked,
                musicEnabled = binding.switchMusic.isChecked,
                vibrationEnabled = binding.switchVibration.isChecked,
                aiDifficultyIndex = binding.spinnerDifficulty.selectedItemPosition
            )
            repository.saveSettings(settings)
            Toast.makeText(this, "设置已保存", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            repository.logout()
            val intent = Intent(this, AuthActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnClaimBonus.setOnClickListener {
            val bonus = repository.claimDailyBonus()
            if (bonus > 0) {
                Toast.makeText(this, "领取成功！+${formatChips(bonus)}", Toast.LENGTH_SHORT).show()
                loadProfile()
            } else {
                Toast.makeText(this, "今日已领取，明天再来", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatChips(amount: Long): String = when {
        amount >= 1_000_000L -> String.format("%.2fM", amount / 1_000_000.0)
        amount >= 1_000L -> String.format("%.1fK", amount / 1_000.0)
        else -> amount.toString()
    }
}
