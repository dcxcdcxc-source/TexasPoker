package com.texaspoker.game.model

/**
 * 玩家状态
 */
enum class PlayerStatus {
    WAITING,        // 等待中
    ACTIVE,         // 轮到行动
    FOLDED,         // 已弃牌
    ALL_IN,         // 全下
    SITTING_OUT,    // 暂离
    ELIMINATED      // 已淘汰（锦标赛）
}

/**
 * 玩家行动类型
 */
enum class PlayerAction {
    FOLD,       // 弃牌
    CHECK,      // 过牌
    CALL,       // 跟注
    RAISE,      // 加注
    ALL_IN,     // 全下
    BET         // 下注
}

/**
 * 玩家类型
 */
enum class PlayerType {
    HUMAN,      // 真人
    AI          // AI机器人
}

/**
 * 玩家数据
 */
data class Player(
    val id: String,
    val name: String,
    val avatar: Int = 0,           // 头像资源ID
    val type: PlayerType = PlayerType.HUMAN,
    var chips: Long = 10000L,
    var holeCards: List<Card> = emptyList(),
    var status: PlayerStatus = PlayerStatus.WAITING,
    var currentBet: Long = 0L,
    var totalBetThisHand: Long = 0L,
    var isDealer: Boolean = false,
    var isSmallBlind: Boolean = false,
    var isBigBlind: Boolean = false,
    var seatIndex: Int = 0,
    var winAmount: Long = 0L,
    var lastAction: PlayerAction? = null,
    val level: Int = 1,            // 玩家等级
    val experience: Long = 0L      // 经验值
) {
    val isActive: Boolean get() = status != PlayerStatus.FOLDED && status != PlayerStatus.ELIMINATED
    val canAct: Boolean get() = status == PlayerStatus.ACTIVE || status == PlayerStatus.WAITING
    val displayChips: String get() = formatChips(chips)

    private fun formatChips(amount: Long): String = when {
        amount >= 1_000_000L -> String.format("%.1fM", amount / 1_000_000.0)
        amount >= 1_000L -> String.format("%.1fK", amount / 1_000.0)
        else -> amount.toString()
    }
}

/**
 * 用户账户信息
 */
data class UserAccount(
    val userId: String,
    val username: String,
    val email: String = "",
    val avatarIndex: Int = 0,
    var chips: Long = 100_000L,
    var diamonds: Int = 0,         // 钻石（高级货币）
    val level: Int = 1,
    val experience: Long = 0L,
    val totalWins: Int = 0,
    val totalGames: Int = 0,
    val bestHand: String = "",
    val joinDate: Long = System.currentTimeMillis(),
    val vipLevel: Int = 0          // VIP等级
)
