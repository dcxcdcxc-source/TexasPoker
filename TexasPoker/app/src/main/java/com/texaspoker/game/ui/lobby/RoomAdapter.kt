package com.texaspoker.game.ui.lobby

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.texaspoker.game.R
import com.texaspoker.game.databinding.ItemRoomBinding
import com.texaspoker.game.model.RoomInfo

class RoomAdapter(
    private val rooms: List<RoomInfo>,
    private val onJoin: (RoomInfo) -> Unit
) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    inner class RoomViewHolder(private val binding: ItemRoomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(room: RoomInfo) {
            binding.tvTableName.text = room.tableName
            binding.tvBlinds.text = "${formatChips(room.smallBlind)}/${formatChips(room.bigBlind)}"
            binding.tvPlayers.text = "${room.playerCount}/${room.maxPlayers}"
            binding.tvAvgPot.text = "均底池: ${formatChips(room.avgPot)}"
            binding.tvHandsPerHour.text = "${room.handsPerHour}手/时"

            // 根据满员情况显示状态
            val fillRatio = room.playerCount.toFloat() / room.maxPlayers
            binding.progressPlayers.progress = (fillRatio * 100).toInt()

            when {
                fillRatio >= 1f -> {
                    binding.btnJoin.text = "观看"
                    binding.btnJoin.setBackgroundColor(0xFF888888.toInt())
                }
                fillRatio >= 0.7f -> {
                    binding.btnJoin.text = "加入"
                    binding.btnJoin.setBackgroundColor(0xFFFF6600.toInt())
                }
                else -> {
                    binding.btnJoin.text = "加入"
                    binding.btnJoin.setBackgroundColor(0xFF00AA44.toInt())
                }
            }

            binding.btnJoin.setOnClickListener { onJoin(room) }
            binding.root.setOnClickListener { onJoin(room) }
        }

        private fun formatChips(amount: Long): String = when {
            amount >= 1_000_000L -> "${amount / 1_000_000}M"
            amount >= 1_000L -> "${amount / 1_000}K"
            else -> amount.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = ItemRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        holder.bind(rooms[position])
    }

    override fun getItemCount() = rooms.size
}
