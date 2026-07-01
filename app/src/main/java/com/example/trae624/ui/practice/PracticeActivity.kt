package com.example.trae624.ui.practice

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.trae624.R
import com.example.trae624.TraeApp
import com.example.trae624.data.model.PracticeRecord
import com.example.trae624.data.sample.SampleData
import com.example.trae624.ui.settings.SettingsActivity
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

class PracticeActivity : AppCompatActivity() {

    private lateinit var viewModel: PracticeViewModel
    private lateinit var tvStem: TextView
    private lateinit var tvQuestionIndex: TextView
    private lateinit var btnOptionA: Button
    private lateinit var btnOptionB: Button
    private lateinit var btnOptionC: Button
    private lateinit var btnOptionD: Button
    private lateinit var tvAnalysis: TextView
    private lateinit var layoutAnalysis: LinearLayout
    private lateinit var btnFavorite: ImageButton
    private lateinit var tvCorrectCount: TextView
    private lateinit var tvWrongCount: TextView
    private lateinit var tvProgress: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var btnMemorize: ImageButton
    private lateinit var btnAutoAdvance: ImageButton
    private lateinit var btnPrev: com.google.android.material.button.MaterialButton
    private lateinit var btnNext: com.google.android.material.button.MaterialButton
    private lateinit var gestureDetector: GestureDetector

    private var lastAutoAdvancedIndex = -1
    private var isSwiping = false
    private var touchStartX = 0f
    private var touchStartY = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_practice)

        viewModel = ViewModelProvider(this).get(PracticeViewModel::class.java)

        initViews()
        setupToolbar()
        setupListeners()
        setupSwipeGesture()
        loadQuestions()
        observeState()
    }

    private fun initViews() {
        tvStem = findViewById(R.id.tvStem)
        tvQuestionIndex = findViewById(R.id.tvQuestionIndex)
        btnOptionA = findViewById(R.id.btnOptionA)
        btnOptionB = findViewById(R.id.btnOptionB)
        btnOptionC = findViewById(R.id.btnOptionC)
        btnOptionD = findViewById(R.id.btnOptionD)
        tvAnalysis = findViewById(R.id.tvAnalysis)
        layoutAnalysis = findViewById(R.id.layoutAnalysis)
        btnFavorite = findViewById(R.id.btnFavorite)
        tvCorrectCount = findViewById(R.id.tvCorrectCount)
        tvWrongCount = findViewById(R.id.tvWrongCount)
        tvProgress = findViewById(R.id.tvProgress)
        scrollView = findViewById(R.id.scrollView)
        btnMemorize = findViewById(R.id.btnMemorize)
        btnAutoAdvance = findViewById(R.id.btnAutoAdvance)
        btnPrev = findViewById(R.id.btnPrev)
        btnNext = findViewById(R.id.btnNext)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val categoryName = intent.getStringExtra("categoryName") ?: "刷题"
        supportActionBar?.title = categoryName
    }

    private fun setupListeners() {
        btnOptionA.setOnClickListener { onOptionClick("A") }
        btnOptionB.setOnClickListener { onOptionClick("B") }
        btnOptionC.setOnClickListener { onOptionClick("C") }
        btnOptionD.setOnClickListener { onOptionClick("D") }

        btnFavorite.setOnClickListener { viewModel.toggleFavorite() }

        tvProgress.setOnClickListener { showProgressDetail() }

        btnAutoAdvance.setOnClickListener {
            val state = viewModel.uiState.value ?: return@setOnClickListener
            viewModel.setAutoAdvance(!state.autoAdvance)
            updateToggleStates(state.copy(autoAdvance = !state.autoAdvance))
        }

        btnMemorize.setOnClickListener {
            viewModel.toggleMemorizeMode()
        }

        btnPrev.setOnClickListener { viewModel.prevQuestion() }
        btnNext.setOnClickListener { viewModel.nextQuestion() }
    }

    private fun setupSwipeGesture() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?, e2: MotionEvent,
                velocityX: Float, velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                // 只在水平滑动明显大于垂直滑动时触发
                if (Math.abs(dx) > Math.abs(dy) * 2 && Math.abs(dx) > 100) {
                    isSwiping = true
                    if (dx > 0) {
                        viewModel.prevQuestion()
                    } else {
                        viewModel.nextQuestion()
                    }
                    return true
                }
                return false
            }
        })

        // ScrollView：左右滑动切换题目，滑动时禁止垂直滚动
        scrollView.setOnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    touchStartX = event.x
                    touchStartY = event.y
                    false // 让ScrollView正常处理
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.x - touchStartX
                    val dy = event.y - touchStartY
                    // 水平滑动明显时，消费事件阻止ScrollView垂直滚动
                    if (Math.abs(dx) > Math.abs(dy) * 2 && Math.abs(dx) > 50) {
                        true
                    } else {
                        false
                    }
                }
                else -> false
            }
        }

        // 选项按钮区域：也支持左右滑动切换题目，但滑动时不触发选项选中
        val optionTouchListener = View.OnTouchListener { v, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_UP) {
                if (isSwiping) {
                    isSwiping = false
                    return@OnTouchListener true // 消费事件，阻止按钮的点击触发
                }
            }
            false // 非滑动操作，正常处理点击
        }
        btnOptionA.setOnTouchListener(optionTouchListener)
        btnOptionB.setOnTouchListener(optionTouchListener)
        btnOptionC.setOnTouchListener(optionTouchListener)
        btnOptionD.setOnTouchListener(optionTouchListener)
    }

    private fun loadQuestions() {
        val categoryId = intent.getIntExtra("categoryId", SampleData.CATEGORY_ANALOGY)
        val mode = intent.getStringExtra("mode") ?: "sequential"
        val count = intent.getIntExtra("count", 0)

        lifecycleScope.launch {
            val repo = (application as TraeApp).repository
            repo.ensureDataLoaded()
            val allQuestions = repo.getQuestionsByCategory(categoryId)

            val questions = when (mode) {
                "random" -> allQuestions.shuffled().take(count.coerceAtLeast(1).coerceAtMost(allQuestions.size))
                "wrong" -> {
                    val wrongRecords = repo.getWrongRecords()
                    val wrongIds = wrongRecords.map { it.questionId }.toSet()
                    allQuestions.filter { it.id in wrongIds }
                }
                "favorite" -> {
                    val favRecords = repo.getFavorites()
                    val favIds = favRecords.map { it.questionId }.toSet()
                    allQuestions.filter { it.id in favIds }
                }
                "conquered" -> {
                    val conqRecords = repo.getConqueredRecords()
                    val conqIds = conqRecords.map { it.questionId }.toSet()
                    allQuestions.filter { it.id in conqIds }
                }
                else -> allQuestions
            }

            if (questions.isEmpty()) {
                runOnUiThread {
                    AlertDialog.Builder(this@PracticeActivity)
                        .setTitle("提示")
                        .setMessage("暂无题目")
                        .setPositiveButton("返回") { _, _ -> finish() }
                        .show()
                }
                return@launch
            }

            val startIndex = if (mode == "sequential") {
                getSharedPreferences("practice_progress", MODE_PRIVATE)
                    .getInt("progress_${categoryId}", 0)
            } else 0

            runOnUiThread {
                viewModel.loadQuestions(questions, startIndex.coerceAtMost(questions.size - 1))
            }
        }
    }

    private fun onOptionClick(answer: String) {
        viewModel.selectAnswer(answer)
    }

    private fun updateToggleStates(state: PracticeUiState) {
        // 自动跳题：selector根据isSelected切换图标
        btnAutoAdvance.isSelected = state.autoAdvance

        // 背题：选中时高亮（黄色tint），未选中时半透明
        val inactiveColor = (0x80FFFFFF).toInt()
        val memorizeActive = ContextCompat.getColor(this, android.R.color.holo_orange_light)
        btnMemorize.setColorFilter(if (state.isMemorizeMode) memorizeActive else inactiveColor)
        btnMemorize.isSelected = state.isMemorizeMode
    }

    private fun observeState() {
        viewModel.uiState.observe(this) { state ->
            val questions = state.questions
            if (questions.isEmpty()) return@observe
            val index = state.currentIndex
            val question = questions[index]

            // 题号（小字号、淡色，换行显示）
            tvQuestionIndex.text = "第 ${index + 1} 题 / 共 ${questions.size} 题"

            // 题干（加黑加粗）
            tvStem.text = question.stem

            // 选项
            btnOptionA.text = "A. ${question.optionA}"
            btnOptionB.text = "B. ${question.optionB}"
            btnOptionC.text = "C. ${question.optionC}"
            btnOptionD.text = "D. ${question.optionD}"

            // 背题模式下禁用选项
            val optionsEnabled = !state.isMemorizeMode && !state.showResult
            btnOptionA.isEnabled = optionsEnabled
            btnOptionB.isEnabled = optionsEnabled
            btnOptionC.isEnabled = optionsEnabled
            btnOptionD.isEnabled = optionsEnabled

            val showAnswer = state.isMemorizeMode || state.showResult

            if (showAnswer) {
                highlightOptions(question.correctAnswer, state.selectedAnswer)
                tvAnalysis.text = question.analysis
                layoutAnalysis.visibility = View.VISIBLE
            } else {
                resetOptionStyles()
                layoutAnalysis.visibility = View.GONE
            }

            // 收藏
            btnFavorite.setImageResource(
                if (state.isFavorite) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )

            // 底栏
            tvCorrectCount.text = "\u2713 ${state.correctCount}"
            tvWrongCount.text = "\u2717 ${state.wrongCount}"
            tvProgress.text = "${state.answeredCount}/${questions.size}"

            // 上一题/下一题按钮状态
            btnPrev.isEnabled = index > 0
            btnNext.isEnabled = index < questions.size - 1

            // 图标选中态
            updateToggleStates(state)

            // 自动跳题
            val isCorrect = state.selectedAnswer == question.correctAnswer && state.showResult
            if (isCorrect && state.autoAdvance && index != lastAutoAdvancedIndex) {
                lastAutoAdvancedIndex = index
                Handler(Looper.getMainLooper()).postDelayed({
                    if (index < questions.size - 1) {
                        viewModel.nextQuestion()
                    }
                }, 400)
            }

            // 滚动到顶部
            scrollView.post { scrollView.scrollTo(0, 0) }

            // 保存顺序练习进度
            saveProgress(index)
        }
    }

    private fun highlightOptions(correctAnswer: String?, selectedAnswer: String?) {
        val correctColor = ContextCompat.getColor(this, android.R.color.holo_green_dark)
        val wrongColor = ContextCompat.getColor(this, android.R.color.holo_red_dark)
        val defaultColor = ContextCompat.getColor(this, android.R.color.black)

        val optionMap = mapOf(
            "A" to btnOptionA, "B" to btnOptionB,
            "C" to btnOptionC, "D" to btnOptionD
        )

        optionMap.forEach { (key, btn) ->
            when {
                key == correctAnswer -> {
                    btn.setTextColor(correctColor)
                }
                key == selectedAnswer && selectedAnswer != correctAnswer -> {
                    btn.setTextColor(wrongColor)
                }
                else -> {
                    btn.setTextColor(defaultColor)
                }
            }
        }
    }

    private fun resetOptionStyles() {
        val defaultBg = ContextCompat.getDrawable(this, R.drawable.bg_option_default)
        val defaultColor = ContextCompat.getColor(this, android.R.color.black)
        listOf(btnOptionA, btnOptionB, btnOptionC, btnOptionD).forEach { btn ->
            btn.background = defaultBg?.constantState?.newDrawable()?.mutate()
            btn.setTextColor(defaultColor)
        }
    }

    private fun showProgressDetail() {
        val state = viewModel.uiState.value ?: return
        val questions = state.questions

        lifecycleScope.launch {
            val repo = (application as TraeApp).repository
            val allRecords = repo.getAnsweredRecords()
            val recordMap = allRecords.associateBy { it.questionId }

            runOnUiThread {
                val dialogView = layoutInflater.inflate(R.layout.dialog_progress, null)
                val container = dialogView.findViewById<LinearLayout>(R.id.progressContainer)
                container.removeAllViews()

                var rowLayout: LinearLayout? = null
                questions.forEachIndexed { index, question ->
                    if (index % 5 == 0) {
                        rowLayout = LinearLayout(this@PracticeActivity).apply {
                            orientation = LinearLayout.HORIZONTAL
                            layoutParams = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                            )
                            gravity = Gravity.CENTER
                        }
                        container.addView(rowLayout)
                    }

                    val record = recordMap[question.id]
                    val bgColor = when {
                        record == null -> R.drawable.bg_circle_default
                        record.isCorrect -> R.drawable.bg_circle_correct
                        else -> R.drawable.bg_circle_wrong
                    }

                    val tv = TextView(this@PracticeActivity).apply {
                        text = "${index + 1}"
                        textSize = 14f
                        gravity = Gravity.CENTER
                        setBackgroundResource(bgColor)
                        setTextColor(ContextCompat.getColor(this@PracticeActivity, android.R.color.white))
                        val size = (40 * resources.displayMetrics.density).toInt()
                        layoutParams = LinearLayout.LayoutParams(size, size).apply {
                            setMargins(8, 8, 8, 8)
                        }
                        // 存储当前index供点击使用
                        tag = index
                    }
                    rowLayout?.addView(tv)
                }

                val dialog = AlertDialog.Builder(this@PracticeActivity)
                    .setTitle("做题进度")
                    .setView(dialogView)
                    .setPositiveButton("关闭", null)
                    .setNeutralButton("重置") { _, _ ->
                        lifecycleScope.launch {
                            val repo2 = (application as TraeApp).repository
                            repo2.deleteAllRecords()
                            viewModel.loadQuestions(state.questions, 0)
                        }
                    }
                    .create()

                // 给每个题号按钮添加点击事件，点击后关闭弹框
                for (i in 0 until container.childCount) {
                    val row = container.getChildAt(i) as? LinearLayout ?: continue
                    for (j in 0 until row.childCount) {
                        val tv = row.getChildAt(j) as? TextView ?: continue
                        val idx = tv.tag as? Int ?: continue
                        tv.setOnClickListener {
                            viewModel.jumpToQuestion(idx)
                            dialog.dismiss()
                        }
                    }
                }

                dialog.show()
            }
        }
    }

    private fun saveProgress(index: Int) {
        val categoryId = intent.getIntExtra("categoryId", SampleData.CATEGORY_ANALOGY)
        val mode = intent.getStringExtra("mode") ?: "sequential"
        if (mode == "sequential") {
            getSharedPreferences("practice_progress", MODE_PRIVATE)
                .edit().putInt("progress_${categoryId}", index).apply()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
