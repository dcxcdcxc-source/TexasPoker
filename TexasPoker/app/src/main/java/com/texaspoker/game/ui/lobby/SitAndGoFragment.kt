package com.texaspoker.game.ui.lobby

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.texaspoker.game.databinding.FragmentSitAndGoBinding
import com.texaspoker.game.model.TableType
import android.content.Intent
import com.texaspoker.game.ui.table.GameTableActivity

class SitAndGoFragment : Fragment() {

    private var _binding: FragmentSitAndGoBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSitAndGoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupSNG()
    }

    private fun setupSNG() {
        val options = listOf(
            Triple("2人单挑", 500L, 2),
            Triple("6人SnG", 1000L, 6),
            Triple("9人SnG", 2000L, 9),
            Triple("迅速局 6人", 500L, 6),
            Triple("双倍或出局", 1000L, 2)
        )

        binding.btnSng1.text = "单挑 买入:500"
        binding.btnSng2.text = "6人局 买入:1000"
        binding.btnSng3.text = "9人局 买入:2000"
        binding.btnSng4.text = "迅速SnG 买入:500"

        binding.btnSng1.setOnClickListener { startSnG(25, 50, 2) }
        binding.btnSng2.setOnClickListener { startSnG(50, 100, 6) }
        binding.btnSng3.setOnClickListener { startSnG(100, 200, 9) }
        binding.btnSng4.setOnClickListener { startSnG(50, 100, 6) }
    }

    private fun startSnG(sb: Long, bb: Long, players: Int) {
        val intent = Intent(requireContext(), GameTableActivity::class.java).apply {
            putExtra("table_id", "sng_${System.currentTimeMillis()}")
            putExtra("table_name", "${players}人SnG")
            putExtra("small_blind", sb)
            putExtra("big_blind", bb)
            putExtra("table_type", TableType.SIT_AND_GO.name)
            putExtra("max_players", players)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
