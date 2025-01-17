package io.norselabs.vpn.based.viewModel.cities

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.norselabs.vpn.common.state.Status
import io.norselabs.vpn.common_network.AppRepository
import io.norselabs.vpn.common_network.models.CitiesRequest
import io.norselabs.vpn.core_vpn.storage.CoreStorage
import io.norselabs.vpn.core_vpn.vpn.Destination
import io.norselabs.vpn.core_vpn.vpn.Protocol
import io.norselabs.vpn.core_vpn.vpn.destination.DestinationStorage
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class CitiesScreenViewModel
@Inject constructor(
  val stateHolder: CitiesScreenStateHolder,
  private val repository: AppRepository,
  private val coreStorage: CoreStorage,
  private val destinationStorage: DestinationStorage,
) : ViewModel() {

  private val state: CitiesScreenState
    get() = stateHolder.state.value

  fun setCountryId(countryId: String?) {
    if (countryId != null) {
      stateHolder.updateState {
        copy(
          status = Status.Loading,
          countryId = countryId,
        )
      }
      getCities(countryId)
    }
  }

  private fun getCities(countryId: String) {
    viewModelScope.launch {
      val protocol = coreStorage.getVpnProtocol().takeIf { it != Protocol.NONE }
      val countries = repository.getCountries(protocol?.strValue).getOrNull()?.data
      val cities = repository.getCities(CitiesRequest(countryId, protocol?.strValue)).getOrNull()?.data
      if (countries != null && cities != null) {
        stateHolder.updateState {
          copy(
            status = Status.Data,
            country = countries.first { it.id == countryId },
            cities = cities.map { city ->
              CityUi(city.id, city.countryId, city.name, city.serversAvailable)
            },
          )
        }
      } else {
        stateHolder.updateState { copy(status = Status.Error(false)) }
      }
    }
  }

  fun onCityClick(city: CityUi) {
    val country = state.country ?: return
    destinationStorage.storeDestination(
      Destination.City(
        cityId = city.id,
        cityName = city.name,
        countryId = country.id,
        countryName = country.name,
        countryCode = country.code,
      ),
    )
    stateHolder.sendEffect(CitiesScreenEffect.GoBackToRoot)
  }

  fun onTryAgainClick() {
    stateHolder.updateState { copy(status = Status.Error(true)) }
    state.countryId?.let(::getCities)
  }
}
