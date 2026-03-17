package com.texaspoker.game.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.texaspoker.game.model.UserAccount

/**
 * 本地数据存储管理器（SharedPreferences + Gson）
 */
class GameRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("texas_poker_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val KEY_USER = "current_user"
        private const val KEY_SETTINGS = "game_settings"
        private const val KEY_CHIPS = "player_chips"
        private const val KEY_TOTAL_WINS = "total_wins"
        private const val KEY_TOTAL_GAMES = "total_games"
        private const val KEY_BEST_HAND = "best_hand"
        private const val DAILY_BONUS_KEY = "last_daily_bonus"
        private const val DAILY_BONUS_AMOUNT = 50_000L
    }

    /**
     * 保存用户信息
     */
    fun saveUser(user: UserAccount) {
        prefs.edit().putString(KEY_USER, gson.toJson(user)).apply()
    }

    /**
     * 加载用户信息
     */
    fun loadUser(): UserAccount? {
        val json = prefs.getString(KEY_USER, null) ?: return null
        return try {
            gson.fromJson(json, UserAccount::class.java)
        } catch (e: Exception) { null }
    }

    /**
     * 获取或创建默认用户
     */
    fun getOrCreateUser(username: String = "玩家"): UserAccount {
        return loadUser() ?: run {
            val user = UserAccount(
                userId = System.currentTimeMillis().toString(),
                username = username,
                chips = 100_000L
            )
            saveUser(user)
            user
        }
    }

    /**
     * 更新筹码
     */
    fun updateChips(userId: String, chips: Long) {
        val user = loadUser() ?: return
        if (user.userId == userId) {
            saveUser(user.copy(chips = chips))
        }
    }

    /**
     * 每日免费筹码奖励
     */
    fun claimDailyBonus(): Long {
        val lastClaim = prefs.getLong(DAILY_BONUS_KEY, 0L)
        val now = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L

        if (now - lastClaim < oneDayMs) return 0L

        prefs.edit().putLong(DAILY_BONUS_KEY, now).apply()

        val user = loadUser() ?: return DAILY_BONUS_AMOUNT
        saveUser(user.copy(chips = user.chips + DAILY_BONUS_AMOUNT))
        return DAILY_BONUS_AMOUNT
    }

    /**
     * 记录游戏结果
     */
    fun recordGameResult(won: Boolean, handName: String = "") {
        val user = loadUser() ?: return
        val updated = user.copy(
            totalGames = user.totalGames + 1,
            totalWins = user.totalWins + if (won) 1 else 0,
            bestHand = if (handName.isNotEmpty() && isBetterHand(handName, user.bestHand)) handName else user.bestHand
        )
        saveUser(updated)
    }

    private fun isBetterHand(newHand: String, currentBest: String): Boolean {
        val handRanks = listOf("高牌", "一对", "两对", "三条", "顺子", "同花", "葫芦", "四条", "同花顺", "皇家同花顺")
        val newIdx = handRanks.indexOfFirst { newHand.contains(it) }
        val curIdx = handRanks.indexOfFirst { currentBest.contains(it) }
        return newIdx > curIdx
    }

    /**
     * 游戏设置
     */
    data class GameSettings(
        val soundEnabled: Boolean = true,
        val musicEnabled: Boolean = true,
        val vibrationEnabled: Boolean = true,
        val showBigBlindAmount: Boolean = true,
        val autoMuckLosingHand: Boolean = true,
        val aiDifficultyIndex: Int = 1  // 0=新手,1=中级,2=高级,3=专家
    )

    fun saveSettings(settings: GameSettings) {
        prefs.edit().putString(KEY_SETTINGS, gson.toJson(settings)).apply()
    }

    fun loadSettings(): GameSettings {
        val json = prefs.getString(KEY_SETTINGS, null) ?: return GameSettings()
        return try {
            gson.fromJson(json, GameSettings::class.java)
        } catch (e: Exception) { GameSettings() }
    }

    fun isLoggedIn(): Boolean = loadUser() != null

    fun logout() {
        prefs.edit().remove(KEY_USER).apply()
    }
}
