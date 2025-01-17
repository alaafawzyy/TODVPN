package co.gulf.todvpn.vpn.ui.widget

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import co.gulf.todvpn.vpn.ui.theme.BasedAppColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun TopBar(
  title: String,
  navigateBack: () -> Unit,
) {
  Surface(
    shadowElevation = 4.dp,
  ) {
    TopAppBar(
      title = { Text(text = title) },
      colors = TopAppBarDefaults.topAppBarColors(
        containerColor = BasedAppColor.Background,
        navigationIconContentColor = BasedAppColor.TextPrimary,
        titleContentColor = BasedAppColor.TextPrimary,
        actionIconContentColor = BasedAppColor.TextPrimary,
      ),
      navigationIcon = {
        IconButton(
          onClick = navigateBack,
        ) {
          Icon(
            imageVector = Icons.Filled.ArrowBack,
            contentDescription = "Go back",
          )
        }
      },
    )
  }
}
