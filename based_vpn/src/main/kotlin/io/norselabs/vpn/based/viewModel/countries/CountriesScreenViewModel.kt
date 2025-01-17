package io.norselabs.vpn.based.viewModel.countries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.norselabs.vpn.common.state.Status
import io.norselabs.vpn.common_flags.mapToFlag
import io.norselabs.vpn.common_network.AppRepository
import io.norselabs.vpn.core_vpn.storage.CoreStorage
import io.norselabs.vpn.core_vpn.vpn.Protocol
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CountriesScreenViewModel
@Inject constructor(
  val stateHolder: CountriesScreenStateHolder,
  private val repository: AppRepository,
  private val coreStorage: CoreStorage,
) : ViewModel() {

  private val state: CountriesScreenState
    get() = stateHolder.state.value

  init {
    stateHolder.updateState { copy(status = Status.Loading) }
    getCountries()
  }

  private fun getCountries() {
    viewModelScope.launch {
      val protocol = coreStorage.getVpnProtocol().takeIf { it != Protocol.NONE }
      val countries = repository.getCountries(protocol?.strValue).getOrNull()?.data
      if (countries != null) {
        stateHolder.updateState {
          copy(
            status = Status.Data,
            countries = countries.map { country ->
              CountryUi(
                id = country.id,
                name = country.name,
                code = country.code,
                flag = mapToFlag(country.code),
                serversAvailable = country.serversAvailable,
              )
            },
          )
        }
      } else {
        stateHolder.updateState { copy(status = Status.Error(false)) }
      }
    }
  }

  fun onCountryClick(country: CountryUi) {
    stateHolder.sendEffect(CountriesScreenEffect.ShowCitiesScreen(country.id))
  }

  fun onTryAgainClick() {
    stateHolder.updateState { copy(status = Status.Error(true)) }
    getCountries()
  }
}
