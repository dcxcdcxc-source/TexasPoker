package com.texaspoker.game.ui.table

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.texaspoker.game.model.Card

/**
 * 单张扑克牌View - 用于显示玩家手牌
 */
class CardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var card: Card? = null
    private var faceUp = true
    private var isHighlighted = false

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        bgPaint.style = Paint.Style.FILL
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = 2f
        textPaint.typeface = Typeface.DEFAULT_BOLD
        textPaint.textAlign = Paint.Align.CENTER
    }

    fun setCard(card: Card, faceUp: Boolean = true) {
        this.card = card
        this.faceUp = faceUp
        invalidate()
    }

    fun setHighlighted(highlighted: Boolean) {
        isHighlighted = highlighted
        invalidate()
    }

    fun flip() {
        faceUp = !faceUp
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val rect = RectF(2f, 2f, w - 2f, h - 2f)

        if (!faceUp || card == null) {
            // 牌背
            bgPaint.color = Color.parseColor("#1565C0")
            canvas.drawRoundRect(rect, 10f, 10f, bgPaint)
            borderPaint.color = Color.parseColor("#1976D2")
            canvas.drawRoundRect(RectF(6f, 6f, w - 6f, h - 6f), 7f, 7f, borderPaint)
            return
        }

        val c = card!!

        // 卡片背景
        bgPaint.color = Color.WHITE
        canvas.drawRoundRect(rect, 10f, 10f, bgPaint)

        // 高亮描边
        borderPaint.color = if (isHighlighted) Color.YELLOW else Color.parseColor("#DDDDDD")
        borderPaint.strokeWidth = if (isHighlighted) 4f else 2f
        canvas.drawRoundRect(rect, 10f, 10f, borderPaint)

        val cardColor = if (c.isRed) Color.parseColor("#CC0000") else Color.parseColor("#111111")

        // 左上角点数
        textPaint.color = cardColor
        textPaint.textSize = h * 0.25f
        canvas.drawText(c.rank.display, w * 0.25f, h * 0.28f, textPaint)

        // 左上花色
        textPaint.textSize = h * 0.22f
        canvas.drawText(c.suit.symbol, w * 0.25f, h * 0.48f, textPaint)

        // 中心大花色
        textPaint.textSize = h * 0.42f
        canvas.drawText(c.suit.symbol, w / 2f, h * 0.68f, textPaint)

        // 右下（倒置）
        textPaint.textSize = h * 0.18f
        canvas.save()
        canvas.rotate(180f, w / 2f, h / 2f)
        canvas.drawText(c.rank.display, w * 0.25f, h * 0.28f, textPaint)
        canvas.restore()
    }
}
