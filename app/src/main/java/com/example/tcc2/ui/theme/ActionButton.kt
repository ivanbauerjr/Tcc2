package com.example.tcc2.ui.theme

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController

@Composable
fun ActionButton(text: String, icon: ImageVector, route: String, navController: NavHostController) {
    Button(
        onClick = { navController.navigate(route) },
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp) // Thicker buttons
            .padding(vertical = 10.dp), // Spacing between buttons
        shape = RoundedCornerShape(12.dp) // Soft rounded corners
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.size(32.dp)) // Bigger icon
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontSize = 20.sp) // Bigger font
    }
}
