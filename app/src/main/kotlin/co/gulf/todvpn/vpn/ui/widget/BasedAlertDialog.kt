package co.gulf.todvpn.vpn.ui.widget

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun BasedAlertDialog(
  title: String,
  description: String? = null,
  onConfirmClick: () -> Unit,
  onDismissClick: (() -> Unit)? = null,
  onDismissRequest: () -> Unit = {},
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    title = { Text(title) },
    text = description?.let { { Text(description) } },
    confirmButton = {
      Button(
        onClick = onConfirmClick,
      ) {
        Text("Ok")
      }
    },
    dismissButton = onDismissClick?.let {
      {
        Button(
          onClick = onDismissClick,
        ) {
          Text("Cancel")
        }
      }
    },
  )
}

@Composable
@Preview
fun BasedAlertDialogPreview() {
  BasedAlertDialog(
    title = "Hello World",
    description = "Lorem ipsum lorem ipsum lorem ipsum lorem ipsum lorem ipsum",
    onConfirmClick = {},
    onDismissClick = {},
  )
}
