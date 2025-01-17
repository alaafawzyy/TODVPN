package co.gulf.todvpn.vpn.ui.screens.cities

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import co.gulf.todvpn.vpn.R
import co.gulf.todvpn.vpn.ui.theme.BasedAppColor
import co.gulf.todvpn.vpn.ui.widget.ErrorScreen
import co.gulf.todvpn.vpn.ui.widget.TopBar
import io.norselabs.vpn.based.compose.EffectHandler
import io.norselabs.vpn.based.viewModel.cities.CitiesScreenEffect as Effect
import io.norselabs.vpn.based.viewModel.cities.CitiesScreenState as State
import io.norselabs.vpn.based.viewModel.cities.CitiesScreenViewModel
import io.norselabs.vpn.based.viewModel.cities.CityUi
import io.norselabs.vpn.common.state.Status

@Composable
fun CitiesScreen(
  countryId: String?,
  navigateBack: () -> Unit,
  navigateBackToRoot: () -> Unit,
) {
  val viewModel = hiltViewModel<CitiesScreenViewModel>()
  val state by viewModel.stateHolder.state.collectAsState()

  LaunchedEffect(countryId) {
    viewModel.setCountryId(countryId)
  }

  EffectHandler(viewModel.stateHolder.effects) { effect ->
    when (effect) {
      is Effect.GoBackToRoot -> navigateBackToRoot()
    }
  }

  CitiesScreenStateless(
    state = state,
    navigateBack = navigateBack,
    onItemClick = viewModel::onCityClick,
    onTryAgainClick = viewModel::onTryAgainClick,
  )
}

@Composable
fun CitiesScreenStateless(
  state: State,
  navigateBack: () -> Unit,
  onItemClick: (CityUi) -> Unit,
  onTryAgainClick: () -> Unit,
) {
  Scaffold(
    containerColor = BasedAppColor.Background,
    topBar = {
      TopBar(
        title = stringResource(R.string.cities_title),
        navigateBack = navigateBack,
      )
    },
    content = { paddingValues ->
      Content(
        paddingValues = paddingValues,
        state = state,
        onItemClick = onItemClick,
        onTryAgainClick = onTryAgainClick,
      )
    },
  )
}

@Composable
private fun Content(
  paddingValues: PaddingValues,
  state: State,
  onItemClick: (CityUi) -> Unit,
  onTryAgainClick: () -> Unit,
) {
  Box(
    modifier = Modifier
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .padding(paddingValues)
      .fillMaxSize(),
  ) {
    when (state.status) {
      is Status.Loading -> Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
      ) {
        CircularProgressIndicator()
      }

      is Status.Error -> ErrorScreen(
        isLoading = (state.status as Status.Error).isLoading,
        onButtonClick = onTryAgainClick,
      )

      is Status.Data -> Data(state, onItemClick)
    }
  }
}

@Composable
private fun Data(
  state: State,
  onItemClick: (CityUi) -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
  ) {
    items(state.cities) { city ->
      CityRow(city, onItemClick)
      Divider(color = BasedAppColor.Divider)
    }
  }
}

@Composable
private fun CityRow(
  city: CityUi,
  onItemClick: (CityUi) -> Unit,
) {
  Row(
    modifier = Modifier
      .clickable(onClick = { onItemClick(city) })
      .heightIn(min = 60.dp)
      .padding(horizontal = 16.dp, vertical = 8.dp)
      .fillMaxWidth(),
  ) {
    Text(
      text = buildAnnotatedString {
        withStyle(style = SpanStyle(color = BasedAppColor.TextPrimary)) {
          append(city.name)
        }
        withStyle(style = SpanStyle(color = BasedAppColor.TextSecondary)) {
          append(" • ")
          append(stringResource(R.string.cities_servers_number, city.serversAvailable))
        }
      },
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      fontSize = 16.sp,
    )
  }
}
