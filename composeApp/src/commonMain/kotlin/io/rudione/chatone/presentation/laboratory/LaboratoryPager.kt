package io.rudione.chatone.presentation.laboratory

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.rudione.chatone.presentation.components.FitText
import chatone.composeapp.generated.resources.Res
import chatone.composeapp.generated.resources.lab_experiments
import chatone.composeapp.generated.resources.lab_overview
import chatone.composeapp.generated.resources.lab_profile
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun LaboratoryPager(content: @Composable (Int) -> Unit) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = pager.currentPage, containerColor = MaterialTheme.colorScheme.background) {
            listOf(Res.string.lab_overview, Res.string.lab_profile, Res.string.lab_experiments).forEachIndexed { index, label ->
                Tab(
                    selected = pager.currentPage == index,
                    onClick = { scope.launch { pager.animateScrollToPage(index) } },
                    modifier = Modifier.height(48.dp)
                ) {
                    FitText(stringResource(label), Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                        minFontSize = 11.sp, marquee = false)
                }
            }
        }
        HorizontalPager(state = pager, key = { it }, modifier = Modifier.weight(1f).fillMaxWidth()) { page -> content(page) }
    }
}
