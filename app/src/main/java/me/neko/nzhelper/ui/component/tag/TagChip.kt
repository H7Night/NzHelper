package me.neko.nzhelper.ui.component.tag

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.neko.nzhelper.ui.theme.TagColors
import me.neko.nzhelper.ui.theme.TagIcons

/**
 * 只读标签胶囊（用于时间线 / 历史卡片 / 详情 / 统计组合展示）。
 *
 * @param name 标签名
 * @param color 颜色名
 * @param icon 图标名，可选
 * @param small 是否使用小号（用于紧凑列表）
 */
@Composable
fun TagChip(
    modifier: Modifier = Modifier,
    name: String,
    color: String,
    icon: String? = null,
    small: Boolean = false,
) {
    val container = TagColors.containerColor(color)
    val content = TagColors.contentColor(color)
    val shape = if (small) RoundedCornerShape(6.dp) else RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .clip(shape)
            .background(container)
            .padding(horizontal = if (small) 6.dp else 8.dp, vertical = if (small) 2.dp else 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = TagIcons.iconFor(icon),
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(if (small) 10.dp else 12.dp)
            )
        }
        Text(
            text = name,
            style = if (small) MaterialTheme.typography.labelSmall
            else MaterialTheme.typography.labelMedium,
            color = content,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}
