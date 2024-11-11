package com.example.cosmetictogether.presentation.form.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.cosmetictogether.data.api.FormRetrofitInterface
import com.example.cosmetictogether.data.api.RetrofitClient
import com.example.cosmetictogether.data.model.FormSummaryResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FormViewModel: ViewModel() {

    private val _formItem = MutableLiveData<FormSummaryResponse>()
    val formItem: LiveData<FormSummaryResponse> get() = _formItem

    private val _thumbnail = MutableLiveData<String>()
    val thumbnail: LiveData<String> get() = _thumbnail

    private val _title = MutableLiveData<String>()
    val title: LiveData<String> get() = _title

    private val _organizerName = MutableLiveData<String>()
    val organizerName: LiveData<String> get() = _organizerName

    private val _organizer_url = MutableLiveData<String>()
    val organizer_url: LiveData<String> get() = _organizer_url

    private val _formStatus = MutableLiveData<String>()
    val formStatus: LiveData<String> get() = _formStatus

    // 추가: FormSummaryResponse 리스트
    private val _formData = MutableLiveData<List<FormSummaryResponse>>()
    val formData: LiveData<List<FormSummaryResponse>> get() = _formData


    private val formApi: FormRetrofitInterface =
        RetrofitClient.getInstance().create(FormRetrofitInterface::class.java)

    fun loadFormSummaryData() {
        formApi.getFormRecent().enqueue(object : Callback<List<FormSummaryResponse>> {
            override fun onResponse(call: Call<List<FormSummaryResponse>>, response: Response<List<FormSummaryResponse>>) {
                if (response.isSuccessful) {
                    response.body()?.let { data ->
                        if (data.isNotEmpty()) {
                            // _formData에 데이터 목록 설정
                            _formData.value = data

                            // 첫 번째 항목을 _formItem에 설정 (필요한 경우)
                            _formItem.value = data[0]
                            _thumbnail.value = data[0].thumbnail
                            _title.value = data[0].title
                            _organizer_url.value = data[0].organizer_url
                            _formStatus.value = data[0].formStatus
                        }
                    }
                }
            }

            override fun onFailure(call: Call<List<FormSummaryResponse>>, t: Throwable) {
                // 실패 시 처리 로직 (예: 로그 출력)
                Log.e("LoadFormSummaryData", "Error: ${t.message}")
            }
        })
    }
}