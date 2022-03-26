package qrcode.scanning.android.views

import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import qrcode.scanning.android.viewmodel.HomeViewModel

@Composable
fun HomeView(viewModel: HomeViewModel) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Button(
            onClick = {
                viewModel.buttonOnClick()
            },
            modifier = Modifier.padding(2.dp)
        ) {
            Text(
                text = "Camera",
                color = Color.Green,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
