package com.dergoogler.mmrl.ui.screens.modules

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.dergoogler.mmrl.R
import com.dergoogler.mmrl.model.local.LocalModule
import com.dergoogler.mmrl.model.local.State
import com.dergoogler.mmrl.model.local.versionDisplay
import com.dergoogler.mmrl.ui.activity.webui.WebUIActivity
import com.dergoogler.mmrl.ui.component.LabelItem
import com.dergoogler.mmrl.ui.component.TextWithIcon
import com.dergoogler.mmrl.ui.component.card.Card
import com.dergoogler.mmrl.ui.component.card.CardDefaults.cardStyle
import com.dergoogler.mmrl.ui.providable.LocalUserPreferences
import com.dergoogler.mmrl.ext.nullable
import com.dergoogler.mmrl.ext.takeTrue
import com.dergoogler.mmrl.platform.file.SuFile.Companion.toFormattedFileSize
import com.dergoogler.mmrl.utils.toFormattedDateSafely

@Composable
fun ModuleItem(
    module: LocalModule,
    progress: Float,
    indeterminate: Boolean = false,
    alpha: Float = 1f,
    decoration: TextDecoration = TextDecoration.None,
    switch: @Composable() (() -> Unit?)? = null,
    indicator: @Composable() (BoxScope.() -> Unit?)? = null,
    startTrailingButton: @Composable() (RowScope.() -> Unit)? = null,
    trailingButton: @Composable() (RowScope.() -> Unit),
    isBlacklisted: Boolean = false,
    isProviderAlive: Boolean,
    createWebUIShortcut: (String) -> Unit,
) {
    val userPreferences = LocalUserPreferences.current
    val menu = userPreferences.modulesMenu
    val context = LocalContext.current

    val canWenUIAccessed = isProviderAlive && module.features.webui && module.state != State.REMOVE
    val clicker: (() -> Unit)? = canWenUIAccessed nullable {
        WebUIActivity.start(
            context = context,
            modId = module.id
        )
    }

    Card(
        modifier = {
            if (isBlacklisted) {
                surface = this.surface.then(
                    Modifier.border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = cardStyle.shape
                    )
                )
            }

            column = Modifier.padding(0.dp)
        },
        style = cardStyle.copy(
            boxContentAlignment = Alignment.Center,
        ),
        absolute = {
            indicator?.invoke(this)
        },
        onLongClick = {
            createWebUIShortcut(module.id)
        },
        onClick = clicker
    ) {
        Row(
            modifier = Modifier.padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .alpha(alpha = alpha)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {

                val icon =
                    if (module.state != State.REMOVE) R.drawable.world_code else R.drawable.world_off

                TextWithIcon(
                    text = module.name,
                    icon = module.features.webui nullable icon,
                    style = MaterialTheme.typography.titleSmall
                )

                Text(
                    text = stringResource(
                        id = R.string.module_version_author,
                        module.versionDisplay, module.author
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    textDecoration = decoration,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (module.lastUpdated != 0L && menu.showUpdatedTime) {
                    Text(
                        text = stringResource(
                            id = R.string.module_update_at,
                            module.lastUpdated.toFormattedDateSafely
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        textDecoration = decoration,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            switch?.invoke()
        }

        Text(
            modifier = Modifier
                .alpha(alpha = alpha)
                .padding(horizontal = 16.dp),
            text = module.description,
            style = MaterialTheme.typography.bodySmall,
            textDecoration = decoration,
            color = MaterialTheme.colorScheme.outline
        )

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            userPreferences.developerMode.takeTrue {
                LabelItem(
                    text = module.id,
                    upperCase = false
                )
            }

            LabelItem(
                text = module.size.toFormattedFileSize(),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )

        }

        when {
            indeterminate -> LinearProgressIndicator(
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(2.dp)
                    .fillMaxWidth()
            )

            progress != 0f -> LinearProgressIndicator(
                progress = { progress },
                strokeCap = StrokeCap.Round,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(1.5.dp)
                    .fillMaxWidth()
            )

            else -> HorizontalDivider(
                thickness = 1.5.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            startTrailingButton?.invoke(this)
            Spacer(modifier = Modifier.weight(1f))
            trailingButton.invoke(this)
        }
    }
}

@Composable
fun StateIndicator(
    @DrawableRes icon: Int,
    color: Color = MaterialTheme.colorScheme.outline,
) = Image(
    modifier = Modifier.requiredSize(150.dp),
    painter = painterResource(id = icon),
    contentDescription = null,
    alpha = 0.1f,
    colorFilter = ColorFilter.tint(color)
)
