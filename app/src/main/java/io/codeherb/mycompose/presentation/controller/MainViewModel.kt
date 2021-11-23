package io.codeherb.mycompose.presentation.controller

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.codeherb.mycompose.*
import io.codeherb.mycompose.presentation.composableMapper
import io.codeherb.mycompose.presentation.model.*
import io.codeherb.mycompose.util.runningFold

class MainViewModel: ViewModel() {

    private val _cart: MutableList<ShopItem> = mutableListOf()
    val cartItems: List<ShopItem>
        get() = _cart.toList()

    private val _eventFlow = MutableLiveData<ScreenEvent>(BaseScreen)
    val eventFlow: LiveData<ScreenEvent>
        get() = _eventFlow

    fun getItems(): List<List<ShopItem>> {
        return listOf(
            ShopItem("8부 슬렉스", price = 40000), ShopItem("스트레이트 핏 진", price = 60000),
            ShopItem("하얀색 셔츠", price = 30000), ShopItem("감색 셔츠", price = 30000),
            ShopItem("스키니진", price = 50000), ShopItem("페이크삭스", price = 5000),
            ShopItem("가죽자켓", price = 1000), ShopItem("롱스커트", price = 60000),
            ShopItem("숏팬츠", price = 40000), ShopItem("스포츠 레깅스", price = 40000),
            ShopItem("가죽 등산화", price = 60000)
        ).windowed(2, 2, true)
    }

    fun addCartItem(item: ShopItem) {
        _cart.add(item)
        _eventFlow.value = FloatingScreen
    }

    fun showDialog() {
        _eventFlow.value = DialogScreen.Show
    }

    fun dismissDialog() {
        _eventFlow.value = DialogScreen.Dismiss
    }

}