package com.texaspoker.game.engine

import com.texaspoker.game.model.*

/**
 * 德州扑克核心游戏引擎
 */
class PokerEngine(private val config: GameConfig) {

    private val state = GameState(config)

    // 事件回调
    var onPhaseChanged: ((GamePhase) -> Unit)? = null
    var onPlayerAction: ((Player, PlayerAction, Long) -> Unit)? = null
    var onCardsDealt: ((List<Player>, List<Card>) -> Unit)? = null
    var onPotUpdated: ((Long) -> Unit)? = null
    var onShowdown: ((List<Pair<Player, HandEvaluation>>) -> Unit)? = null
    var onGameOver: ((Player?) -> Unit)? = null
    var onNextPlayer: ((Player) -> Unit)? = null
    var onChipsChanged: ((Player, Long) -> Unit)? = null
    var onMessage: ((String) -> Unit)? = null

    fun getState(): GameState = state

    /**
     * 添加玩家入桌
     */
    fun addPlayer(player: Player) {
        if (state.players.size < config.maxPlayers) {
            player.seatIndex = state.players.size
            state.players.add(player)
        }
    }

    /**
     * 开始新的一手牌
     */
    fun startNewHand() {
        if (state.players.count { it.chips > 0 } < 2) {
            onMessage?.invoke("需要至少2名有筹码的玩家")
            return
        }

        state.handNumber++
        state.currentPhase = GamePhase.WAITING

        // 重置玩家状态
        state.players.forEach { player ->
            player.holeCards = emptyList()
            player.status = if (player.chips > 0) PlayerStatus.WAITING else PlayerStatus.ELIMINATED
            player.currentBet = 0L
            player.totalBetThisHand = 0L
            player.winAmount = 0L
            player.isDealer = false
            player.isSmallBlind = false
            player.isBigBlind = false
            player.lastAction = null
        }

        // 重置底池
        state.pots.clear()
        state.pots.add(Pot())
        state.communityCards.clear()
        state.currentBet = 0L

        // 移动庄家位置
        val activePlayers = state.players.filter { it.status != PlayerStatus.ELIMINATED }
        if (activePlayers.size < 2) return

        state.dealerIndex = (state.dealerIndex + 1) % state.players.size
        while (state.players[state.dealerIndex].status == PlayerStatus.ELIMINATED) {
            state.dealerIndex = (state.dealerIndex + 1) % state.players.size
        }
        state.players[state.dealerIndex].isDealer = true

        // 发洗牌
        state.deck.clear()
        state.deck.addAll(DeckManager.shuffle(DeckManager.createDeck()))

        // 设置盲注
        setupBlinds()

        // 发底牌
        dealHoleCards()

        // 翻前开始
        startPreFlop()
    }

    private fun setupBlinds() {
        val active = state.players.filter { it.status != PlayerStatus.ELIMINATED && it.chips > 0 }
        if (active.size < 2) return

        val dealerPos = state.dealerIndex
        var sbPos = (dealerPos + 1) % state.players.size
        while (state.players[sbPos].status == PlayerStatus.ELIMINATED || state.players[sbPos].chips == 0L) {
            sbPos = (sbPos + 1) % state.players.size
        }

        var bbPos = (sbPos + 1) % state.players.size
        while (state.players[bbPos].status == PlayerStatus.ELIMINATED || state.players[bbPos].chips == 0L) {
            bbPos = (bbPos + 1) % state.players.size
        }

        state.players[sbPos].isSmallBlind = true
        state.players[bbPos].isBigBlind = true

        // 强制下注小盲
        val sbAmount = minOf(config.smallBlind, state.players[sbPos].chips)
        placeBet(state.players[sbPos], sbAmount)

        // 强制下注大盲
        val bbAmount = minOf(config.bigBlind, state.players[bbPos].chips)
        placeBet(state.players[bbPos], bbAmount)

        state.currentBet = config.bigBlind
        state.minRaise = config.bigBlind
        state.lastRaiseAmount = config.bigBlind

        // 翻前第一个行动者是大盲后面的玩家
        var utgPos = (bbPos + 1) % state.players.size
        while (state.players[utgPos].status == PlayerStatus.ELIMINATED || state.players[utgPos].chips == 0L) {
            utgPos = (utgPos + 1) % state.players.size
        }
        state.currentPlayerIndex = utgPos
    }

    private fun dealHoleCards() {
        val active = state.players.filter { it.status != PlayerStatus.ELIMINATED }
        // 每人发2张底牌
        repeat(2) { _ ->
            active.forEach { player ->
                val cards = DeckManager.deal(state.deck, 1)
                player.holeCards = player.holeCards + cards
            }
        }
        onCardsDealt?.invoke(active, emptyList())
    }

    private fun startPreFlop() {
        state.currentPhase = GamePhase.PRE_FLOP
        onPhaseChanged?.invoke(GamePhase.PRE_FLOP)
        onMessage?.invoke("翻前下注")
        notifyCurrentPlayer()
    }

    /**
     * 玩家执行行动
     */
    fun performAction(playerId: String, action: PlayerAction, amount: Long = 0L) {
        val player = state.players.find { it.id == playerId } ?: return
        if (state.players[state.currentPlayerIndex].id != playerId) return

        when (action) {
            PlayerAction.FOLD -> handleFold(player)
            PlayerAction.CHECK -> handleCheck(player)
            PlayerAction.CALL -> handleCall(player)
            PlayerAction.RAISE -> handleRaise(player, amount)
            PlayerAction.BET -> handleBet(player, amount)
            PlayerAction.ALL_IN -> handleAllIn(player)
        }

        player.lastAction = action
        onPlayerAction?.invoke(player, action, amount)

        // 检查手牌是否结束
        if (checkHandEnd()) return

        // 检查回合是否结束
        if (checkRoundEnd()) {
            advancePhase()
        } else {
            moveToNextPlayer()
        }
    }

    private fun handleFold(player: Player) {
        player.status = PlayerStatus.FOLDED
        onMessage?.invoke("${player.name} 弃牌")
    }

    private fun handleCheck(player: Player) {
        if (player.currentBet < state.currentBet) {
            onMessage?.invoke("当前有下注，无法过牌")
            return
        }
        onMessage?.invoke("${player.name} 过牌")
    }

    private fun handleCall(player: Player) {
        val callAmount = minOf(state.currentBet - player.currentBet, player.chips)
        placeBet(player, callAmount)
        if (player.chips == 0L) player.status = PlayerStatus.ALL_IN
        onMessage?.invoke("${player.name} 跟注 ${callAmount}")
    }

    private fun handleRaise(player: Player, raiseTotal: Long) {
        val raiseAmount = raiseTotal - player.currentBet
        if (raiseAmount <= 0 || raiseAmount > player.chips) return
        val actualAmount = minOf(raiseAmount, player.chips)
        placeBet(player, actualAmount)
        state.lastRaiseAmount = raiseTotal - state.currentBet
        state.currentBet = raiseTotal
        state.minRaise = state.lastRaiseAmount
        if (player.chips == 0L) player.status = PlayerStatus.ALL_IN
        onMessage?.invoke("${player.name} 加注到 ${raiseTotal}")
    }

    private fun handleBet(player: Player, amount: Long) {
        if (amount <= 0 || amount > player.chips) return
        placeBet(player, amount)
        state.currentBet = amount
        state.lastRaiseAmount = amount
        state.minRaise = amount
        if (player.chips == 0L) player.status = PlayerStatus.ALL_IN
        onMessage?.invoke("${player.name} 下注 ${amount}")
    }

    private fun handleAllIn(player: Player) {
        val amount = player.chips
        placeBet(player, amount)
        if (player.currentBet > state.currentBet) {
            state.lastRaiseAmount = player.currentBet - state.currentBet
            state.currentBet = player.currentBet
        }
        player.status = PlayerStatus.ALL_IN
        onMessage?.invoke("${player.name} 全下 ${amount}")
    }

    private fun placeBet(player: Player, amount: Long) {
        val actual = minOf(amount, player.chips)
        player.chips -= actual
        player.currentBet += actual
        player.totalBetThisHand += actual
        addToPot(actual)
        onChipsChanged?.invoke(player, player.chips)
        onPotUpdated?.invoke(state.totalPot)
    }

    private fun addToPot(amount: Long) {
        val pot = state.pots.lastOrNull() ?: Pot().also { state.pots.add(it) }
        state.pots[state.pots.size - 1] = pot.copy(amount = pot.amount + amount)
    }

    private fun checkHandEnd(): Boolean {
        val active = state.players.filter {
            it.status != PlayerStatus.FOLDED && it.status != PlayerStatus.ELIMINATED
        }
        if (active.size == 1) {
            // 所有其他人弃牌，唯一的玩家赢
            val winner = active.first()
            winner.chips += state.totalPot
            winner.winAmount = state.totalPot
            onChipsChanged?.invoke(winner, winner.chips)
            onMessage?.invoke("${winner.name} 赢得底池 ${state.totalPot}")
            onGameOver?.invoke(winner)
            state.currentPhase = GamePhase.GAME_OVER
            return true
        }
        return false
    }

    private fun checkRoundEnd(): Boolean {
        val activePlayers = state.players.filter {
            it.status != PlayerStatus.FOLDED &&
            it.status != PlayerStatus.ELIMINATED &&
            it.status != PlayerStatus.ALL_IN
        }

        if (activePlayers.isEmpty()) return true
        if (activePlayers.all { it.currentBet >= state.currentBet || it.chips == 0L }) {
            // 所有人都已下注到同等水平
            return true
        }
        return false
    }

    private fun advancePhase() {
        // 重置本轮下注
        state.players.forEach { player ->
            if (player.status != PlayerStatus.FOLDED && player.status != PlayerStatus.ELIMINATED) {
                player.currentBet = 0L
                if (player.status == PlayerStatus.ACTIVE) player.status = PlayerStatus.WAITING
            }
        }
        state.currentBet = 0L

        when (state.currentPhase) {
            GamePhase.PRE_FLOP -> dealFlop()
            GamePhase.FLOP -> dealTurn()
            GamePhase.TURN -> dealRiver()
            GamePhase.RIVER -> showdown()
            else -> {}
        }
    }

    private fun dealFlop() {
        state.currentPhase = GamePhase.FLOP
        DeckManager.deal(state.deck, 1)  // 烧牌
        val flop = DeckManager.deal(state.deck, 3)
        state.communityCards.addAll(flop)
        onPhaseChanged?.invoke(GamePhase.FLOP)
        onCardsDealt?.invoke(emptyList(), flop)
        onMessage?.invoke("翻牌: ${flop.joinToString(" ")}")
        setFirstActivePlayerAfterDealer()
        notifyCurrentPlayer()
    }

    private fun dealTurn() {
        state.currentPhase = GamePhase.TURN
        DeckManager.deal(state.deck, 1)  // 烧牌
        val turn = DeckManager.deal(state.deck, 1)
        state.communityCards.addAll(turn)
        onPhaseChanged?.invoke(GamePhase.TURN)
        onCardsDealt?.invoke(emptyList(), turn)
        onMessage?.invoke("转牌: ${turn.first()}")
        setFirstActivePlayerAfterDealer()
        notifyCurrentPlayer()
    }

    private fun dealRiver() {
        state.currentPhase = GamePhase.RIVER
        DeckManager.deal(state.deck, 1)  // 烧牌
        val river = DeckManager.deal(state.deck, 1)
        state.communityCards.addAll(river)
        onPhaseChanged?.invoke(GamePhase.RIVER)
        onCardsDealt?.invoke(emptyList(), river)
        onMessage?.invoke("河牌: ${river.first()}")
        setFirstActivePlayerAfterDealer()
        notifyCurrentPlayer()
    }

    private fun showdown() {
        state.currentPhase = GamePhase.SHOWDOWN
        onPhaseChanged?.invoke(GamePhase.SHOWDOWN)

        val activePlayers = state.players.filter {
            it.status != PlayerStatus.FOLDED && it.status != PlayerStatus.ELIMINATED
        }

        val winners = HandEvaluator.determineWinners(activePlayers, state.communityCards)
        onShowdown?.invoke(winners)

        // 分配底池
        distributePots(winners)

        onMessage?.invoke("摊牌！${winners.joinToString(", ") { "${it.first.name}: ${it.second.description}" }}")
        onGameOver?.invoke(winners.firstOrNull()?.first)
        state.currentPhase = GamePhase.GAME_OVER
    }

    private fun distributePots(winners: List<Pair<Player, HandEvaluation>>) {
        state.pots.forEach { pot ->
            val splitAmount = pot.amount / winners.size
            winners.forEach { (player, _) ->
                player.chips += splitAmount
                player.winAmount += splitAmount
                onChipsChanged?.invoke(player, player.chips)
            }
        }
    }

    private fun setFirstActivePlayerAfterDealer() {
        var pos = (state.dealerIndex + 1) % state.players.size
        repeat(state.players.size) {
            val p = state.players[pos]
            if (p.status != PlayerStatus.FOLDED && p.status != PlayerStatus.ELIMINATED && p.status != PlayerStatus.ALL_IN) {
                state.currentPlayerIndex = pos
                return
            }
            pos = (pos + 1) % state.players.size
        }
    }

    private fun moveToNextPlayer() {
        var nextIdx = (state.currentPlayerIndex + 1) % state.players.size
        repeat(state.players.size) {
            val p = state.players[nextIdx]
            if (p.status != PlayerStatus.FOLDED &&
                p.status != PlayerStatus.ELIMINATED &&
                p.status != PlayerStatus.ALL_IN) {
                state.currentPlayerIndex = nextIdx
                notifyCurrentPlayer()
                return
            }
            nextIdx = (nextIdx + 1) % state.players.size
        }
    }

    private fun notifyCurrentPlayer() {
        val player = state.players.getOrNull(state.currentPlayerIndex) ?: return
        player.status = PlayerStatus.ACTIVE
        onNextPlayer?.invoke(player)
    }

    fun canCheck(player: Player): Boolean = player.currentBet >= state.currentBet
    fun callAmount(player: Player): Long = minOf(state.currentBet - player.currentBet, player.chips)
    fun minRaiseAmount(player: Player): Long = state.currentBet + state.minRaise
    fun maxRaiseAmount(player: Player): Long = player.chips + player.currentBet
}
