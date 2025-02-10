package co.gulf.todvpn.vpn.ui.screens.countries

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import co.gulf.todvpn.vpn.ui.theme.BasedAppColor
import co.gulf.todvpn.vpn.ui.widget.ErrorScreen
import co.gulf.todvpn.vpn.ui.widget.TopBar
import io.norselabs.vpn.based.compose.EffectHandler
import io.norselabs.vpn.based.viewModel.countries.CountriesScreenEffect as Effect
import io.norselabs.vpn.based.viewModel.countries.CountriesScreenState as State
import io.norselabs.vpn.based.viewModel.countries.CountriesScreenViewModel
import io.norselabs.vpn.based.viewModel.countries.CountryUi
import io.norselabs.vpn.common.state.Status
import androidx.compose.material3.MaterialTheme
import co.gulf.todvpn.vpn.R
import androidx.compose.foundation.focusable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color


@Composable
fun CountriesScreen(
  navigateBack: () -> Unit,
  navigateToCities: (String) -> Unit,
) {
  val viewModel = hiltViewModel<CountriesScreenViewModel>()
  val state by viewModel.stateHolder.state.collectAsState()

  EffectHandler(viewModel.stateHolder.effects) { effect ->
    when (effect) {
      is Effect.ShowCitiesScreen ->
        navigateToCities(effect.countryId)
    }
  }

  CountriesScreenStateless(
    state = state,
    navigateBack = navigateBack,
    onItemClick = viewModel::onCountryClick,
    onTryAgainClick = viewModel::onTryAgainClick,
  )
}

@Composable
fun CountriesScreenStateless(
  state: State,
  navigateBack: () -> Unit,
  onItemClick: (CountryUi) -> Unit,
  onTryAgainClick: () -> Unit,
) {
  Scaffold(
    containerColor = BasedAppColor.Background,
    topBar = {
      TopBar(
        title = stringResource(R.string.countries_title),
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
fun Content(
  paddingValues: PaddingValues,
  state: State,
  onItemClick: (CountryUi) -> Unit,
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

      is Status.Data -> {
        val todStreamingCountries = state.countries.filter {
          it.name in listOf("United Arab Emirates", "Qatar", "Bahrain","Iraq","Oman")
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
          // TOD STREAMING Section
          if (todStreamingCountries.isNotEmpty()) {
            item {
              Spacer(modifier = Modifier.height(16.dp))
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .padding(horizontal = 16.dp)
                  .fillMaxWidth()
              ) {
                Image(
                  painter = painterResource(id = R.drawable.tod_tv), // Replace with your logo's resource name
                  contentDescription = null,
                  modifier = Modifier
                    .size(46.dp) // Adjust the size as per your design
                    .padding(end = 8.dp) // Add some space between the logo and the text
                )
                Text(
                  text = "VIP STREAMING",
                  style = MaterialTheme.typography.titleMedium,
                  color = BasedAppColor.TextPrimary
                )
              }

            }
            items(todStreamingCountries) { country ->
              CountryRow(country, onItemClick)
              Divider(color = BasedAppColor.Divider)
            }

            // Add spacing between TOD section and other countries
            item { Spacer(modifier = Modifier.height(24.dp)) }
          }

          // Normal Countries Section
          items(state.countries) { country ->
            CountryRow(country, onItemClick)
            Divider(color = BasedAppColor.Divider)
          }
        }
      }
    }
  }
}

@Composable
private fun CountryRow(
  country: CountryUi,
  onItemClick: (CountryUi) -> Unit,
) {
  var isFocused by remember { mutableStateOf(false) }

  Row(
    modifier = Modifier
      .clip(RoundedCornerShape(8.dp))
      .clickable(onClick = { onItemClick(country) })
      .heightIn(min = 60.dp)
      .padding(horizontal = 16.dp, vertical = 16.dp)
      .fillMaxWidth()
      .focusable() // Make the row focusable
      .onFocusChanged { focusState -> isFocused = focusState.isFocused }
      .graphicsLayer(
        scaleX = if (isFocused) 1.1f else 1f, // Scale slightly on focus
        scaleY = if (isFocused) 1.1f else 1f, // Scale slightly on focus
      )
      .background(
        color = if (isFocused) BasedAppColor.ButtonPrimaryText.copy(alpha = 0.2f) else Color.Transparent, // Add a background highlight on focus
        shape = RoundedCornerShape(8.dp),
      ),
  ) {
    val flagRes = country.flag?.res
    if (flagRes != null) {
      var isFocused by remember { mutableStateOf(false) }

      Box(
        modifier = Modifier
          .size(width = 36.dp, height = 24.dp)
          .focusable() // Enable focus
          .onFocusChanged { focusState -> isFocused = focusState.isFocused }
          .graphicsLayer(
            scaleX = if (isFocused) 1.1f else 1f, // Add scaling effect
            scaleY = if (isFocused) 1.1f else 1f,
          )
          .background(
            color = if (isFocused) BasedAppColor.ButtonPrimary.copy(alpha = 0.3f) else Color.Transparent, // Add highlight effect
            shape = RoundedCornerShape(4.dp),
          )
          .clip(RoundedCornerShape(4.dp))
      ) {
        Image(
          painter = painterResource(flagRes),
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(), // Ensure the image fills the Box
        )
      }
    } else {
      Box(
        modifier = Modifier
          .size(width = 36.dp, height = 24.dp)
          .clip(RoundedCornerShape(4.dp))
          .background(BasedAppColor.Divider),
      )
    }
    Spacer(modifier = Modifier.size(16.dp))
    Text(
      text = country.name,
      overflow = TextOverflow.Ellipsis,
      maxLines = 1,
      fontSize = 16.sp,
      color = BasedAppColor.TextPrimary,
    )

  }
}
