package com.texaspoker.game.ai

import com.texaspoker.game.engine.HandEvaluator
import com.texaspoker.game.model.*
import kotlin.random.Random

/**
 * AI难度级别
 */
enum class AIDifficulty {
    BEGINNER,       // 新手 - 随机行动
    INTERMEDIATE,   // 中级 - 基于手牌强度
    ADVANCED,       // 高级 - 考虑位置和筹码
    EXPERT          // 专家 - 完整策略
}

/**
 * AI玩家决策引擎
 */
class AIDecisionEngine(private val difficulty: AIDifficulty = AIDifficulty.INTERMEDIATE) {

    data class AIDecision(
        val action: PlayerAction,
        val amount: Long = 0L,
        val reasoning: String = ""
    )

    /**
     * AI决策主入口
     */
    fun makeDecision(
        aiPlayer: Player,
        gameState: GameState,
        communityCards: List<Card>
    ): AIDecision {
        return when (difficulty) {
            AIDifficulty.BEGINNER -> beginnerStrategy(aiPlayer, gameState)
            AIDifficulty.INTERMEDIATE -> intermediateStrategy(aiPlayer, gameState, communityCards)
            AIDifficulty.ADVANCED -> advancedStrategy(aiPlayer, gameState, communityCards)
            AIDifficulty.EXPERT -> expertStrategy(aiPlayer, gameState, communityCards)
        }
    }

    /**
     * 新手策略 - 随机但偏向跟注
     */
    private fun beginnerStrategy(player: Player, state: GameState): AIDecision {
        val rand = Random.nextFloat()
        val canCheck = player.currentBet >= state.currentBet

        return when {
            rand < 0.3f -> AIDecision(PlayerAction.FOLD, reasoning = "新手弃牌")
            rand < 0.7f -> if (canCheck) {
                AIDecision(PlayerAction.CHECK, reasoning = "新手过牌")
            } else {
                AIDecision(PlayerAction.CALL, reasoning = "新手跟注")
            }
            else -> {
                val raiseAmount = state.currentBet + state.config.bigBlind * 2
                if (raiseAmount < player.chips + player.currentBet) {
                    AIDecision(PlayerAction.RAISE, raiseAmount, "新手加注")
                } else {
                    AIDecision(PlayerAction.CALL, reasoning = "新手跟注")
                }
            }
        }
    }

    /**
     * 中级策略 - 基于手牌强度
     */
    private fun intermediateStrategy(
        player: Player, state: GameState, communityCards: List<Card>
    ): AIDecision {
        val handStrength = calculateHandStrength(player.holeCards, communityCards)
        val canCheck = player.currentBet >= state.currentBet
        val callAmt = minOf(state.currentBet - player.currentBet, player.chips)
        val potOdds = if (state.totalPot > 0) callAmt.toFloat() / (state.totalPot + callAmt) else 0f

        return when {
            handStrength > 0.8f -> {
                // 强牌 - 加注或全下
                val raiseAmt = minOf(state.currentBet + state.config.bigBlind * 3, player.chips + player.currentBet)
                AIDecision(PlayerAction.RAISE, raiseAmt, "强牌加注")
            }
            handStrength > 0.6f -> {
                // 中等牌 - 跟注或小加注
                if (canCheck) AIDecision(PlayerAction.CHECK, reasoning = "中等牌过牌")
                else AIDecision(PlayerAction.CALL, reasoning = "中等牌跟注")
            }
            handStrength > 0.3f -> {
                // 弱牌 - 根据底池赔率决定
                when {
                    canCheck -> AIDecision(PlayerAction.CHECK, reasoning = "弱牌过牌")
                    potOdds < handStrength -> AIDecision(PlayerAction.CALL, reasoning = "弱牌跟注")
                    else -> AIDecision(PlayerAction.FOLD, reasoning = "弱牌弃牌")
                }
            }
            else -> {
                // 很弱 - 弃牌或过牌
                if (canCheck) AIDecision(PlayerAction.CHECK, reasoning = "垃圾牌过牌")
                else AIDecision(PlayerAction.FOLD, reasoning = "垃圾牌弃牌")
            }
        }
    }

    /**
     * 高级策略 - 考虑位置、筹码比例、对手行为
     */
    private fun advancedStrategy(
        player: Player, state: GameState, communityCards: List<Card>
    ): AIDecision {
        val handStrength = calculateHandStrength(player.holeCards, communityCards)
        val positionBonus = calculatePositionBonus(player, state)
        val effectiveStrength = (handStrength + positionBonus).coerceIn(0f, 1f)
        val canCheck = player.currentBet >= state.currentBet
        val activePlayers = state.activePlayers.size

        // 底池筹码比（SPR）
        val spr = if (state.totalPot > 0) player.chips.toFloat() / state.totalPot else 10f

        // 虚张声势概率（位置好且对手少时更可能诈唬）
        val bluffProb = if (player.isDealer && activePlayers <= 2) 0.25f else 0.1f
        val isBluffing = Random.nextFloat() < bluffProb && handStrength < 0.3f

        return when {
            isBluffing -> {
                val betAmt = (state.totalPot * 0.75).toLong().coerceAtLeast(state.config.bigBlind * 2)
                AIDecision(PlayerAction.BET, betAmt, "诈唬下注")
            }
            effectiveStrength > 0.85f -> {
                // 超强牌 - 价值下注
                val betSize = (state.totalPot * 0.8).toLong().coerceAtLeast(state.config.bigBlind * 2)
                val raiseAmt = (player.currentBet + betSize).coerceAtMost(player.chips + player.currentBet)
                if (canCheck && spr < 2) AIDecision(PlayerAction.ALL_IN, reasoning = "超强牌全下")
                else AIDecision(PlayerAction.RAISE, raiseAmt, "超强牌加注")
            }
            effectiveStrength > 0.65f -> {
                if (canCheck) AIDecision(PlayerAction.CHECK, reasoning = "好牌控池")
                else {
                    val callAmt = minOf(state.currentBet - player.currentBet, player.chips)
                    if (callAmt < player.chips * 0.3) AIDecision(PlayerAction.CALL, reasoning = "好牌跟注")
                    else AIDecision(PlayerAction.FOLD, reasoning = "代价太高放弃")
                }
            }
            effectiveStrength > 0.4f -> {
                if (canCheck) AIDecision(PlayerAction.CHECK, reasoning = "中等牌过牌")
                else {
                    val callAmt = minOf(state.currentBet - player.currentBet, player.chips)
                    val potOdds = callAmt.toFloat() / (state.totalPot + callAmt)
                    if (potOdds < effectiveStrength) AIDecision(PlayerAction.CALL, reasoning = "值得跟注")
                    else AIDecision(PlayerAction.FOLD, reasoning = "赔率不合适")
                }
            }
            else -> {
                if (canCheck) AIDecision(PlayerAction.CHECK, reasoning = "弱牌过牌")
                else AIDecision(PlayerAction.FOLD, reasoning = "弱牌弃牌")
            }
        }
    }

    /**
     * 专家策略 - GTO近似策略
     */
    private fun expertStrategy(
        player: Player, state: GameState, communityCards: List<Card>
    ): AIDecision {
        val baseDecision = advancedStrategy(player, state, communityCards)
        val handStrength = calculateHandStrength(player.holeCards, communityCards)

        // 平衡范围 - 随机化强牌和诈唬的行动
        val randomFactor = Random.nextFloat()

        return when {
            handStrength > 0.9f && randomFactor < 0.3f -> {
                // 慢打强牌
                if (player.currentBet >= state.currentBet)
                    AIDecision(PlayerAction.CHECK, reasoning = "慢打超强牌")
                else
                    AIDecision(PlayerAction.CALL, reasoning = "慢打跟注")
            }
            handStrength < 0.2f && randomFactor < 0.2f -> {
                // 半诈唬
                val betAmt = (state.totalPot * 0.6).toLong().coerceAtLeast(state.config.bigBlind)
                AIDecision(PlayerAction.BET, betAmt, "半诈唬")
            }
            else -> baseDecision
        }
    }

    /**
     * 计算手牌强度 (0.0 ~ 1.0)
     * 使用Monte Carlo模拟近似
     */
    private fun calculateHandStrength(holeCards: List<Card>, communityCards: List<Card>): Float {
        if (holeCards.isEmpty()) return 0.5f

        // 翻前评估 - 基于起手牌强度表
        if (communityCards.isEmpty()) {
            return evaluateStartingHand(holeCards)
        }

        // 翻牌后评估 - 基于当前手牌强度
        val eval = HandEvaluator.evaluate(holeCards, communityCards)
        return when (eval.handRank) {
            HandRank.ROYAL_FLUSH -> 1.0f
            HandRank.STRAIGHT_FLUSH -> 0.97f
            HandRank.FOUR_OF_A_KIND -> 0.94f
            HandRank.FULL_HOUSE -> 0.85f
            HandRank.FLUSH -> 0.78f
            HandRank.STRAIGHT -> 0.72f
            HandRank.THREE_OF_A_KIND -> 0.62f
            HandRank.TWO_PAIR -> 0.52f
            HandRank.ONE_PAIR -> {
                val pairValue = eval.primaryValue
                0.3f + (pairValue - 2f) / 12f * 0.2f
            }
            HandRank.HIGH_CARD -> {
                val highValue = eval.primaryValue
                0.1f + (highValue - 2f) / 12f * 0.15f
            }
        }
    }

    /**
     * 翻前起手牌强度评估
     */
    private fun evaluateStartingHand(holeCards: List<Card>): Float {
        if (holeCards.size < 2) return 0.3f

        val card1 = holeCards[0]
        val card2 = holeCards[1]
        val highRank = maxOf(card1.rank.value, card2.rank.value)
        val lowRank = minOf(card1.rank.value, card2.rank.value)
        val isPair = card1.rank == card2.rank
        val isSuited = card1.suit == card2.suit
        val gap = highRank - lowRank

        return when {
            // AA, KK, QQ
            isPair && highRank >= 12 -> 0.85f + (highRank - 12f) / 2f * 0.1f
            // JJ, TT
            isPair && highRank >= 10 -> 0.75f
            // 99-66
            isPair && highRank >= 6 -> 0.65f
            // 低对
            isPair -> 0.55f
            // AK suited
            highRank == 14 && lowRank == 13 && isSuited -> 0.78f
            // AK offsuit
            highRank == 14 && lowRank == 13 -> 0.72f
            // AQ, AJ suited
            highRank == 14 && lowRank >= 11 && isSuited -> 0.70f
            // AQ, AJ offsuit
            highRank == 14 && lowRank >= 11 -> 0.64f
            // A + medium
            highRank == 14 && isSuited -> 0.55f
            highRank == 14 -> 0.50f
            // KQ suited
            highRank == 13 && lowRank == 12 && isSuited -> 0.65f
            highRank == 13 && lowRank == 12 -> 0.60f
            // 连牌同花
            gap <= 2 && isSuited -> 0.50f
            gap <= 2 -> 0.45f
            // 其他
            isSuited -> 0.35f
            else -> 0.25f
        }
    }

    /**
     * 位置加成
     */
    private fun calculatePositionBonus(player: Player, state: GameState): Float {
        return when {
            player.isDealer -> 0.08f      // 庄家位置最好
            player.isBigBlind -> -0.02f   // 大盲位置最差（已下注，最后下注）
            else -> 0.03f
        }
    }
}
