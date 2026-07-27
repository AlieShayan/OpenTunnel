package dev.opentunnel.vpn.core

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

enum class PromptFieldType { TEXT, PASSWORD, SELECT }

@Immutable
data class PromptChoice(val value: String, val label: String)

@Immutable
data class PromptField(
    val name: String,
    val label: String,
    val type: PromptFieldType,
    val prefill: String = "",
    val choices: List<PromptChoice> = emptyList(),
)

/**
 * Something the tunnel thread needs a human to answer before it can continue.
 */
@Immutable
sealed interface UserPrompt {
    val id: Long

    /** An auth form the profile could not satisfy on its own (OTP, group, …). */
    @Immutable
    data class Auth(
        override val id: Long,
        val title: String,
        val banner: String? = null,
        val message: String? = null,
        val error: String? = null,
        val fields: List<PromptField>,
    ) : UserPrompt

    /** The server certificate is not trusted, and not the one we pinned. */
    @Immutable
    data class CertTrust(
        override val id: Long,
        val host: String,
        val reason: String,
        val fingerprint: String,
        val details: String,
    ) : UserPrompt
}

sealed interface PromptResult {
    data class Values(val values: Map<String, String>) : PromptResult
    data object Accept : PromptResult
    data object Cancel : PromptResult
}

/**
 * Bridges the (blocking, native) tunnel thread and the (async, Compose) UI.
 *
 * The tunnel thread calls [await], which parks until the UI calls [submit] or
 * the timeout expires. Only one prompt can be outstanding at a time, which
 * matches how libopenconnect drives authentication.
 */
object Interaction {

    private const val PROMPT_TIMEOUT_SECONDS = 180L

    private val ids = AtomicLong(0)
    private val _pending = MutableStateFlow<UserPrompt?>(null)
    val pending: StateFlow<UserPrompt?> = _pending.asStateFlow()

    private var inbox: ArrayBlockingQueue<PromptResult>? = null

    /** Called from the tunnel thread. Blocks. Returns null on timeout. */
    fun await(prompt: UserPrompt): PromptResult {
        val queue = ArrayBlockingQueue<PromptResult>(1)
        synchronized(this) {
            inbox = queue
            _pending.value = prompt
        }
        return try {
            queue.poll(PROMPT_TIMEOUT_SECONDS, TimeUnit.SECONDS) ?: PromptResult.Cancel
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            PromptResult.Cancel
        } finally {
            synchronized(this) {
                inbox = null
                _pending.value = null
            }
        }
    }

    /** Called from the UI thread. */
    fun submit(result: PromptResult) {
        synchronized(this) { inbox }?.offer(result)
    }

    fun cancelPending() = submit(PromptResult.Cancel)

    fun nextId(): Long = ids.incrementAndGet()
}
