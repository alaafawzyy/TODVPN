package co.gulf.todvpn.vpn.ui.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.collections.immutable.ImmutableList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : Any> Dropdown(
  label: String,
  items: ImmutableList<T>,
  selected: T,
  mapItemLabel: (T) -> String,
  onSelect: (T) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  var expanded by remember { mutableStateOf(false) }
  val isEnabled by rememberUpdatedState(enabled)

  Box(modifier = modifier) {
    ExposedDropdownMenuBox(
      expanded = isEnabled && expanded,
      onExpandedChange = { if (isEnabled) expanded = it },
    ) {
      OutlinedTextField(
        value = mapItemLabel(selected),
        label = {
          Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
          )
        },
        onValueChange = {},
        textStyle = MaterialTheme.typography.bodyLarge,
        readOnly = true,
        trailingIcon = {
          ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        },
        colors = OutlinedTextFieldDefaults.colors(),
        modifier = Modifier
          .fillMaxWidth()
          .menuAnchor(),
      )

      ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier.fillMaxWidth(),
      ) {
        items.forEach { item ->
          DropdownMenuItem(
            text = { Text(text = mapItemLabel(item)) },
            onClick = {
              onSelect(item)
              expanded = false
            },
          )
        }
      }
    }
  }
}
