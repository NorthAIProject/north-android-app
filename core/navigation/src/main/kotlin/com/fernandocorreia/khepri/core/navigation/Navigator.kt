package com.fernandocorreia.khepri.core.navigation

import com.fernandocorreia.khepri.core.common.ApplicationScope
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The one way to move around the app: deep links, push taps, widgets, shortcuts and in-app links all call it.
 * Destinations are commands, so the same push tapped twice lands twice. Commands are held until there is a session.
 */
public interface Navigator {
    public fun open(destination: AppDestination)

    /** Opens a link; false when this build does not understand it, so the caller can open the browser. */
    public fun open(link: String): Boolean

    public fun push(route: Route)

    public fun pop()

    public fun popToRoot()

    public fun select(tab: AppTab)
}

/** Invite and crew join codes, kept on disk so they survive process death and sign-up. Implemented in `:app`. */
public interface PendingLinks {
    public suspend fun put(destination: AppDestination)

    public suspend fun take(): List<AppDestination>
}

/**
 * Queues commands in arrival order. Invite and crew join codes are written to [PendingLinks] before their command is
 * queued, so a screen that redeems on the command finds the code. `NavigationState` takes the queue while signed in.
 */
// Not account data: commands queued while signed out (a tapped link) are for whoever signs in next, and
// NavigationState takes the queue only while someone is signed in.
@Suppress("AccountStateNeedsCleaner")
@Singleton
public class QueuedNavigator
@Inject
constructor(private val pendingLinks: PendingLinks, @ApplicationScope scope: CoroutineScope) : Navigator {
    private val intake = Channel<Stamped>(Channel.UNLIMITED)
    private val generation = AtomicLong()
    private val queue = MutableStateFlow<List<NavCommand>>(emptyList())

    /** Commands not yet applied. */
    public val pending: StateFlow<List<NavCommand>> = queue.asStateFlow()

    init {
        scope.launch {
            for (stamped in intake) {
                if (stamped.generation != generation.get()) continue
                keep(stamped.command)
                queue.update { if (stamped.generation == generation.get()) it + stamped.command else it }
            }
        }
    }

    override fun open(destination: AppDestination) {
        send(NavCommand.Open(destination))
    }

    override fun open(link: String): Boolean = DeepLinkParser.parse(link)?.also(::open) != null

    override fun push(route: Route) {
        send(NavCommand.Push(route))
    }

    override fun pop() {
        send(NavCommand.Pop)
    }

    override fun popToRoot() {
        send(NavCommand.PopToRoot)
    }

    override fun select(tab: AppTab) {
        send(NavCommand.Select(tab))
    }

    /** Takes every queued command at once, in order. */
    public fun takeAll(): List<NavCommand> = queue.getAndUpdate { emptyList() }

    /** Drops what the last session left queued, including commands sent but not yet queued. */
    public fun drop() {
        generation.incrementAndGet()
        queue.value = emptyList()
    }

    private fun send(command: NavCommand) {
        intake.trySend(Stamped(generation.get(), command))
    }

    private data class Stamped(val generation: Long, val command: NavCommand)

    @Suppress("TooGenericExceptionCaught")
    private suspend fun keep(command: NavCommand) {
        val destination = (command as? NavCommand.Open)?.destination
        if (destination !is AppDestination.Invite && destination !is AppDestination.CrewJoin) return
        try {
            pendingLinks.put(destination)
        } catch (e: CancellationException) {
            throw e
        } catch (expected: Exception) {
            return
        }
    }
}
