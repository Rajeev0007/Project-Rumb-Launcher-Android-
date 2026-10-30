/*
 * Copyright 2022, Lawnchair
 * Modifications copyright 2026, Rajeev0007 (Rumb Launcher)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package app.lawnchair.ui.preferences.about

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import app.lawnchair.ui.preferences.LocalIsExpandedScreen
import app.lawnchair.ui.preferences.components.NavigationActionPreference
import app.lawnchair.ui.preferences.components.controls.ClickablePreference
import app.lawnchair.ui.preferences.components.layout.PreferenceDivider
import app.lawnchair.ui.preferences.components.layout.PreferenceGroupHeading
import app.lawnchair.ui.preferences.components.layout.PreferenceGroupItem
import app.lawnchair.ui.preferences.components.layout.PreferenceLayoutLazyColumn
import app.lawnchair.ui.preferences.navigation.AboutLicenses
import com.android.launcher3.BuildConfig
import com.android.launcher3.R

private const val DEVELOPER = "Rajeev0007"
private const val DEVELOPER_URL = "https://github.com/Rajeev0007"
private const val REPOSITORY_URL = "https://github.com/Rajeev0007/Project-Rumb-Launcher-Android-"
private const val RELEASES_URL = "$REPOSITORY_URL/releases"
private const val ISSUES_URL = "$REPOSITORY_URL/issues"
private const val PRIVACY_URL = "$REPOSITORY_URL#privacy"

/**
 * Rumb Launcher's About screen: Rumb identity, developer, and links. Under Legal, the open-source
 * licences entry carries the copyright and licence notices that GPL-3.0 and Apache-2.0 require.
 */
@Composable
fun About(
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    fun open(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri())
        if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
    }

    PreferenceLayoutLazyColumn(
        label = stringResource(id = R.string.about_label),
        modifier = modifier,
        backArrowVisible = !LocalIsExpandedScreen.current,
    ) {
        item { Spacer(Modifier.padding(top = 12.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Image(
                    painter = painterResource(id = R.drawable.rumb_logo),
                    contentDescription = null,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)),
                )
            }
        }
        item { Spacer(modifier = Modifier.height(16.dp)) }
        item {
            Text(
                text = stringResource(id = R.string.derived_app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = BuildConfig.VERSION_DISPLAY_NAME,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Text(
                text = "by $DEVELOPER",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
        item { Spacer(modifier = Modifier.requiredHeight(20.dp)) }
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                LawnchairLink(
                    iconResId = R.drawable.ic_github,
                    label = "GitHub",
                    modifier = Modifier.weight(weight = 1f),
                    url = REPOSITORY_URL,
                )
                LawnchairLink(
                    iconResId = R.drawable.ic_new_releases,
                    label = "Updates",
                    modifier = Modifier.weight(weight = 1f),
                    url = RELEASES_URL,
                )
                LawnchairLink(
                    iconResId = R.drawable.ic_help,
                    label = "Feedback",
                    modifier = Modifier.weight(weight = 1f),
                    url = ISSUES_URL,
                )
            }
        }
        item { PreferenceGroupHeading("Developer") }
        item {
            PreferenceGroupItem(cutTop = false, cutBottom = false) {
                HorizontalLawnchairLink(
                    iconResId = R.drawable.ic_github,
                    label = DEVELOPER,
                    url = DEVELOPER_URL,
                )
            }
        }
        item { PreferenceGroupHeading(stringResource(R.string.legal)) }
        item {
            PreferenceGroupItem(cutTop = false, cutBottom = true) {
                NavigationActionPreference(
                    label = stringResource(id = R.string.acknowledgements),
                    destination = AboutLicenses,
                    subtitle = "Licences and copyright notices",
                )
            }
        }
        item {
            PreferenceGroupItem(cutTop = true, cutBottom = false) {
                PreferenceDivider()
                ClickablePreference(
                    label = stringResource(id = R.string.privacy_policy),
                    onClick = { open(PRIVACY_URL) },
                )
            }
        }
        item { Spacer(modifier = Modifier.requiredHeight(24.dp)) }
    }
}
