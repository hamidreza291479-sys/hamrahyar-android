package com.lucifer.hamrahyar.ui.theme.domain.usecase

import com.lucifer.hamrahyar.ui.theme.domain.model.ActiveService
import com.lucifer.hamrahyar.ui.theme.domain.model.PriorityLevel
import com.lucifer.hamrahyar.ui.theme.domain.repository.OnlineServiceRepository
import kotlinx.coroutines.delay

class CreateOrderUseCase(private val repository: OnlineServiceRepository) {

    suspend operator fun invoke(
        serviceId: String,
        priority: PriorityLevel,
        formData: String?,
        clientRequestId: String
    ): Result<ActiveService> {
        var currentRequestId = clientRequestId
        var attempts = 0
        val maxAttempts = 3

        while (attempts < maxAttempts) {
            val result = repository.createOrder(
                serviceId = serviceId,
                priority = priority,
                formData = formData,
                clientRequestId = currentRequestId
            )

            if (result.isSuccess) {
                return result
            }

            val exception = result.exceptionOrNull()
            val message = exception?.message ?: ""

            // Idempotent retries for network/timeout errors
            if (message.contains("timeout") || message.contains("اتصال") || message.contains("server")) {
                attempts++
                if (attempts < maxAttempts) {
                    delay(2000L * attempts) // Exponential backoff
                    continue
                }
            }

            // If it's a domain error (like ACTIVE_ORDER_EXISTS), don't retry, just return it
            return result
        }

        return Result.failure(Exception("خطا در برقراری ارتباط با سرور. لطفا دوباره تلاش کنید."))
    }
}
