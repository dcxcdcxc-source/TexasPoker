package com.texaspoker.game.ui.lobby

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.texaspoker.game.R
import com.texaspoker.game.data.GameRepository
import com.texaspoker.game.databinding.ActivityMainBinding
import com.texaspoker.game.model.UserAccount
import com.texaspoker.game.ui.profile.ProfileActivity

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    lateinit var repository: GameRepository
    var currentUser: UserAccount? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GameRepository(this)
        currentUser = repository.loadUser()

        setupBottomNav()
        setupHeader()
        checkDailyBonus()

        // 默认显示现金局大厅
        if (savedInstanceState == null) {
            showFragment(CashGameFragment())
        }
    }

    private fun setupHeader() {
        currentUser?.let { user ->
            binding.tvUsername.text = user.username
            binding.tvChips.text = formatChips(user.chips)
            binding.tvLevel.text = "Lv.${user.level}"
        }

        binding.btnAddChips.setOnClickListener {
            showAddChipsDialog()
        }

        binding.btnProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_cash -> {
                    showFragment(CashGameFragment())
                    true
                }
                R.id.nav_tournament -> {
                    showFragment(TournamentListFragment())
                    true
                }
                R.id.nav_sit_and_go -> {
                    showFragment(SitAndGoFragment())
                    true
                }
                R.id.nav_leaderboard -> {
                    showFragment(LeaderboardFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun checkDailyBonus() {
        val bonus = repository.claimDailyBonus()
        if (bonus > 0) {
            Toast.makeText(this, "每日奖励！获得 ${formatChips(bonus)} 筹码", Toast.LENGTH_LONG).show()
            refreshChips()
        }
    }

    private fun showAddChipsDialog() {
        val dialog = AddChipsDialog()
        dialog.onChipsAdded = { amount ->
            currentUser = repository.loadUser()
            refreshChips()
        }
        dialog.show(supportFragmentManager, "add_chips")
    }

    fun refreshChips() {
        currentUser = repository.loadUser()
        binding.tvChips.text = formatChips(currentUser?.chips ?: 0)
    }

    override fun onResume() {
        super.onResume()
        refreshChips()
    }

    private fun formatChips(amount: Long): String = when {
        amount >= 1_000_000L -> String.format("%.1fM", amount / 1_000_000.0)
        amount >= 1_000L -> String.format("%.1fK", amount / 1_000.0)
        else -> amount.toString()
    }
}
