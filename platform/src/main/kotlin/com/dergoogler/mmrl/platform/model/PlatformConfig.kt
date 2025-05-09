package com.dergoogler.mmrl.platform.model

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dergoogler.mmrl.platform.Platform
import com.dergoogler.mmrl.platform.TIMEOUT_MILLIS
import com.dergoogler.mmrl.platform.stub.IServiceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

interface PlatformConfig {
    var context: Context?
    var platform: Platform?
    var debug: Boolean
    var provider: IServiceManager?
    suspend fun get(
        provider: IProvider,
        timeoutMillis: Long = TIMEOUT_MILLIS,
    ): IServiceManager

    suspend fun from(
        provider: IProvider,
        timeoutMillis: Long = TIMEOUT_MILLIS,
    ): IServiceManager
}

data class PlatformConfigImpl(
    override var context: Context? = null,
    override var platform: Platform? = null,
    override var debug: Boolean = false,
    override var provider: IServiceManager? = null,
) : PlatformConfig {
    @OptIn(InternalCoroutinesApi::class)
    override suspend fun get(
        provider: IProvider,
        timeoutMillis: Long,
    ) = withTimeout(timeoutMillis) {
        suspendCancellableCoroutine<IServiceManager> { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val service = IServiceManager.Stub.asInterface(binder)
                    continuation.tryResume(service)?.let {
                        continuation.completeResume(it)
                    }
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    continuation.tryResumeWithException(IllegalStateException("IServiceManager destroyed"))
                        ?.let {
                            continuation.completeResume(it)
                        }
                }

                override fun onBindingDied(name: ComponentName?) {
                    continuation.tryResumeWithException(IllegalStateException("IServiceManager destroyed"))
                        ?.let {
                            continuation.completeResume(it)
                        }
                }
            }

            provider.bind(connection)

            continuation.invokeOnCancellation {
                provider.unbind(connection)
            }
        }
    }

    @Throws(IllegalStateException::class)
    override suspend fun from(
        provider: IProvider,
        timeoutMillis: Long,
    ): IServiceManager = withContext(Dispatchers.Main) {
        when {
            !provider.isAvailable() -> throw IllegalStateException("${provider.name} not available")
            !provider.isAuthorized() -> throw IllegalStateException("${provider.name} not authorized")
            else -> get(provider, timeoutMillis)
        }
    }
}