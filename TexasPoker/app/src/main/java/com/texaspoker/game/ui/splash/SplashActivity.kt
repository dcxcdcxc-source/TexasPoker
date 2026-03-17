package com.texaspoker.game.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import androidx.appcompat.app.AppCompatActivity
import com.texaspoker.game.R
import com.texaspoker.game.data.GameRepository
import com.texaspoker.game.databinding.ActivitySplashBinding
import com.texaspoker.game.ui.auth.AuthActivity
import com.texaspoker.game.ui.lobby.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private lateinit var repository: GameRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = GameRepository(this)

        // Logo动画
        val fadeIn = AlphaAnimation(0f, 1f).apply { duration = 1000 }
        val scaleUp = ScaleAnimation(0.5f, 1f, 0.5f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply { duration = 1000 }
        val animSet = AnimationSet(true).apply {
            addAnimation(fadeIn)
            addAnimation(scaleUp)
        }
        binding.logoContainer.startAnimation(animSet)

        // 2.5秒后跳转
        Handler(Looper.getMainLooper()).postDelayed({
            navigateNext()
        }, 2500)
    }

    private fun navigateNext() {
        val intent = if (repository.isLoggedIn()) {
            Intent(this, MainActivity::class.java)
        } else {
            Intent(this, AuthActivity::class.java)
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
