package com.giathinh.uthconnect.data.repository

import com.giathinh.uthconnect.data.di.AuthDataModule
import com.giathinh.uthconnect.domain.model.NotificationCategory
import com.giathinh.uthconnect.domain.model.PostNotification
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.inject.Provider

class DemoDataRepositoryTest {
    @Test fun productionBindingUsesFirebaseWithoutInitializingSdkOrDemo() {
        val source = SdkFirebaseAuthSource(
            Provider { error("Binding must not eagerly initialize Firebase Auth") },
            Provider { error("Binding must not eagerly initialize Firestore") },
        )
        val firebase = FirebaseAuthRepository(source)
        val selected = AuthDataModule.provideAuth(Provider { firebase })
        assertSame(firebase, selected)
    }

    @Test fun scheduleFiltersByDateAndHandlesEmptyDay() = runTest {
        val repository = FakeScheduleRepository()
        val schedule = repository.getScheduleByDate("2026-11-03").first()
        assertEquals(2, schedule.size)
        assertTrue(schedule.all { it.date == "2026-11-03" })
        assertTrue(repository.getScheduleByDate("2099-01-01").first().isEmpty())
    }

    @Test fun feedFiltersAndUnknownDetailsAreEmpty() = runTest {
        val repository = FakeNotificationRepository()
        val feed = repository.getNewsfeed(NotificationCategory.ALL).first()
        assertTrue(feed.isNotEmpty())
        assertEquals(feed.sortedWith(
            compareByDescending<PostNotification> { it.isPinned }.thenByDescending { it.publishedAt }
        ), feed)
        val category = feed.first().category
        assertEquals(feed.filter { it.category == category }, repository.getNewsfeed(category).first())
        assertNull(repository.getNotificationDetail("missing").getOrThrow())
    }

    @Test fun profileUpdatesAreVisibleAndUnknownUidIsEmpty() = runTest {
        val repository = FakeUserRepository()
        val user = repository.getCurrentUser().first()!!
        val updated = user.copy(displayName = "Tên đã cập nhật")
        repository.updateProfile(updated).getOrThrow()
        assertEquals(updated, repository.getUserProfile(user.uid).first())
        assertEquals(updated, repository.getCurrentUser().first())
        assertNull(repository.getUserProfile("missing").first())
    }
}
