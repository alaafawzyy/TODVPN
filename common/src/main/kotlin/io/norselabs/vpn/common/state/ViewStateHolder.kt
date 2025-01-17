package io.norselabs.vpn.common.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class ViewStateHolder<State, Effect>(initialState: State) {

  private val coroutineScope = CoroutineScope(Dispatchers.IO)

  private val _state = MutableStateFlow(initialState)
  val state: StateFlow<State>
    get() = _state

  private val _effects = MutableSharedFlow<Effect>()
  val effects: SharedFlow<Effect>
    get() = _effects.asSharedFlow()

  /**
   * Creates a new immutable state for each update.
   * newState lambda must provide the copied state object.
   */
  fun updateState(newState: State.() -> State) = _state.update(newState)

  fun sendEffect(effect: Effect) {
    coroutineScope.launch {
      _effects.subscriptionCount.firstOrNull { it > 0 }
      _effects.emit(effect)
    }
  }
}
