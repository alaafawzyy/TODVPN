package io.norselabs.vpn.based.viewModel.countries

import io.norselabs.vpn.common.state.Status
import io.norselabs.vpn.common.state.ViewStateHolder
import io.norselabs.vpn.common_flags.CountryFlag
import javax.inject.Inject

class CountriesScreenStateHolder
@Inject
constructor() : ViewStateHolder<CountriesScreenState, CountriesScreenEffect>(
  CountriesScreenState(),
)

data class CountriesScreenState(
  val status: Status = Status.Data,
  val countries: List<CountryUi> = emptyList(),
)

data class CountryUi(
  val id: String,
  val name: String,
  val code: String,
  val flag: CountryFlag?,
  val serversAvailable: Int,
)

sealed interface CountriesScreenEffect {
  data class ShowCitiesScreen(val countryId: String) : CountriesScreenEffect
}
