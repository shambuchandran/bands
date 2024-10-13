package com.example.bands.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.bands.DestinationScreen
import com.example.bands.R
import com.example.bands.utils.navigateTo

enum class BottomNavigationItem(val icon: Int, val navDestination: DestinationScreen) {
    CHATLIST(R.drawable.messages, DestinationScreen.ChatList),
    STATUSLIST(R.drawable.sparkles, DestinationScreen.StatusList),
    PROFILE(R.drawable.user, DestinationScreen.Profile)
}

@Composable
fun BottomNavigationMenu(
    selectedItem: BottomNavigationItem,
    navController: NavController
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(8.dp)
            .background(
                colorResource(id = R.color.chatBgColor)
            ).clip(RoundedCornerShape(8.dp))
    ) {
        for (item in BottomNavigationItem.values()) {
            Image(
                painter = painterResource(id = item.icon),
                contentDescription = "icon",
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp)
                    .weight(1f)
                    .clickable {
                        navigateTo(navController,item.navDestination.route)
                    }, colorFilter = if (item == selectedItem) ColorFilter.tint(color = Color.Black) else ColorFilter.tint(color = Color.Gray) )

        }
    }

}