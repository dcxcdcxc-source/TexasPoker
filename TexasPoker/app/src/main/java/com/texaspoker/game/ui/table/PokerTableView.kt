package com.texaspoker.game.ui.table

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.texaspoker.game.model.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * 自定义牌桌View - GGPoker风格绿色椭圆牌桌
 */
class PokerTableView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var gameState: GameState? = null
    private val players = mutableListOf<Player>()
    private val communityCards = mutableListOf<Card>()
    private var highlightedSeat = -1
    private var showAllCards = false

    // 绘制工具
    private val tablePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val tableRimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val seatPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chipsPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val potPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 座位位置（9人桌）
    private val seatAngles = floatArrayOf(
        270f,   // 0: 底部中间（真人）
        330f,   // 1
        30f,    // 2
        90f,    // 3
        150f,   // 4
        210f,   // 5
        250f,   // 6
        310f,   // 7
        0f      // 8
    )

    init {
        tablePaint.color = Color.parseColor("#1B5E20")  // 深绿色
        tablePaint.style = Paint.Style.FILL

        tableRimPaint.color = Color.parseColor("#8B4513")  // 棕色桌沿
        tableRimPaint.style = Paint.Style.STROKE
        tableRimPaint.strokeWidth = 20f

        seatPaint.color = Color.parseColor("#2D2D2D")
        seatPaint.style = Paint.Style.FILL

        cardPaint.color = Color.WHITE
        cardPaint.style = Paint.Style.FILL

        textPaint.color = Color.WHITE
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.typeface = Typeface.DEFAULT_BOLD

        chipsPaint.color = Color.parseColor("#FFD700")  // 金色
        chipsPaint.textAlign = Paint.Align.CENTER
        chipsPaint.typeface = Typeface.DEFAULT_BOLD

        potPaint.color = Color.parseColor("#FFD700")
        potPaint.textAlign = Paint.Align.CENTER
        potPaint.typeface = Typeface.DEFAULT_BOLD

        highlightPaint.color = Color.parseColor("#FFEB3B")  // 黄色高亮
        highlightPaint.style = Paint.Style.STROKE
        highlightPaint.strokeWidth = 4f
    }

    fun updateState(state: GameState) {
        gameState = state
        players.clear()
        players.addAll(state.players)
        communityCards.clear()
        communityCards.addAll(state.communityCards)
        invalidate()
    }

    fun highlightPlayer(seatIndex: Int) {
        highlightedSeat = seatIndex
        invalidate()
    }

    fun showAllCards(state: GameState) {
        showAllCards = true
        updateState(state)
    }

    fun clearTable() {
        communityCards.clear()
        showAllCards = false
        highlightedSeat = -1
        invalidate()
    }

    fun updatePlayerChips(seatIndex: Int, chips: Long) {
        players.getOrNull(seatIndex)?.let { p ->
            // chips已在Player对象中更新
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val cx = w / 2f
        val cy = h / 2f

        // 绘制背景
        canvas.drawColor(Color.parseColor("#1A1A2E"))

        // 绘制桌沿
        val rimRect = RectF(20f, 20f, w - 20f, h - 20f)
        canvas.drawOval(rimRect, tableRimPaint)

        // 绘制桌面
        val tableRect = RectF(30f, 30f, w - 30f, h - 30f)
        canvas.drawOval(tableRect, tablePaint)

        // 绘制桌面花纹
        drawTablePattern(canvas, cx, cy, w, h)

        // 绘制底池
        drawPot(canvas, cx, cy)

        // 绘制公共牌
        drawCommunityCards(canvas, cx, cy)

        // 绘制玩家座位
        val tableRadiusX = (w - 80f) / 2f * 0.82f
        val tableRadiusY = (h - 80f) / 2f * 0.82f

        players.forEachIndexed { idx, player ->
            val angle = if (idx < seatAngles.size) seatAngles[idx] else (idx * 40f)
            val rad = Math.toRadians(angle.toDouble())
            val sx = cx + tableRadiusX * cos(rad).toFloat()
            val sy = cy + tableRadiusY * sin(rad).toFloat()
            drawPlayerSeat(canvas, player, sx, sy, idx == highlightedSeat)
        }
    }

    private fun drawTablePattern(canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float) {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E7D32")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        // 简单装饰圆
        canvas.drawOval(RectF(cx - 80f, cy - 40f, cx + 80f, cy + 40f), linePaint)
    }

    private fun drawPot(canvas: Canvas, cx: Float, cy: Float) {
        val pot = gameState?.totalPot ?: 0L
        if (pot == 0L) return

        potPaint.textSize = 24f
        canvas.drawText("底池: ${formatChips(pot)}", cx, cy - 20f, potPaint)
    }

    private fun drawCommunityCards(canvas: Canvas, cx: Float, cy: Float) {
        if (communityCards.isEmpty()) return

        val cardW = 48f
        val cardH = 68f
        val spacing = 8f
        val totalW = communityCards.size * (cardW + spacing) - spacing
        val startX = cx - totalW / 2f

        communityCards.forEachIndexed { idx, card ->
            val x = startX + idx * (cardW + spacing)
            val y = cy - cardH / 2f + 25f
            drawCard(canvas, card, x, y, cardW, cardH, true)
        }
    }

    private fun drawPlayerSeat(canvas: Canvas, player: Player, x: Float, y: Float, highlight: Boolean) {
        val seatRadius = 45f

        // 高亮显示当前行动玩家
        if (highlight) {
            val hlPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFEB3B")
                style = Paint.Style.STROKE
                strokeWidth = 4f
            }
            canvas.drawCircle(x, y, seatRadius + 6f, hlPaint)
        }

        // 座位背景
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = when {
                player.status == PlayerStatus.FOLDED -> Color.parseColor("#444444")
                player.status == PlayerStatus.ALL_IN -> Color.parseColor("#880088")
                player.status == PlayerStatus.ACTIVE -> Color.parseColor("#1565C0")
                else -> Color.parseColor("#2D2D2D")
            }
            style = Paint.Style.FILL
        }
        canvas.drawCircle(x, y, seatRadius, bgPaint)

        // 边框
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (highlight) Color.YELLOW else Color.parseColor("#555555")
            style = Paint.Style.STROKE
            strokeWidth = 2f
        }
        canvas.drawCircle(x, y, seatRadius, borderPaint)

        // 玩家名字
        textPaint.textSize = 14f
        textPaint.color = if (player.status == PlayerStatus.FOLDED) Color.GRAY else Color.WHITE
        canvas.drawText(player.name.take(6), x, y - 8f, textPaint)

        // 筹码
        chipsPaint.textSize = 13f
        canvas.drawText(formatChips(player.chips), x, y + 10f, chipsPaint)

        // 标识（庄家/大小盲）
        val badge = when {
            player.isDealer -> "D"
            player.isSmallBlind -> "SB"
            player.isBigBlind -> "BB"
            else -> null
        }
        badge?.let {
            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF6F00")
                style = Paint.Style.FILL
            }
            canvas.drawCircle(x + seatRadius - 5f, y - seatRadius + 5f, 14f, badgePaint)
            textPaint.textSize = 10f
            textPaint.color = Color.WHITE
            canvas.drawText(it, x + seatRadius - 5f, y - seatRadius + 9f, textPaint)
        }

        // 显示牌背（AI有牌时）或弃牌状态
        if (player.status != PlayerStatus.FOLDED && player.holeCards.isNotEmpty()) {
            if (showAllCards && player.holeCards.isNotEmpty()) {
                // 显示底牌
                drawMiniCard(canvas, player.holeCards[0], x - 20f, y + seatRadius + 5f)
                drawMiniCard(canvas, player.holeCards[1], x + 5f, y + seatRadius + 5f)
            } else {
                // 画牌背
                drawCardBack(canvas, x - 20f, y + seatRadius + 5f)
                drawCardBack(canvas, x + 5f, y + seatRadius + 5f)
            }
        }

        // 本轮下注额
        if (player.currentBet > 0) {
            val betPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFD700")
                textAlign = Paint.Align.CENTER
                textSize = 12f
            }
            canvas.drawText("${formatChips(player.currentBet)}", x, y - seatRadius - 5f, betPaint)
        }
    }

    private fun drawCard(canvas: Canvas, card: Card, x: Float, y: Float, w: Float, h: Float, faceUp: Boolean) {
        if (!faceUp) {
            drawCardBack(canvas, x, y, w, h)
            return
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 6f, 6f, paint)

        // 描边
        val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CCCCCC")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(rect, 6f, 6f, border)

        val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (card.isRed) Color.RED else Color.BLACK
            textAlign = Paint.Align.LEFT
            textSize = h * 0.28f
            typeface = Typeface.DEFAULT_BOLD
        }

        canvas.drawText(card.rank.display, x + 3f, y + h * 0.32f, tp)

        val suitPaint = Paint(tp).apply { textSize = h * 0.26f; textAlign = Paint.Align.CENTER }
        canvas.drawText(card.suit.symbol, x + w / 2f, y + h * 0.68f, suitPaint)
    }

    private fun drawCardBack(canvas: Canvas, x: Float, y: Float, w: Float = 28f, h: Float = 38f) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1565C0")
            style = Paint.Style.FILL
        }
        val rect = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(rect, 4f, 4f, paint)

        val pattern = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1976D2")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        canvas.drawRoundRect(RectF(x + 2f, y + 2f, x + w - 2f, y + h - 2f), 3f, 3f, pattern)
    }

    private fun drawMiniCard(canvas: Canvas, card: Card, x: Float, y: Float) {
        drawCard(canvas, card, x, y, 26f, 36f, true)
    }

    private fun formatChips(amount: Long): String = when {
        amount >= 1_000_000L -> String.format("%.1fM", amount / 1_000_000.0)
        amount >= 1_000L -> String.format("%.1fK", amount / 1_000.0)
        else -> amount.toString()
    }
}
