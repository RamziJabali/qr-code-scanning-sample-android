package qrcode.scanning.android.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import qrcode.scanning.android.HomeViewState

class HomeViewModel {
    private val _viewState = MutableStateFlow(HomeViewState())
    val viewState = _viewState.asStateFlow()

    fun buttonOnClick(){
        _viewState.value = HomeViewState(isButtonClicked = true)
    }
}