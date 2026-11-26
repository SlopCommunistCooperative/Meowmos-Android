package io.github.slopcommunistcooperative.meowmos.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.slopcommunistcooperative.meowmos.data.local.MeowmosDatabase
import io.github.slopcommunistcooperative.meowmos.data.service.AccountService
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ResourceListViewModel @Inject constructor(
    accountService: AccountService,
    database: MeowmosDatabase,
) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val resources = accountService.currentAccount
        .map { it?.accountKey() }
        .distinctUntilChanged()
        .flatMapLatest { key ->
            if (key == null) flowOf(emptyList()) else database.memoDao().observeResources(key)
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
}
