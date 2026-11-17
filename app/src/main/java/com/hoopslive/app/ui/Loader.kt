package com.hoopslive.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.UnknownHostException

sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Ok<out T>(val data: T) : Load<T>
    data class Err(val message: String) : Load<Nothing>
}

/**
 * One loader per screen section. Survives configuration changes (it lives in the
 * navigation back-stack entry), polls while live data is on screen, and pauses polling
 * whenever the screen is not started (backgrounded app or another screen on top).
 */
class LoaderViewModel : ViewModel() {
    val state = MutableStateFlow<Load<Any?>>(Load.Loading)
    val refreshing = MutableStateFlow(false)
    private val active = MutableStateFlow(true)
    private var fetch: (suspend (Boolean) -> Any?)? = null
    private var poll: ((Any?) -> Long?)? = null
    private var job: Job? = null

    fun bind(fetch: suspend (Boolean) -> Any?, poll: (Any?) -> Long?) {
        this.fetch = fetch
        this.poll = poll
        if (job == null) start(false)
    }

    fun refresh() {
        start(true)
    }

    fun setActive(value: Boolean) {
        active.value = value
    }

    private fun start(force: Boolean) {
        val f = fetch ?: return
        job?.cancel()
        job = viewModelScope.launch {
            var forceNext = force
            while (true) {
                if (state.value is Load.Ok<*>) refreshing.value = true else state.value = Load.Loading
                try {
                    state.value = Load.Ok(withContext(Dispatchers.Default) { f(forceNext) })
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (state.value !is Load.Ok<*>) state.value = Load.Err(friendly(e))
                } finally {
                    refreshing.value = false
                }
                val ok = state.value as? Load.Ok<*> ?: return@launch
                val ms = poll?.invoke(ok.data) ?: return@launch
                delay(ms)
                active.first { it }
                forceNext = true
            }
        }
    }

    private fun friendly(e: Exception): String = when (e) {
        is UnknownHostException -> "No internet connection"
        is IOException -> e.message ?: "Network error"
        else -> e.message ?: "Something went wrong"
    }
}

class LoadHandle<T>(val state: Load<T>, val refreshing: Boolean, val refresh: () -> Unit)

@Composable
fun <T> rememberLoad(
    key: String,
    poll: (T) -> Long? = { null },
    fetch: suspend (force: Boolean) -> T,
): LoadHandle<T> {
    val vm: LoaderViewModel = viewModel(key = key)
    @Suppress("UNCHECKED_CAST")
    vm.bind(fetch) { data -> poll(data as T) }
    LifecycleStartEffect(vm) {
        vm.setActive(true)
        onStopOrDispose { vm.setActive(false) }
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val refreshing by vm.refreshing.collectAsStateWithLifecycle()
    @Suppress("UNCHECKED_CAST")
    return LoadHandle(state as Load<T>, refreshing) { vm.refresh() }
}
