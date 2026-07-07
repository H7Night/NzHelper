package me.neko.nzhelper.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color

object TagColors {

    private val palette = mapOf(
        "rose" to Color(0xFFE11D48),
        "emerald" to Color(0xFF059669),
        "amber" to Color(0xFFD97706),
        "violet" to Color(0xFF7C3AED),
        "teal" to Color(0xFF0D9488),
        "orange" to Color(0xFFEA580C),
        "pink" to Color(0xFFDB2777),
        "slate" to Color(0xFF475569)
    )

    val names: List<String> = palette.keys.toList()

    @Composable
    fun colorFor(name: String): Color {
        val fallback = MaterialTheme.colorScheme.primary
        val c = palette[name.lowercase()]
        return remember(name) { c ?: fallback }
    }

    /** chip 容器色：原色低透明度叠加，浅/深色模式都可用。 */
    @Composable
    fun containerColor(name: String): Color = colorFor(name).copy(alpha = 0.16f)

    /** chip 文字 / 图标色：原色（保证可读）。 */
    @Composable
    fun contentColor(name: String): Color = colorFor(name)
}
