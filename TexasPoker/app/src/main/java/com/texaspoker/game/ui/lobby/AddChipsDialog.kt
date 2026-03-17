package com.texaspoker.game.ui.lobby

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.texaspoker.game.data.GameRepository

class AddChipsDialog : DialogFragment() {

    var onChipsAdded: ((Long) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = arrayOf(
            "免费筹码 +50,000 (每日)",
            "筹码包 +200,000 💎",
            "大筹码包 +1,000,000 💎",
            "豪华筹码 +5,000,000 💎"
        )

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle("获取筹码")
            .setItems(items) { _, which ->
                val repo = GameRepository(requireContext())
                when (which) {
                    0 -> {
                        val bonus = repo.claimDailyBonus()
                        if (bonus > 0) {
                            Toast.makeText(context, "领取成功！+${bonus}", Toast.LENGTH_SHORT).show()
                            onChipsAdded?.invoke(bonus)
                        } else {
                            Toast.makeText(context, "今日已领取，明天再来", Toast.LENGTH_SHORT).show()
                        }
                    }
                    1 -> {
                        // 模拟购买
                        val user = repo.loadUser() ?: return@setItems
                        repo.updateChips(user.userId, user.chips + 200_000L)
                        onChipsAdded?.invoke(200_000L)
                        Toast.makeText(context, "+200,000 筹码", Toast.LENGTH_SHORT).show()
                    }
                    2 -> {
                        val user = repo.loadUser() ?: return@setItems
                        repo.updateChips(user.userId, user.chips + 1_000_000L)
                        onChipsAdded?.invoke(1_000_000L)
                        Toast.makeText(context, "+1,000,000 筹码", Toast.LENGTH_SHORT).show()
                    }
                    3 -> {
                        val user = repo.loadUser() ?: return@setItems
                        repo.updateChips(user.userId, user.chips + 5_000_000L)
                        onChipsAdded?.invoke(5_000_000L)
                        Toast.makeText(context, "+5,000,000 筹码", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("取消", null)
            .create()
    }
}
