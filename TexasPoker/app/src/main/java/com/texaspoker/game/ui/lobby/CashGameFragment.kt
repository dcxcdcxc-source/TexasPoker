package com.texaspoker.game.ui.lobby

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.texaspoker.game.databinding.FragmentCashGameBinding
import com.texaspoker.game.model.*
import com.texaspoker.game.ui.table.GameTableActivity

class CashGameFragment : Fragment() {

    private var _binding: FragmentCashGameBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCashGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTables()
    }

    private fun setupTables() {
        val rooms = generateRooms()
        val adapter = RoomAdapter(rooms) { room ->
            joinTable(room)
        }
        binding.rvRooms.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRooms.adapter = adapter
    }

    private fun generateRooms(): List<RoomInfo> = listOf(
        RoomInfo("t1", "新手桌", TableType.CASH_GAME, 10, 20, 3, 9, 450, 85),
        RoomInfo("t2", "微注桌", TableType.CASH_GAME, 25, 50, 5, 9, 1200, 90),
        RoomInfo("t3", "小注桌", TableType.CASH_GAME, 50, 100, 7, 9, 3500, 88),
        RoomInfo("t4", "中注桌", TableType.CASH_GAME, 100, 200, 6, 9, 8000, 82),
        RoomInfo("t5", "高注桌", TableType.CASH_GAME, 500, 1000, 4, 9, 35000, 78),
        RoomInfo("t6", "豪客桌", TableType.CASH_GAME, 1000, 2000, 2, 9, 120000, 65),
        RoomInfo("t7", "6人桌", TableType.CASH_GAME, 50, 100, 4, 6, 2800, 95),
        RoomInfo("t8", "短牌桌 VIP", TableType.CASH_GAME, 200, 400, 5, 6, 15000, 80)
    )

    private fun joinTable(room: RoomInfo) {
        val user = (activity as? MainActivity)?.currentUser ?: return

        if (user.chips < room.bigBlind * 20) {
            (activity as? MainActivity)?.let {
                android.widget.Toast.makeText(
                    it, "筹码不足，请先获取筹码", android.widget.Toast.LENGTH_SHORT
                ).show()
            }
            return
        }

        val intent = Intent(requireContext(), GameTableActivity::class.java).apply {
            putExtra("table_id", room.tableId)
            putExtra("table_name", room.tableName)
            putExtra("small_blind", room.smallBlind)
            putExtra("big_blind", room.bigBlind)
            putExtra("table_type", room.tableType.name)
            putExtra("max_players", room.maxPlayers)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
