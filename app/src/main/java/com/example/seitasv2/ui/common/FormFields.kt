package com.example.seitasv2.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

private val PeachShape = RoundedCornerShape(14.dp)
private val PeachContainer = Color(0x1AFFFFFF)   // blanco 10% opacidad
private val PeachBorder = Color(0x33FFFFFF)      // borde 20% opacidad
private val PeachText = Color(0xFF3B2B27)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeachTextFieldEmail(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Email",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
        singleLine = true,
        shape = PeachShape,
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            unfocusedIndicatorColor = PeachBorder,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = PeachContainer,
            unfocusedContainerColor = PeachContainer,
            focusedLabelColor = PeachText,
            unfocusedLabelColor = PeachText.copy(alpha = 0.8f),
            focusedTextColor = PeachText,
            unfocusedTextColor = PeachText
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeachTextFieldPassword(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Contraseña",
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
        singleLine = true,
        shape = PeachShape,
        visualTransformation = PasswordVisualTransformation(),
        modifier = modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        colors = TextFieldDefaults.colors(
            focusedIndicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
            unfocusedIndicatorColor = PeachBorder,
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedContainerColor = PeachContainer,
            unfocusedContainerColor = PeachContainer,
            focusedLabelColor = PeachText,
            unfocusedLabelColor = PeachText.copy(alpha = 0.8f),
            focusedTextColor = PeachText,
            unfocusedTextColor = PeachText
        )
    )
}
