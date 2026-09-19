package com.openmusic.search.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openmusic.search.data.local.prefs.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsDataStore
) : ViewModel() {
    val theme: StateFlow<SettingsDataStore.Theme> = settings.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsDataStore.Theme.SYSTEM)
    val autoPlayNext: StateFlow<Boolean> = settings.autoPlayNext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val backgroundPlayback: StateFlow<Boolean> = settings.backgroundPlayback
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)
    val wifiOnly: StateFlow<Boolean> = settings.wifiOnly
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setTheme(t: SettingsDataStore.Theme) { viewModelScope.launch { settings.setTheme(t) } }
    fun setAutoPlayNext(b: Boolean) { viewModelScope.launch { settings.setAutoPlayNext(b) } }
    fun setBackgroundPlayback(b: Boolean) { viewModelScope.launch { settings.setBackgroundPlayback(b) } }
    fun setWifiOnly(b: Boolean) { viewModelScope.launch { settings.setWifiOnly(b) } }
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val theme by viewModel.theme.collectAsState()
    val autoPlay by viewModel.autoPlayNext.collectAsState()
    val bgPlayback by viewModel.backgroundPlayback.collectAsState()
    val wifiOnly by viewModel.wifiOnly.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("设置", style = MaterialTheme.typography.titleLarge)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))

        SettingSwitch("自动下一首", autoPlay, viewModel::setAutoPlayNext)
        SettingSwitch("后台播放", bgPlayback, viewModel::setBackgroundPlayback)
        SettingSwitch("仅 Wi-Fi 下载", wifiOnly, viewModel::setWifiOnly)

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
        Text("主题：${theme.name}", style = MaterialTheme.typography.bodyMedium)
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
        ) {
            listOf(SettingsDataStore.Theme.LIGHT, SettingsDataStore.Theme.DARK, SettingsDataStore.Theme.SYSTEM).forEach { t ->
                androidx.compose.material3.FilterChip(
                    selected = theme == t,
                    onClick = { viewModel.setTheme(t) },
                    label = { Text(t.name) }
                )
            }
        }

        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
        Text("关于", style = MaterialTheme.typography.titleMedium)
        Text("OpenMusic Search v1.0.0", style = MaterialTheme.typography.bodySmall)
        Text("搜索并播放来自 Internet Archive、Wikimedia Commons 等合法来源的音视频内容。", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
