package com.giathinh.uthconnect

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.giathinh.uthconnect.domain.model.BookCondition
import com.giathinh.uthconnect.domain.model.BookDealType
import com.giathinh.uthconnect.domain.model.BookListing
import com.giathinh.uthconnect.presentation.books.BooksScreen
import com.giathinh.uthconnect.presentation.books.BooksUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BooksContactUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun contactFailureShowsError() {
        var opened = ""
        val handler = object : UriHandler {
            override fun openUri(uri: String) { opened = uri; throw IllegalStateException("No handler") }
        }
        val listing = BookListing("qa", "QA contact book", "QA", BookCondition.GOOD,
            BookDealType.SALE, 1000, "", "Seller", "qa@example.com", ownerUid = "seller")
        compose.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                BooksScreen(BooksUiState(isLoading = false, currentUserUid = "buyer", listings = listOf(listing)), {}, {})
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onAllNodes(hasScrollAction())[0].performScrollToNode(hasText(listing.title))
        compose.onNodeWithText(listing.title).performClick()
        compose.onNodeWithText(context.getString(R.string.books_contact_seller)).performClick()
        compose.onNodeWithText(context.getString(R.string.books_contact_failed_title)).assertExists()
        compose.runOnIdle { assertEquals("mailto:qa%40example.com", opened) }
    }
}
