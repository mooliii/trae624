package com.example.trae624.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.trae624.R
import com.example.trae624.TraeApp
import com.example.trae624.data.sample.SampleData
import com.example.trae624.ui.practice.PracticeActivity
import kotlinx.coroutines.launch

class CategoryDetailActivity : AppCompatActivity() {

    private var categoryId = SampleData.CATEGORY_ANALOGY
    private var categoryName = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_detail)

        categoryId = intent.getIntExtra("categoryId", SampleData.CATEGORY_ANALOGY)
        categoryName = intent.getStringExtra("categoryName") ?: ""

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = categoryName

        setupClickListeners()
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    private fun setupClickListeners() {
        // 顺序练习
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardSequential).setOnClickListener {
            startPractice("sequential")
        }

        // 随机练习
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardRandom).setOnClickListener {
            loadStatsWithTotal { total ->
                showRandomDialog(total)
            }
        }

        // 错题
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardWrong).setOnClickListener {
            startPractice("wrong")
        }

        // 收藏
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardFavorite).setOnClickListener {
            startPractice("favorite")
        }

        // 斩题
        findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardConquered).setOnClickListener {
            startPractice("conquered")
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val repo = (application as TraeApp).repository
            val allQuestions = repo.getQuestionsByCategory(categoryId)
            val answeredRecords = repo.getAnsweredRecords()
            val wrongRecords = repo.getWrongRecords()
            val favRecords = repo.getFavorites()
            val conqRecords = repo.getConqueredRecords()

            val answeredCount = allQuestions.count { q -> answeredRecords.any { it.questionId == q.id } }
            val wrongCount = allQuestions.count { q -> wrongRecords.any { it.questionId == q.id } }
            val favCount = allQuestions.count { q -> favRecords.any { it.questionId == q.id } }
            val conqCount = allQuestions.count { q -> conqRecords.any { it.questionId == q.id } }

            // 顶部统计
            findViewById<TextView>(R.id.tvTotalCount).text = "${allQuestions.size}"
            findViewById<TextView>(R.id.tvAnsweredCount).text = "$answeredCount"

            // 顺序练习进度
            findViewById<TextView>(R.id.tvSequentialProgress).text = "已刷 $answeredCount/${allQuestions.size}"

            // 我的记录数字
            findViewById<TextView>(R.id.tvWrongCount).text = "$wrongCount"
            findViewById<TextView>(R.id.tvFavCount).text = "$favCount"
            findViewById<TextView>(R.id.tvConqCount).text = "$conqCount"
        }
    }

    private fun loadStatsWithTotal(callback: (Int) -> Unit) {
        lifecycleScope.launch {
            val repo = (application as TraeApp).repository
            val allQuestions = repo.getQuestionsByCategory(categoryId)
            callback(allQuestions.size)
        }
    }

    private fun showRandomDialog(total: Int) {
        val options = arrayOf("50题", "100题", "200题", "500题")
        val counts = intArrayOf(50, 100, 200, 500)
        AlertDialog.Builder(this)
            .setTitle("选择练习题量")
            .setItems(options) { _, which ->
                startPractice("random", counts[which].coerceAtMost(total))
            }
            .show()
    }

    private fun startPractice(mode: String, count: Int = 0) {
        val intent = Intent(this, PracticeActivity::class.java)
        intent.putExtra("categoryId", categoryId)
        intent.putExtra("categoryName", categoryName)
        intent.putExtra("mode", mode)
        intent.putExtra("count", count)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
