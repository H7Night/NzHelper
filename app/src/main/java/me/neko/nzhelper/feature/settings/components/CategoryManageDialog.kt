package me.neko.nzhelper.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun CategoryManageDialog(
    title: String,
    items: List<String>,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerLowest,
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, style = MaterialTheme.typography.headlineSmall)
                    TextButton(onClick = onReset) { Text("恢复默认") }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { Text("输入新增项") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (inputText.isNotBlank() && inputText.trim() !in items) {
                                    onAdd(inputText.trim())
                                    inputText = ""
                                }
                            }
                        ) { Icon(Icons.Default.Add, contentDescription = "添加") }
                    }
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items) { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            // 只剩1项时禁用删除
                            val canRemove = items.size > 1
                            IconButton(
                                onClick = { if (canRemove) onRemove(item) },
                                enabled = canRemove,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "删除",
                                    tint = if (canRemove) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("完成") }
            }
        }
    }
}