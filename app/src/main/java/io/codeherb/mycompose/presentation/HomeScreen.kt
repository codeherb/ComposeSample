package io.codeherb.mycompose.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.ExperimentalUnitApi
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberImagePainter
import io.codeherb.mycompose.R
import io.codeherb.mycompose.presentation.controller.MainViewModel
import io.codeherb.mycompose.presentation.model.*
import io.codeherb.mycompose.util.runningFold

@Composable
fun MyApp() {
    val viewModel: MainViewModel = viewModel()
    val screenState = viewModel.eventFlow
        .runningFold(mapOf<Int, @Composable BoxScope.() -> Unit>()) { acc, v ->
            acc.toMutableMap().apply {
                put(v.hashCode(), composableMapper(v))
            }
        }.observeAsState(mapOf())

    Scaffold(
        topBar = {
            TopAppBar {
                Text(
                    text = stringResource(R.string.home_screen_title),
                    modifier = Modifier
                        .align(Alignment.CenterVertically)
                        .padding(start = 12.dp)
                )
            }
        }
    ) {
        Box(modifier = Modifier.padding(it)) {
            screenState.value.forEach {
                it.value(this)
            }
        }
    }
}

val IdentityComposable: (@Composable BoxScope.() -> Unit) = (@Composable {})
val composableMapper: (ScreenEvent) -> (@Composable BoxScope.() -> Unit) = { event ->
    when (event) {
        FloatingScreen -> (@Composable {
            val ctrl = viewModel<MainViewModel>()
            FloatingButtons(ctrl.cartItems) {
                ctrl.showDialog()
            }
        })
        DialogScreen.Show -> (@Composable {
            val ctrl = viewModel<MainViewModel>()
            MyDialog("${ctrl.cartItems.joinToString(", ") { it.name }}, ${ctrl.cartItems.size}개를 주문하시겠습니까?", {
                ctrl.dismissDialog()
            }, {
                ctrl.dismissDialog()
            })
        })
        BaseScreen -> (@Composable {
            val ctrl = viewModel<MainViewModel>()
            ShopList(ctrl.getItems()) { item ->
                ctrl.addCartItem(item)
            }
        })
        else -> IdentityComposable
    }
}

@Composable
fun ShopList(list: List<List<ShopItem>>, onItemClick: (ShopItem) -> Unit) {
    LazyColumn {
        items(list) {
            Row(modifier = Modifier.fillParentMaxWidth()) {
                it.forEachIndexed { idx, item ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                start = (if (idx % 2 == 0) 12.dp else 6.dp),
                                end = (if (idx % 2 == 0) 6.dp else 12.dp),
                                top = 6.dp, bottom = 6.dp
                            )
                    ) {
                        ShoppingItem(item) {
                            onItemClick(item)
                        }
                    }
                }
                if (it.size % 2 > 0) {
                    Column(modifier = Modifier.weight(1f)) { /** Do Nothing */ }
                }
            }
        }
    }
}

@OptIn(ExperimentalUnitApi::class)
@Composable
fun ShoppingItem(item: ShopItem, onClick: () -> Unit) {
    Box {
        Image(
            rememberImagePainter(data= item.imgUrl), null, modifier = Modifier
                .heightIn(max = 200.dp)
                .fillMaxWidth())
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .shadow(elevation = 2.dp, shape = CircleShape)
                .background(color = Color.White, shape = CircleShape)
        ) {
            Image(Icons.Outlined.ShoppingCart, null)
        }
    }
    Row {
        Text(text = item.name)
        Text(
            text = "${item.price}원",
            textAlign = TextAlign.End,
            fontSize = TextUnit(12f, TextUnitType.Sp),
            color = Color.Gray,
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Bottom)
        )
    }

}

@Composable
fun BoxScope.FloatingButtons(items: List<ShopItem>, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .background(Color.Red)
            .fillMaxWidth()
            .requiredHeight(50.dp)
            .align(Alignment.BottomStart)
    ) {
        Row {
            Image(imageVector = Icons.Outlined.ShoppingCart, contentDescription = null)
            Text(
                text = "${String.format("%,d", items.fold(0) { acc, item -> acc + item.price })}원",
                modifier = Modifier.padding(start = 10.dp, end = 10.dp)
            )
        }
    }
}
@Composable
fun MyDialog(message: String, onClick: () -> Unit, onCancelClick: () -> Unit) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = stringResource(R.string.app_name))
        }, text = {
            Text(text = message)
        }, buttons = {
            Row(modifier = Modifier.fillMaxWidth()) {
                val buttonModifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp)
                Button(
                    onClick = onClick,
                    modifier = buttonModifier.padding(start = 12.dp, end = 6.dp)
                ) {
                    Text(text = stringResource(R.string.ok))
                }
                Button(
                    onClick = onCancelClick,
                    modifier = buttonModifier.padding(end = 12.dp)
                ) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        }
    )
}