package com.texaspoker.game.ui.table

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.texaspoker.game.ai.AIDecisionEngine
import com.texaspoker.game.ai.AIDifficulty
import com.texaspoker.game.data.GameRepository
import com.texaspoker.game.databinding.ActivityGameTableBinding
import com.texaspoker.game.engine.PokerEngine
import com.texaspoker.game.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID

class GameTableActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGameTableBinding
    private lateinit var engine: PokerEngine
    private lateinit var repository: GameRepository
    private lateinit var aiEngine: AIDecisionEngine
    private lateinit var tableView: PokerTableView
    private val handler = Handler(Looper.getMainLooper())

    private var humanPlayer: Player? = null
    private var config: GameConfig? = null
    private var isGameRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGameTableBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GameRepository(this)

        // 读取桌配置
        val tableId = intent.getStringExtra("table_id") ?: "default"
        val tableName = intent.getStringExtra("table_name") ?: "德州扑克"
        val sb = intent.getLongExtra("small_blind", 25L)
        val bb = intent.getLongExtra("big_blind", 50L)
        val maxPlayers = intent.getIntExtra("max_players", 9)

        config = GameConfig(
            tableId = tableId,
            tableName = tableName,
            smallBlind = sb,
            bigBlind = bb,
            maxPlayers = maxPlayers,
            aiCount = maxPlayers - 1
        )

        val settings = repository.loadSettings()
        aiEngine = AIDecisionEngine(
            when (settings.aiDifficultyIndex) {
                0 -> AIDifficulty.BEGINNER
                2 -> AIDifficulty.ADVANCED
                3 -> AIDifficulty.EXPERT
                else -> AIDifficulty.INTERMEDIATE
            }
        )

        binding.tvTableName.text = tableName
        binding.tvBlinds.text = "${sb}/${bb}"

        setupTableView()
        setupButtons()
        initGame()
    }

    private fun setupTableView() {
        tableView = PokerTableView(this)
        binding.tableContainer.addView(tableView)
    }

    private fun setupButtons() {
        binding.btnFold.setOnClickListener { performAction(PlayerAction.FOLD) }
        binding.btnCheck.setOnClickListener { performAction(PlayerAction.CHECK) }
        binding.btnCall.setOnClickListener { performAction(PlayerAction.CALL) }
        binding.btnRaise.setOnClickListener {
            val amount = binding.sliderBet.value.toLong()
            performAction(PlayerAction.RAISE, amount)
        }
        binding.btnAllIn.setOnClickListener { performAction(PlayerAction.ALL_IN) }
        binding.btnLeave.setOnClickListener { finish() }

        binding.sliderBet.addOnChangeListener { _, value, _ ->
            binding.tvBetAmount.text = "加注: ${value.toLong()}"
        }
    }

    private fun initGame() {
        val cfg = config ?: return
        engine = PokerEngine(cfg)

        // 添加真人玩家
        val user = repository.loadUser()
        val playerChips = minOf(user?.chips ?: 10000L, cfg.maxBuyIn)
        humanPlayer = Player(
            id = user?.userId ?: "human",
            name = user?.username ?: "你",
            type = PlayerType.HUMAN,
            chips = playerChips,
            seatIndex = 0
        )
        engine.addPlayer(humanPlayer!!)

        // 添加AI玩家
        val aiNames = listOf("Alex", "Bob", "Carol", "Dave", "Eva", "Frank", "Grace", "Henry")
        val aiCount = minOf(cfg.aiCount, aiNames.size)
        for (i in 0 until aiCount) {
            val aiChips = (3000L..15000L).random()
            engine.addPlayer(
                Player(
                    id = "ai_$i",
                    name = aiNames[i],
                    type = PlayerType.AI,
                    chips = aiChips,
                    seatIndex = i + 1
                )
            )
        }

        // 注册引擎回调
        setupEngineCallbacks()

        // 开始第一手牌
        handler.postDelayed({ startNewHand() }, 1000)
    }

    private fun setupEngineCallbacks() {
        engine.onPhaseChanged = { phase ->
            runOnUiThread {
                binding.tvPhase.text = when (phase) {
                    GamePhase.PRE_FLOP -> "翻前"
                    GamePhase.FLOP -> "翻牌"
                    GamePhase.TURN -> "转牌"
                    GamePhase.RIVER -> "河牌"
                    GamePhase.SHOWDOWN -> "摊牌"
                    else -> ""
                }
                tableView.updateState(engine.getState())
            }
        }

        engine.onCardsDealt = { players, community ->
            runOnUiThread {
                tableView.updateState(engine.getState())
                // 显示玩家手牌（仅对真人）
                humanPlayer?.let { hp ->
                    showHoleCards(hp.holeCards)
                }
            }
        }

        engine.onPotUpdated = { pot ->
            runOnUiThread {
                binding.tvPot.text = "底池: ${formatChips(pot)}"
            }
        }

        engine.onNextPlayer = { player ->
            runOnUiThread {
                if (player.type == PlayerType.HUMAN) {
                    showActionButtons(true)
                    updateActionButtons(player)
                    startTurnTimer()
                } else {
                    showActionButtons(false)
                    // AI延迟行动
                    handler.postDelayed({
                        executeAIAction(player)
                    }, (800L..2000L).random())
                }
                tableView.highlightPlayer(player.seatIndex)
            }
        }

        engine.onShowdown = { winners ->
            runOnUiThread {
                tableView.showAllCards(engine.getState())
                val msg = winners.joinToString("\n") { (player, eval) ->
                    "${player.name}: ${eval.description}"
                }
                showMessage("摊牌结果:\n$msg")
            }
        }

        engine.onGameOver = { winner ->
            runOnUiThread {
                showActionButtons(false)
                winner?.let {
                    val isHumanWin = it.id == humanPlayer?.id
                    if (isHumanWin) {
                        showWinAnimation(it.winAmount)
                    }
                }
                // 保存筹码
                humanPlayer?.let { hp ->
                    repository.updateChips(hp.id, hp.chips)
                }
                // 2秒后开始下一手
                handler.postDelayed({
                    if (!isFinishing) startNewHand()
                }, 3000)
            }
        }

        engine.onMessage = { msg ->
            runOnUiThread {
                binding.tvMessage.text = msg
            }
        }

        engine.onChipsChanged = { player, chips ->
            runOnUiThread {
                if (player.id == humanPlayer?.id) {
                    binding.tvMyChips.text = "筹码: ${formatChips(chips)}"
                }
                tableView.updatePlayerChips(player.seatIndex, chips)
            }
        }
    }

    private fun startNewHand() {
        if (isFinishing) return
        isGameRunning = true
        binding.tvPot.text = "底池: 0"
        tableView.clearTable()
        engine.startNewHand()
    }

    private fun performAction(action: PlayerAction, amount: Long = 0L) {
        val player = humanPlayer ?: return
        cancelTurnTimer()
        showActionButtons(false)
        engine.performAction(player.id, action, amount)
    }

    private fun executeAIAction(aiPlayer: Player) {
        val state = engine.getState()
        val decision = aiEngine.makeDecision(aiPlayer, state, state.communityCards)
        engine.performAction(aiPlayer.id, decision.action, decision.amount)
    }

    private fun updateActionButtons(player: Player) {
        val state = engine.getState()
        val canCheck = engine.canCheck(player)
        val callAmt = engine.callAmount(player)
        val minRaise = engine.minRaiseAmount(player)
        val maxRaise = engine.maxRaiseAmount(player)

        binding.btnCheck.visibility = if (canCheck) View.VISIBLE else View.GONE
        binding.btnCall.visibility = if (!canCheck) View.VISIBLE else View.GONE
        binding.btnCall.text = "跟注 ${formatChips(callAmt)}"

        // 设置加注滑块
        binding.sliderBet.valueFrom = minRaise.toFloat()
        binding.sliderBet.valueTo = maxRaise.toFloat().coerceAtLeast(minRaise.toFloat() + 1)
        binding.sliderBet.value = minRaise.toFloat()
        binding.tvBetAmount.text = "加注: ${formatChips(minRaise)}"

        binding.btnAllIn.text = "全下 ${formatChips(player.chips)}"
    }

    private fun showActionButtons(show: Boolean) {
        val visibility = if (show) View.VISIBLE else View.GONE
        binding.actionPanel.visibility = visibility
    }

    private fun showHoleCards(cards: List<Card>) {
        if (cards.size >= 2) {
            binding.cardView1.setCard(cards[0])
            binding.cardView2.setCard(cards[1])
            binding.myCardsPanel.visibility = View.VISIBLE
        }
    }

    private fun showWinAnimation(amount: Long) {
        binding.tvWinAmount.text = "+${formatChips(amount)}"
        binding.tvWinAmount.visibility = View.VISIBLE
        handler.postDelayed({
            binding.tvWinAmount.visibility = View.GONE
        }, 2500)
        showMessage("恭喜！你赢了 ${formatChips(amount)}！")
    }

    private fun showMessage(msg: String) {
        binding.tvMessage.text = msg
    }

    private var turnTimerRunnable: Runnable? = null
    private var timeLeft = 30

    private fun startTurnTimer() {
        timeLeft = 30
        turnTimerRunnable = object : Runnable {
            override fun run() {
                binding.tvTimer.text = "${timeLeft}s"
                if (timeLeft <= 5) binding.tvTimer.setTextColor(0xFFFF3333.toInt())
                else binding.tvTimer.setTextColor(0xFFFFFFFF.toInt())
                timeLeft--
                if (timeLeft < 0) {
                    // 超时自动弃牌
                    performAction(PlayerAction.FOLD)
                } else {
                    handler.postDelayed(this, 1000)
                }
            }
        }
        handler.post(turnTimerRunnable!!)
    }

    private fun cancelTurnTimer() {
        turnTimerRunnable?.let { handler.removeCallbacks(it) }
        binding.tvTimer.text = ""
    }

    private fun formatChips(amount: Long): String = when {
        amount >= 1_000_000L -> String.format("%.1fM", amount / 1_000_000.0)
        amount >= 1_000L -> String.format("%.1fK", amount / 1_000.0)
        else -> amount.toString()
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelTurnTimer()
        // 保存筹码
        humanPlayer?.let { repository.updateChips(it.id, it.chips) }
    }
}
