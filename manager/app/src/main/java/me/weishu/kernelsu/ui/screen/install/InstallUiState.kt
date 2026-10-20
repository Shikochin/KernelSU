package me.weishu.kernelsu.ui.screen.install

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.util.KernelModuleState
import me.weishu.kernelsu.ui.util.LkmSelection

@Immutable
internal data class InstallUiState(
    val installMethod: InstallMethod?,
    val lkmSelection: LkmSelection,
    val partitionSelectionIndex: Int,
    val displayPartitions: List<String>,
    val remotePartitionSelectionIndex: Int,
    val remoteDisplayPartitions: List<String>,
    val currentKmi: String,
    val slotSuffix: String,
    val installMethodOptions: List<InstallMethod>,
    val canSelectPartition: Boolean,
    val advancedOptionsShown: Boolean,
    val allowShell: Boolean,
    val enableAdb: Boolean,
    val forceBackup: Boolean,
    val canForceBackup: Boolean,
    val blockModulesEnabled: Boolean,
    val blockModules: String,
    val moduleSearchQuery: String,
    val blockModulesValid: Boolean,
    val kernelModuleState: KernelModuleState,
    val loadedModulesLoading: Boolean,
    val selectedBlockModules: Set<String>,
) {
    val kernelModules = kernelModuleState.availableModules(selectedBlockModules, moduleSearchQuery)

    val loadedModulesStatus: Int?
        get() = when {
            loadedModulesLoading -> R.string.install_loaded_modules_loading
            kernelModuleState.blocked == null -> R.string.install_blocked_modules_failed
            kernelModuleState.loaded == null -> R.string.install_loaded_modules_failed
            kernelModules.isEmpty() && moduleSearchQuery.isNotEmpty() -> R.string.install_kernel_modules_no_matches
            kernelModules.isEmpty() -> R.string.install_loaded_modules_empty
            else -> null
        }

    fun kernelModuleSummary(name: String): Int = when (name) {
        in kernelModuleState.blocked.orEmpty() -> R.string.install_kernel_module_blocked
        in kernelModuleState.loaded.orEmpty() -> R.string.install_kernel_module_loaded
        else -> R.string.install_kernel_module_manual
    }
}

@Immutable
internal data class InstallScreenActions(
    val onBack: () -> Unit,
    val onSelectMethod: (InstallMethod) -> Unit,
    val onDownloadFile: () -> Unit,
    val onSelectBootImage: () -> Unit,
    val onUploadLkm: () -> Unit,
    val onClearLkm: () -> Unit,
    val onSelectPartition: (Int) -> Unit,
    val onNext: () -> Unit,
    val onAdvancedOptionsClicked: () -> Unit,
    val onSelectAllowShell: (Boolean) -> Unit,
    val onSelectEnableAdb: (Boolean) -> Unit,
    val onSelectForceBackup: (Boolean) -> Unit,
    val onSelectBlockModules: (Boolean) -> Unit,
    val onBlockModulesChange: (String) -> Unit,
    val onRefreshLoadedModules: () -> Unit,
    val onSelectLoadedModule: (String, Boolean) -> Unit,
)
