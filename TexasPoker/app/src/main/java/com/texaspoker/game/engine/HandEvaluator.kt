package com.texaspoker.game.engine

import com.texaspoker.game.model.*

/**
 * 牌组管理器 - 负责创建和洗牌
 */
object DeckManager {

    fun createDeck(): MutableList<Card> {
        val deck = mutableListOf<Card>()
        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                deck.add(Card(rank, suit))
            }
        }
        return deck
    }

    fun shuffle(deck: MutableList<Card>): MutableList<Card> {
        deck.shuffle()
        return deck
    }

    fun deal(deck: MutableList<Card>, count: Int = 1): List<Card> {
        if (deck.size < count) throw IllegalStateException("牌组中的牌不足")
        val cards = deck.take(count)
        repeat(count) { deck.removeAt(0) }
        return cards
    }
}

/**
 * 手牌评估器 - 德州扑克最强5张牌判断
 */
object HandEvaluator {

    /**
     * 从7张牌中选出最强的5张牌并返回评估结果
     */
    fun evaluate(holeCards: List<Card>, communityCards: List<Card>): HandEvaluation {
        val allCards = holeCards + communityCards
        if (allCards.size < 2) return HandEvaluation(HandRank.HIGH_CARD, 0)

        // 生成所有可能的5张牌组合
        val combinations = getCombinations(allCards, 5)
        return combinations.maxOf { evaluateFiveCards(it) }
    }

    /**
     * 评估精确的5张牌
     */
    fun evaluateFiveCards(cards: List<Card>): HandEvaluation {
        require(cards.size == 5) { "必须是5张牌" }

        val sorted = cards.sortedByDescending { it.rank.value }
        val ranks = sorted.map { it.rank.value }
        val suits = sorted.map { it.suit }

        val isFlush = suits.distinct().size == 1
        val isStraight = isStraight(ranks)
        val rankGroups = ranks.groupBy { it }.mapValues { it.value.size }

        // 皇家同花顺
        if (isFlush && isStraight && ranks.first() == 14) {
            return HandEvaluation(HandRank.ROYAL_FLUSH, 14, description = "皇家同花顺")
        }

        // 同花顺
        if (isFlush && isStraight) {
            val highCard = if (ranks == listOf(14, 5, 4, 3, 2)) 5 else ranks.first()
            return HandEvaluation(HandRank.STRAIGHT_FLUSH, highCard, description = "同花顺 ${Rank.values().find { it.value == highCard }?.display}高")
        }

        // 四条
        val fourOfAKind = rankGroups.entries.find { it.value == 4 }
        if (fourOfAKind != null) {
            val kicker = ranks.first { it != fourOfAKind.key }
            return HandEvaluation(HandRank.FOUR_OF_A_KIND, fourOfAKind.key, listOf(kicker),
                "四条 ${rankDisplay(fourOfAKind.key)}")
        }

        // 葫芦
        val threeOfAKind = rankGroups.entries.find { it.value == 3 }
        val pair = rankGroups.entries.find { it.value == 2 }
        if (threeOfAKind != null && pair != null) {
            return HandEvaluation(HandRank.FULL_HOUSE, threeOfAKind.key, listOf(pair.key),
                "葫芦 ${rankDisplay(threeOfAKind.key)}带${rankDisplay(pair.key)}")
        }

        // 同花
        if (isFlush) {
            return HandEvaluation(HandRank.FLUSH, ranks.first(), ranks.drop(1),
                "同花 ${rankDisplay(ranks.first())}高")
        }

        // 顺子
        if (isStraight) {
            val highCard = if (ranks == listOf(14, 5, 4, 3, 2)) 5 else ranks.first()
            return HandEvaluation(HandRank.STRAIGHT, highCard, description = "顺子 ${rankDisplay(highCard)}高")
        }

        // 三条
        if (threeOfAKind != null) {
            val kickers = ranks.filter { it != threeOfAKind.key }.sortedDescending()
            return HandEvaluation(HandRank.THREE_OF_A_KIND, threeOfAKind.key, kickers,
                "三条 ${rankDisplay(threeOfAKind.key)}")
        }

        // 两对
        val pairs = rankGroups.entries.filter { it.value == 2 }.sortedByDescending { it.key }
        if (pairs.size == 2) {
            val kicker = ranks.first { it != pairs[0].key && it != pairs[1].key }
            return HandEvaluation(HandRank.TWO_PAIR, pairs[0].key, listOf(pairs[1].key, kicker),
                "两对 ${rankDisplay(pairs[0].key)}和${rankDisplay(pairs[1].key)}")
        }

        // 一对
        if (pairs.size == 1) {
            val kickers = ranks.filter { it != pairs[0].key }.sortedDescending()
            return HandEvaluation(HandRank.ONE_PAIR, pairs[0].key, kickers,
                "一对 ${rankDisplay(pairs[0].key)}")
        }

        // 高牌
        return HandEvaluation(HandRank.HIGH_CARD, ranks.first(), ranks.drop(1),
            "高牌 ${rankDisplay(ranks.first())}")
    }

    private fun isStraight(ranks: List<Int>): Boolean {
        val sorted = ranks.sortedDescending()
        // 正常顺子
        if (sorted[0] - sorted[4] == 4 && sorted.distinct().size == 5) return true
        // A-2-3-4-5 (最小顺子)
        if (sorted == listOf(14, 5, 4, 3, 2)) return true
        return false
    }

    private fun getCombinations(cards: List<Card>, k: Int): List<List<Card>> {
        if (k == 0) return listOf(emptyList())
        if (cards.isEmpty()) return emptyList()
        val result = mutableListOf<List<Card>>()
        for (i in cards.indices) {
            val rest = getCombinations(cards.drop(i + 1), k - 1)
            rest.forEach { result.add(listOf(cards[i]) + it) }
        }
        return result
    }

    private fun rankDisplay(value: Int): String =
        Rank.values().find { it.value == value }?.display ?: value.toString()

    /**
     * 判断胜者（返回胜者索引列表，多个表示平局）
     */
    fun determineWinners(
        players: List<Player>,
        communityCards: List<Card>
    ): List<Pair<Player, HandEvaluation>> {
        val evaluations = players
            .filter { it.isActive && it.holeCards.isNotEmpty() }
            .map { player ->
                val eval = evaluate(player.holeCards, communityCards)
                Pair(player, eval)
            }

        if (evaluations.isEmpty()) return emptyList()

        val maxEval = evaluations.maxOf { it.second }
        return evaluations.filter { it.second.compareTo(maxEval) == 0 }
    }
}
