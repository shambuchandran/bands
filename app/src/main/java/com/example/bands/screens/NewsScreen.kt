package com.example.bands.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.bands.DestinationScreen
import com.example.bands.di.NewsViewModel
import com.kwabenaberko.newsapilib.models.Article
import java.util.Locale

@Composable
fun NewsSection(newsViewModel: NewsViewModel, navController: NavController) {
    val articles by newsViewModel.article.collectAsState(emptyList())
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryBar(newsViewModel)
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(articles) {
                ArticleItem(article = it, navController = navController)
            }
        }


    }
}

@Composable
fun CategoryBar(newsViewModel: NewsViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var ifSearchExpanded by remember { mutableStateOf(false) }
    val categoryList =
        listOf("general", "business", "entertainment", "health", "science", "sports", "technology")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()), verticalAlignment = Alignment.CenterVertically
    ) {
        if (ifSearchExpanded) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                Modifier
                    .padding(1.dp)
                    .height(48.dp)
                    .border(1.dp, Color.Gray, CircleShape)
                    .clip(CircleShape), trailingIcon = {
                    IconButton(onClick = {
                        ifSearchExpanded = false
                        if (searchQuery.isNotEmpty()) {
                            newsViewModel.fetchEverythingWithQuery(searchQuery)
                        }
                    }) {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "search")
                    }
                }
            )

        } else {
            IconButton(onClick = { ifSearchExpanded = true }) {
                Icon(imageVector = Icons.Default.Search, contentDescription = "search")
            }
        }
        categoryList.forEach {
            Button(
                onClick = { newsViewModel.fetchNewsTopHeadlines(it) },
                modifier = Modifier
                    .padding(4.dp)
                    .height(48.dp)
            ) {
                Text(text = it.uppercase(Locale.ROOT))
            }
        }
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleItem(article: Article, navController: NavController) {

    Card(
        modifier = Modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = {
            navController.navigate(DestinationScreen.NewsArticleScreenRoute(article.url))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = article.urlToImage,
                contentDescription = "image",
                modifier = Modifier
                    .size(80.dp)
                    .aspectRatio(1f), contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp)
            ) {
                Text(text = article.title, fontWeight = FontWeight.Bold, maxLines = 3)
                Text(text = article.source.name, fontSize = 14.sp, maxLines = 1)

            }


        }

    }
}