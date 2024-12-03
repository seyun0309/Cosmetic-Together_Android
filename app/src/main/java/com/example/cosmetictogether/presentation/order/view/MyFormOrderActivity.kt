package com.example.cosmetictogether.presentation.order.view

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cosmetictogether.R
import com.example.cosmetictogether.databinding.ActivityMyformOrderBinding
import com.example.cosmetictogether.presentation.form.view.FormActivity
import com.example.cosmetictogether.presentation.form.view.MyFormActivity
import com.example.cosmetictogether.presentation.mypage.view.MyPageActivity
import com.example.cosmetictogether.presentation.order.adapter.OrderListAdapter
import com.example.cosmetictogether.presentation.order.viewmodel.MyFormOrderViewModel

class MyFormOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyformOrderBinding
    private val viewModel: MyFormOrderViewModel by viewModels()

    private lateinit var orderListAdapter: OrderListAdapter// 어댑터 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMyformOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        orderListAdapter = OrderListAdapter(
            emptyList(),
            onOrderDetailClick = { orderId, formId -> navigateToOrderList(orderId, formId) }, // 기존 btnFormDetail 로직
        )

        binding.formRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.formRecyclerView.adapter = orderListAdapter

        // ViewModel 관찰
        viewModel.orders.observe(this) { orders ->
            orderListAdapter.updateData(orders) // RecyclerView 데이터 업데이트
        }

        val formId = intent.getLongExtra("formId", -1)
        if (formId != -1L) {
            viewModel.loadOrderList(formId) // API 호출
        } else {
            Toast.makeText(this, "폼 ID를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
        }

        binding.backBtn.setOnClickListener {
            val intent = Intent(this, MyFormActivity::class.java)
            startActivity(intent)
        }

        // BottomNavigationView의 항목 클릭 이벤트 설정
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.action_post -> {
                    // PostActivity로 이동할 경우 구현
                    true
                }
                R.id.action_form -> {
                    val intent = Intent(this, FormActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.action_home -> {
                    // HomeActivity로 이동할 경우 구현
                    true
                }
                R.id.action_alarm -> {
                    // AlarmActivity로 이동할 경우 구현
                    true
                }
                R.id.action_mypage -> {
                    // MyPageActivity로 이동
                    val intent = Intent(this, MyPageActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateToOrderList(orderId: Long, formId: Long) {
        val intent = Intent(this, MyFormOrderDetailActivity::class.java)
        intent.putExtra("orderId", orderId)
        intent.putExtra("formId", formId)
        startActivity(intent)
    }
}