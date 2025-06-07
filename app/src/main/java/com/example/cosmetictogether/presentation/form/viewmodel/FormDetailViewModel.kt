package com.example.cosmetictogether.presentation.form.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.cosmetictogether.data.api.FormRetrofitInterface
import com.example.cosmetictogether.data.api.RetrofitClient
import com.example.cosmetictogether.data.model.APIResponse
import com.example.cosmetictogether.data.model.CreateOrderRequest
import com.example.cosmetictogether.data.model.CreateOrderResponse
import com.example.cosmetictogether.data.model.DetailFormResponse
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FormDetailViewModel(private val repository: FormDetailRepository) : ViewModel() {
    private val _formItem = MutableLiveData<DetailFormResponse>()
    val formItem: LiveData<DetailFormResponse> get() = _formItem

    // 각 productId별 수량과 가격을 저장하는 MutableLiveData Map
    private val _quantityMap = MutableLiveData<MutableMap<Long, Int>>() // productId를 Long으로 변경
    private val _totalPriceMap = MutableLiveData<MutableMap<Long, Int>>() // productId를 Long으로 변경

    // 각 상품별 수량과 총 가격을 개별 LiveData로 관리
    private val _quantityList = MutableLiveData<MutableList<Int>>()
    val quantityList: LiveData<MutableList<Int>> get() = _quantityList

    private val _totalPriceList = MutableLiveData<MutableList<Int>>()
    val totalPriceList: LiveData<MutableList<Int>> get() = _totalPriceList

    private val _deliveryPrice = MutableLiveData(0)
    val deliveryPrice: LiveData<Int> get() = _deliveryPrice

    private val _finalTotalPrice = MutableLiveData(0)
    val finalTotalPrice: LiveData<Int> get() = _finalTotalPrice

    private val formApi: FormRetrofitInterface =
        RetrofitClient.getInstance().create(FormRetrofitInterface::class.java)

    private val _orderResponse = MutableLiveData<CreateOrderResponse>()
    val orderResponse: LiveData<CreateOrderResponse> get() = _orderResponse

    private val _basicResponse = MutableLiveData<APIResponse>()
    val basicResponse: LiveData<APIResponse> get() = _basicResponse

    private val _isOrderInputsValid = MutableLiveData(false)
    val isOrderInputsValid: LiveData<Boolean> get() = _isOrderInputsValid

    private val _isDeliverySelected = MutableLiveData(false)
    val isDeliverySelected: LiveData<Boolean> get() = _isDeliverySelected

    private val _isProductQuantityValid = MutableLiveData(false)
    val isProductQuantityValid: LiveData<Boolean> get() = _isProductQuantityValid

    private val _selectedDeliveryOptionIndex = MutableLiveData<Int?>()
    val selectedDeliveryOptionIndex: LiveData<Int?> get() = _selectedDeliveryOptionIndex

    private val _selectedDeliveryId = MutableLiveData<Long?>()
    val selectedDeliveryId: LiveData<Long?> get() = _selectedDeliveryId

    private val _organizerId = MutableLiveData<Long>()
    val organizerId: LiveData<Long> get() = _organizerId

    private val _isFollowing = MutableLiveData<Boolean>()
    val isFollowing: LiveData<Boolean> get() = _isFollowing

    private val _instagramUrl = MutableLiveData<String>()
    val instagramUrl: LiveData<String> get() = _instagramUrl

    private val _deleteStatus = MutableLiveData<Boolean>()
    val deleteStatus: LiveData<Boolean> get() = _deleteStatus

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> get() = _errorMessage

    init {
        _quantityMap.value = mutableMapOf()
        _totalPriceMap.value = mutableMapOf()
    }

    // 버튼 활성화 여부를 갱신
    private fun validateButtonState() {
        val allValid = (_isOrderInputsValid.value == true
                && _isDeliverySelected.value == true
                && _isProductQuantityValid.value == true)
        (isPurchaseButtonEnabled as MediatorLiveData).value = allValid
    }

    fun setOrganizerId(id: Long) {
        _organizerId.value = id
    }

    fun updateOrderInputsValid(isValid: Boolean) {
        _isOrderInputsValid.value = isValid
    }

    fun updateDeliverySelection(isSelected: Boolean) {
        _isDeliverySelected.value = isSelected
    }

    fun updateProductQuantityValidity(isValid: Boolean) {
        _isProductQuantityValid.value = isValid
    }

    // 버튼 활성화 여부를 결정하는 LiveData
    val isPurchaseButtonEnabled: LiveData<Boolean> = MediatorLiveData<Boolean>().apply {
        addSource(isOrderInputsValid) { validateButtonState() }
        addSource(isDeliverySelected) { validateButtonState() }
        addSource(isProductQuantityValid) { validateButtonState() }
    }

    // 폼 세부 정보 조회
    fun getFormDetail(formId: Long, token: String) {
        formApi.getFormDetail(token, formId).enqueue(object : Callback<DetailFormResponse> {
            override fun onResponse(call: Call<DetailFormResponse>, response: Response<DetailFormResponse>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        _formItem.value = data
                        // 여기에서 products의 개수로 initializeProducts 호출
                        _instagramUrl.value = "https://www.instagram.com/${formItem.value?.instagram}"
                        Log.d("인스타그램", _instagramUrl.value!!)
                        initializeProducts(data.products.size)
                        setOrganizerId(data.organizerId)
                    }
                }
            }

            override fun onFailure(call: Call<DetailFormResponse>, t: Throwable) {
                Log.e("getFormDetail", "Error: ${t.message}")
            }
        })
    }

    fun initializeProducts(productCount: Int) {
        _quantityList.value = MutableList(productCount) { 0 }
        _totalPriceList.value = MutableList(productCount) { 0 }
        calculateFinalTotalPrice() // 초기 결제 금액 계산
    }

    // 수량 변경 시 최종 결제 금액을 업데이트
    fun increaseQuantity(productId: Long, maxQuantity: Int, price: Int) {
        val quantityMap = _quantityMap.value ?: mutableMapOf()
        val currentQuantity = quantityMap[productId] ?: 0
        if (currentQuantity < maxQuantity) {
            quantityMap[productId] = currentQuantity + 1
            _quantityMap.value = quantityMap
            calculateFinalTotalPrice()  // 변경된 수량을 반영해 총 결제 금액 업데이트
        }
    }

    fun decreaseQuantity(productId: Long, price: Int) {
        val quantityMap = _quantityMap.value ?: mutableMapOf()
        val currentQuantity = quantityMap[productId] ?: 0
        if (currentQuantity > 0) {
            quantityMap[productId] = currentQuantity - 1
            _quantityMap.value = quantityMap
            calculateFinalTotalPrice()  // 변경된 수량을 반영해 총 결제 금액 업데이트
        }
    }

    // 배송 금액 설정 시 호출
    fun setDeliveryPrice(price: Int) {
        _deliveryPrice.value = price
        calculateFinalTotalPrice()  // 배송 금액 설정 시 최종 결제 금액 업데이트
    }

    // 라디오 버튼에 배송 ID 연결
    fun setSelectedDeliveryId(deliveryId: Long) {
        _selectedDeliveryId.value = deliveryId
    }

    // 수량에 대한 LiveData 제공 메서드
    fun getQuantityLiveData(productId: Long): LiveData<Int> {
        val liveData = MutableLiveData<Int>()
        _quantityMap.observeForever { map ->
            liveData.value = map[productId] ?: 0
        }
        return liveData
    }

    // 수량 확인
    fun getSelectedProducts(): Pair<List<Long>, List<Int>> {
        val quantityMap = _quantityMap.value ?: return Pair(emptyList(), emptyList())
        val products = formItem.value?.products ?: return Pair(emptyList(), emptyList())

        // 수량이 0보다 큰 상품만 필터링하여 productsId와 orderQuantity 리스트 생성
        val productsId = mutableListOf<Long>()
        val orderQuantity = mutableListOf<Int>()

        products.forEach { product ->
            val quantity = quantityMap[product.productId] ?: 0
            if (quantity > 0) {
                productsId.add(product.productId)
                orderQuantity.add(quantity)
            }
        }

        return Pair(productsId, orderQuantity)
    }

    // 주문
    fun createOrder(context: Context, formId: Long, token: String, orderRequest: CreateOrderRequest) {
        formApi.createOrder(formId, token, orderRequest).enqueue(object : Callback<CreateOrderResponse> {
            override fun onResponse(call: Call<CreateOrderResponse>, response: Response<CreateOrderResponse>) {
                if (response.isSuccessful) {
                    _orderResponse.value = response.body()

                } else {
                    val errorMessage = parseErrorMessage(response)
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CreateOrderResponse>, t: Throwable) {
                // 네트워크 오류 처리
            }
        })
    }

    private fun calculateProductTotalPrice(): Int {
        val quantityMap = _quantityMap.value ?: return 0
        val products = formItem.value?.products ?: return 0
        return products.sumOf { product ->
            val quantity = quantityMap[product.productId] ?: 0
            product.price * quantity
        }
    }

    private fun calculateFinalTotalPrice() {
        val productTotal = calculateProductTotalPrice()  // 모든 상품의 총 합계를 계산
        val deliveryCost = _deliveryPrice.value ?: 0
        _finalTotalPrice.value = productTotal + deliveryCost  // 배송 금액과 상품 합계를 더함
    }

    private fun parseErrorMessage(response: Response<*>): String {
        return try {
            val errorBody = response.errorBody()?.string() ?: return "알 수 없는 오류가 발생했습니다."
            val errorJson = JSONObject(errorBody)
            errorJson.getString("message") // 서버에서 보낸 메시지
        } catch (e: Exception) {
            "오류를 처리할 수 없습니다."
        }
    }

    // 찜
    fun toggleFavorite(formId: Long, token: String) {
        viewModelScope.launch {
            val success = repository.toggleFavorite(formId, token)
            if(success) {
                getFormDetail(formId, token)
            }
        }
    }

    fun toggleFollow(formId: Long, token: String) {
        viewModelScope.launch {
            val success = repository.toggleFollow(formId, token)
            if(success) {
                getFormDetail(formId, token)
            }
        }
    }

    fun deleteForm(token: String, formId: Long) {
        viewModelScope.launch {
            try {
                val success = repository.deletePost(token, formId)
                _deleteStatus.value = success
                if (!success) {
                    _errorMessage.value = "게시글 삭제에 실패했습니다."
                }
            } catch (e: Exception) {
                _deleteStatus.value = false
                _errorMessage.value = e.message ?: "게시글 삭제 중 오류가 발생했습니다."
            }
        }
    }

}