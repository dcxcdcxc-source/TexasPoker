package com.texaspoker.game.model

/**
 * 扑克牌花色
 */
enum class Suit(val symbol: String, val color: String) {
    SPADES("♠", "black"),
    HEARTS("♥", "red"),
    DIAMONDS("♦", "red"),
    CLUBS("♣", "black")
}

/**
 * 扑克牌点数
 */
enum class Rank(val value: Int, val display: String) {
    TWO(2, "2"),
    THREE(3, "3"),
    FOUR(4, "4"),
    FIVE(5, "5"),
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A")
}

/**
 * 一张扑克牌
 */
data class Card(val rank: Rank, val suit: Suit) {
    val isRed: Boolean get() = suit == Suit.HEARTS || suit == Suit.DIAMONDS
    val display: String get() = "${rank.display}${suit.symbol}"

    override fun toString(): String = display
}

/**
 * 手牌类型（从低到高）
 */
enum class HandRank(val displayName: String) {
    HIGH_CARD("高牌"),
    ONE_PAIR("一对"),
    TWO_PAIR("两对"),
    THREE_OF_A_KIND("三条"),
    STRAIGHT("顺子"),
    FLUSH("同花"),
    FULL_HOUSE("葫芦"),
    FOUR_OF_A_KIND("四条"),
    STRAIGHT_FLUSH("同花顺"),
    ROYAL_FLUSH("皇家同花顺")
}

/**
 * 手牌评估结果
 */
data class HandEvaluation(
    val handRank: HandRank,
    val primaryValue: Int,
    val secondaryValues: List<Int> = emptyList(),
    val description: String = handRank.displayName
) : Comparable<HandEvaluation> {
    override fun compareTo(other: HandEvaluation): Int {
        if (handRank.ordinal != other.handRank.ordinal) {
            return handRank.ordinal.compareTo(other.handRank.ordinal)
        }
        if (primaryValue != other.primaryValue) {
            return primaryValue.compareTo(other.primaryValue)
        }
        for (i in secondaryValues.indices) {
            if (i >= other.secondaryValues.size) break
            val cmp = secondaryValues[i].compareTo(other.secondaryValues[i])
            if (cmp != 0) return cmp
        }
        return 0
    }
}
