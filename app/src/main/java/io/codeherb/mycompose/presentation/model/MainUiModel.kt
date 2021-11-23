package io.codeherb.mycompose.presentation.model

sealed class ScreenEvent
object FloatingScreen: ScreenEvent()
sealed class DialogScreen : ScreenEvent() {
    object Show: DialogScreen() {
        override fun hashCode(): Int = DialogScreen::class.hashCode()
    }
    object Dismiss: DialogScreen() {
        override fun hashCode(): Int = DialogScreen::class.hashCode()
    }
}
object BaseScreen: ScreenEvent()

data class ShopItem(
    val name: String,
    val imgUrl: String = "https://avatars2.githubusercontent.com/u/13534988?v=4",
    val price: Int = 0
)