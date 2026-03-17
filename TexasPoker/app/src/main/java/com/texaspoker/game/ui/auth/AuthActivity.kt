package com.texaspoker.game.ui.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.texaspoker.game.data.GameRepository
import com.texaspoker.game.databinding.ActivityAuthBinding
import com.texaspoker.game.ui.lobby.MainActivity

class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private lateinit var repository: GameRepository
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)
        repository = GameRepository(this)

        setupUI()
    }

    private fun setupUI() {
        binding.btnToggleMode.setOnClickListener {
            isLoginMode = !isLoginMode
            updateMode()
        }

        binding.btnSubmit.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            if (username.isEmpty()) {
                showToast("请输入用户名")
                return@setOnClickListener
            }
            handleAuth(username)
        }

        binding.btnGuest.setOnClickListener {
            // 访客登录
            val guestName = "访客${(1000..9999).random()}"
            loginUser(guestName)
        }

        updateMode()
    }

    private fun updateMode() {
        if (isLoginMode) {
            binding.tvTitle.text = "登录"
            binding.btnSubmit.text = "登录"
            binding.btnToggleMode.text = "没有账号？立即注册"
            binding.layoutConfirmPassword.visibility = View.GONE
        } else {
            binding.tvTitle.text = "注册"
            binding.btnSubmit.text = "注册"
            binding.btnToggleMode.text = "已有账号？立即登录"
            binding.layoutConfirmPassword.visibility = View.VISIBLE
        }
    }

    private fun handleAuth(username: String) {
        if (isLoginMode) {
            // 简化登录逻辑（本地模式）
            loginUser(username)
        } else {
            // 注册
            if (username.length < 2) {
                showToast("用户名至少2个字符")
                return
            }
            loginUser(username)
        }
    }

    private fun loginUser(username: String) {
        repository.getOrCreateUser(username)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
