package io.norselabs.vpn.based.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.flow.Flow

@Composable
fun <Effect : Any> EffectHandler(
  effects: Flow<Effect>,
  onEffect: (effect: Effect) -> Unit,
) {
  val currentOnEffect by rememberUpdatedState(onEffect)
  LaunchedEffect(Unit) { effects.collect { effect -> currentOnEffect.invoke(effect) } }
}
