package com.texaspoker.game.model

/**
 * 游戏阶段
 */
enum class GamePhase {
    WAITING,        // 等待玩家
    PRE_FLOP,       // 翻前
    FLOP,           // 翻牌
    TURN,           // 转牌
    RIVER,          // 河牌
    SHOWDOWN,       // 摊牌
    GAME_OVER       // 游戏结束
}

/**
 * 桌类型
 */
enum class TableType {
    CASH_GAME,      // 现金局
    TOURNAMENT,     // 锦标赛
    SIT_AND_GO      // 坐就走
}

/**
 * 游戏配置
 */
data class GameConfig(
    val tableId: String,
    val tableType: TableType = TableType.CASH_GAME,
    val tableName: String = "德州扑克",
    val smallBlind: Long = 25L,
    val bigBlind: Long = 50L,
    val maxPlayers: Int = 9,
    val minBuyIn: Long = 1000L,
    val maxBuyIn: Long = 10000L,
    val turnTimeLimit: Int = 30,   // 秒
    val aiCount: Int = 5           // AI玩家数量
)

/**
 * 底池信息
 */
data class Pot(
    val amount: Long = 0L,
    val eligiblePlayers: List<String> = emptyList()  // 有资格赢得此底池的玩家ID
)

/**
 * 游戏状态
 */
data class GameState(
    val config: GameConfig,
    val players: MutableList<Player> = mutableListOf(),
    val deck: MutableList<Card> = mutableListOf(),
    val communityCards: MutableList<Card> = mutableListOf(),
    val pots: MutableList<Pot> = mutableListOf(),
    var currentPhase: GamePhase = GamePhase.WAITING,
    var currentPlayerIndex: Int = 0,
    var dealerIndex: Int = 0,
    var currentBet: Long = 0L,
    var handNumber: Int = 0,
    var lastRaiseAmount: Long = 0L,
    var minRaise: Long = 0L
) {
    val totalPot: Long get() = pots.sumOf { it.amount }
    val activePlayers: List<Player> get() = players.filter { it.isActive }
    val currentPlayer: Player? get() = if (currentPlayerIndex < players.size) players[currentPlayerIndex] else null
}

/**
 * 房间信息（大厅显示用）
 */
data class RoomInfo(
    val tableId: String,
    val tableName: String,
    val tableType: TableType,
    val smallBlind: Long,
    val bigBlind: Long,
    val playerCount: Int,
    val maxPlayers: Int,
    val avgPot: Long = 0L,
    val handsPerHour: Int = 80
)

/**
 * 锦标赛信息
 */
data class TournamentInfo(
    val tournamentId: String,
    val name: String,
    val buyIn: Long,
    val startTime: Long,
    val currentPlayers: Int,
    val maxPlayers: Int,
    val prizePool: Long,
    val status: TournamentStatus = TournamentStatus.REGISTERING,
    val blindStructure: List<BlindLevel> = defaultBlindStructure()
)

enum class TournamentStatus {
    REGISTERING, RUNNING, FINISHED
}

data class BlindLevel(
    val level: Int,
    val smallBlind: Long,
    val bigBlind: Long,
    val duration: Int = 15  // 分钟
)

fun defaultBlindStructure(): List<BlindLevel> = listOf(
    BlindLevel(1, 25, 50),
    BlindLevel(2, 50, 100),
    BlindLevel(3, 75, 150),
    BlindLevel(4, 100, 200),
    BlindLevel(5, 150, 300),
    BlindLevel(6, 200, 400),
    BlindLevel(7, 300, 600),
    BlindLevel(8, 400, 800),
    BlindLevel(9, 600, 1200),
    BlindLevel(10, 800, 1600)
)
