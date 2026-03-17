package com.texaspoker.game.ui.lobby

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.texaspoker.game.databinding.FragmentTournamentListBinding
import com.texaspoker.game.model.*
import com.texaspoker.game.ui.tournament.TournamentActivity
import java.util.concurrent.TimeUnit

class TournamentListFragment : Fragment() {

    private var _binding: FragmentTournamentListBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentTournamentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupTournaments()
    }

    private fun setupTournaments() {
        val now = System.currentTimeMillis()
        val tournaments = listOf(
            TournamentInfo("tn1", "每日锦标赛", 1000L, now + TimeUnit.MINUTES.toMillis(15), 45, 100, 80000L, TournamentStatus.REGISTERING),
            TournamentInfo("tn2", "周末大赛", 5000L, now + TimeUnit.HOURS.toMillis(2), 123, 200, 800000L, TournamentStatus.REGISTERING),
            TournamentInfo("tn3", "免费大赛 Freeroll", 0L, now + TimeUnit.MINUTES.toMillis(5), 89, 500, 50000L, TournamentStatus.REGISTERING),
            TournamentInfo("tn4", "高手锦标赛", 10000L, now - TimeUnit.MINUTES.toMillis(30), 200, 200, 1500000L, TournamentStatus.RUNNING),
            TournamentInfo("tn5", "超级卫星赛", 2000L, now + TimeUnit.HOURS.toMillis(4), 67, 150, 250000L, TournamentStatus.REGISTERING)
        )

        val adapter = TournamentAdapter(tournaments) { tournament ->
            joinTournament(tournament)
        }
        binding.rvTournaments.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTournaments.adapter = adapter
    }

    private fun joinTournament(tournament: TournamentInfo) {
        val user = (activity as? MainActivity)?.currentUser ?: return
        if (tournament.buyIn > 0 && user.chips < tournament.buyIn) {
            android.widget.Toast.makeText(activity, "筹码不足", android.widget.Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(requireContext(), TournamentActivity::class.java).apply {
            putExtra("tournament_id", tournament.tournamentId)
            putExtra("tournament_name", tournament.name)
            putExtra("buy_in", tournament.buyIn)
            putExtra("prize_pool", tournament.prizePool)
        }
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
