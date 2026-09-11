package com.lucifer.hamrahyar.ui.theme.service

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lucifer.hamrahyar.ui.theme.domain.model.ActiveService
import com.lucifer.hamrahyar.ui.theme.domain.model.PriorityLevel
import com.lucifer.hamrahyar.ui.theme.domain.usecase.CreateOrderUseCase
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import com.lucifer.hamrahyar.ui.theme.data.model.FormFieldError
import com.lucifer.hamrahyar.ui.theme.data.model.InitialFormIncompleteException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class OrderRegistrationState {
    object Idle : OrderRegistrationState()
    object Submitting : OrderRegistrationState()
    data class Success(val activeService: ActiveService) : OrderRegistrationState()
    data class ActiveOrderExists(val activeService: ActiveService) : OrderRegistrationState()
    data class ValidationError(val errors: List<FormFieldError>) : OrderRegistrationState()
    data class Error(val message: String) : OrderRegistrationState()
}

class OrderViewModel(
    private val createOrderUseCase: CreateOrderUseCase,
    private val repository: OnlineServiceRepository
) : ViewModel() {

    private val _registrationState = MutableStateFlow<OrderRegistrationState>(OrderRegistrationState.Idle)
    val registrationState: StateFlow<OrderRegistrationState> = _registrationState.asStateFlow()

    private var currentClientRequestId: String? = null

    fun submitOrder(serviceId: String, priority: PriorityLevel, formData: String?) {
        if (_registrationState.value is OrderRegistrationState.Submitting) return

        viewModelScope.launch {
            _registrationState.value = OrderRegistrationState.Submitting
            
            val requestId = currentClientRequestId ?: UUID.randomUUID().toString().also { 
                currentClientRequestId = it 
            }

            createOrderUseCase(serviceId, priority, formData, requestId)
                .onSuccess { activeService ->
                    currentClientRequestId = null // Clear on success
                    _registrationState.value = OrderRegistrationState.Success(activeService)
                }
                .onFailure { exception ->
                    when {
                        exception is InitialFormIncompleteException -> {
                            _registrationState.value = OrderRegistrationState.ValidationError(exception.errors)
                        }
                        exception.message?.contains("ACTIVE_ORDER_EXISTS") == true || exception.message?.contains("سفارش فعال") == true -> {
                            // The repository might have failed because it couldn't decode the data,
                            // but we know an active order exists. We can't easily recover it here 
                            // without the phone, but the UI can trigger recovery or we can try to restore.
                            _registrationState.value = OrderRegistrationState.Error("شما در حال حاضر یک سفارش فعال دارید.")
                        }
                        else -> {
                            _registrationState.value = OrderRegistrationState.Error(exception.message ?: "خطای ناشناخته")
                        }
                    }
                }
        }
    }

    fun resetState() {
        _registrationState.value = OrderRegistrationState.Idle
    }
    
    fun checkForActiveOrder(guestKey: String) {
        if (_registrationState.value is OrderRegistrationState.Submitting) return
        
        viewModelScope.launch {
            _registrationState.value = OrderRegistrationState.Submitting
            repository.getMyActiveOrder(guestKey)
                .onSuccess { activeOrder ->
                    if (activeOrder != null) {
                        _registrationState.value = OrderRegistrationState.ActiveOrderExists(activeOrder)
                    } else {
                        _registrationState.value = OrderRegistrationState.Idle
                    }
                }
                .onFailure { 
                    _registrationState.value = OrderRegistrationState.Idle 
                }
        }
    }

    fun recoverOrder(phone: String) {
        // This is now legacy but we'll keep it as a fallback if needed
        viewModelScope.launch {
            _registrationState.value = OrderRegistrationState.Submitting
            repository.getActiveOrderByPhone(phone)
                .onSuccess { activeOrder ->
                    if (activeOrder != null) {
                        _registrationState.value = OrderRegistrationState.ActiveOrderExists(activeOrder)
                    } else {
                        _registrationState.value = OrderRegistrationState.Error("سفارش فعالی یافت نشد.")
                    }
                }
                .onFailure { 
                    _registrationState.value = OrderRegistrationState.Error("خطا در بازیابی سفارش.")
                }
        }
    }
}

class OrderViewModelFactory(
    private val createOrderUseCase: CreateOrderUseCase,
    private val repository: OnlineServiceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return OrderViewModel(createOrderUseCase, repository) as T
    }
}
