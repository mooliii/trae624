package com.example.trae624.ui.home

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.example.trae624.R
import com.example.trae624.TraeApp
import com.example.trae624.data.model.Question
import com.example.trae624.data.sample.SampleData
import com.example.trae624.ui.practice.PracticeActivity
import com.example.trae624.ui.settings.SettingsActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "哞津刷题"

        setupBottomNav()
        setupCategories()
        loadStats()
    }

    override fun onResume() {
        super.onResume()
        loadStats()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            val layoutBank = findViewById<ScrollView>(R.id.layoutBank)
            val layoutWrong = findViewById<LinearLayout>(R.id.layoutWrong)
            val layoutFavorite = findViewById<LinearLayout>(R.id.layoutFavorite)
            val layoutConquered = findViewById<LinearLayout>(R.id.layoutConquered)

            when (item.itemId) {
                R.id.nav_bank -> {
                    layoutBank.visibility = View.VISIBLE
                    layoutWrong.visibility = View.GONE
                    layoutFavorite.visibility = View.GONE
                    layoutConquered.visibility = View.GONE
                    supportActionBar?.title = "哞津刷题"
                    true
                }
                R.id.nav_wrong -> {
                    layoutBank.visibility = View.GONE
                    layoutWrong.visibility = View.VISIBLE
                    layoutFavorite.visibility = View.GONE
                    layoutConquered.visibility = View.GONE
                    supportActionBar?.title = "哞津错题"
                    true
                }
                R.id.nav_favorite -> {
                    layoutBank.visibility = View.GONE
                    layoutWrong.visibility = View.GONE
                    layoutFavorite.visibility = View.VISIBLE
                    layoutConquered.visibility = View.GONE
                    supportActionBar?.title = "哞津收藏"
                    true
                }
                R.id.nav_conquered -> {
                    layoutBank.visibility = View.GONE
                    layoutWrong.visibility = View.GONE
                    layoutFavorite.visibility = View.GONE
                    layoutConquered.visibility = View.VISIBLE
                    supportActionBar?.title = "哞津斩题"
                    true
                }
                else -> false
            }
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnWrongPractice).setOnClickListener {
            startPractice("wrong")
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnFavPractice).setOnClickListener {
            startPractice("favorite")
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnConqPractice).setOnClickListener {
            startPractice("conquered")
        }
    }

    private fun startPractice(mode: String) {
        val intent = Intent(this, PracticeActivity::class.java)
        intent.putExtra("categoryId", SampleData.CATEGORY_ANALOGY)
        intent.putExtra("categoryName", "行测")
        intent.putExtra("mode", mode)
        intent.putExtra("count", 0)
        startActivity(intent)
    }

    private fun setupCategories() {
        val container = findViewById<LinearLayout>(R.id.categoryContainer)
        container.removeAllViews()

        val categories = listOf(
            SampleData.CATEGORY_ANALOGY to "类比推理"
        )

        lifecycleScope.launch {
            val repo = (application as TraeApp).repository
            repo.ensureDataLoaded()

            categories.forEach { (id, name) ->
                val count = repo.getQuestionsByCategory(id).size
                val itemView = layoutInflater.inflate(R.layout.item_category, container, false)
                itemView.findViewById<TextView>(R.id.tvCategoryName).text = name
                itemView.findViewById<TextView>(R.id.tvCategoryCount).text = "${count}题"
                itemView.setOnClickListener {
                    val intent = Intent(this@MainActivity, CategoryDetailActivity::class.java)
                    intent.putExtra("categoryId", id)
                    intent.putExtra("categoryName", name)
                    startActivity(intent)
                }
                container.addView(itemView)
            }
        }
    }

    private fun loadStats() {
        lifecycleScope.launch {
            val repo = (application as TraeApp).repository
            val allQuestions = repo.getQuestionsByCategory(SampleData.CATEGORY_ANALOGY)
            val wrongRecords = repo.getWrongRecords()
            val favRecords = repo.getFavorites()
            val conqRecords = repo.getConqueredRecords()

            val wrongQ = allQuestions.filter { q -> wrongRecords.any { it.questionId == q.id } }
            val favQ = allQuestions.filter { q -> favRecords.any { it.questionId == q.id } }
            val conqQ = allQuestions.filter { q -> conqRecords.any { it.questionId == q.id } }

            findViewById<TextView>(R.id.tvWrongCount).text = "${wrongQ.size} 题"
            findViewById<TextView>(R.id.tvFavCount).text = "${favQ.size} 题"
            findViewById<TextView>(R.id.tvConqCount).text = "${conqQ.size} 题"

            populateList(R.id.layoutWrongList, wrongQ, "错题", "#F44336", "wrong")
            populateList(R.id.layoutFavList, favQ, "收藏", "#FF9800", "favorite")
            populateList(R.id.layoutConqList, conqQ, "斩题", "#4CAF50", "conquered")
        }
    }

    private fun populateList(containerId: Int, questions: List<Question>, statusLabel: String, statusColor: String, mode: String) {
        val container = findViewById<LinearLayout>(containerId)
        container.removeAllViews()

        questions.forEachIndexed { index, question ->
            val itemView = layoutInflater.inflate(R.layout.item_question_list, container, false)
            itemView.findViewById<TextView>(R.id.tvIndex).text = "${index + 1}."
            itemView.findViewById<TextView>(R.id.tvStem).text = question.stem
            itemView.findViewById<TextView>(R.id.tvStatus).apply {
                text = statusLabel
                setTextColor(android.graphics.Color.parseColor(statusColor))
            }
            itemView.setOnClickListener {
                val intent = Intent(this, PracticeActivity::class.java)
                intent.putExtra("categoryId", SampleData.CATEGORY_ANALOGY)
                intent.putExtra("categoryName", "行测")
                intent.putExtra("mode", mode)
                intent.putExtra("count", 0)
                startActivity(intent)
            }
            container.addView(itemView)

            // 添加分隔线
            val divider = View(this)
            divider.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
                // height is 1px
            )
            divider.setBackgroundColor(0xFFEEEEEE.toInt())
            container.addView(divider)
        }
    }
}