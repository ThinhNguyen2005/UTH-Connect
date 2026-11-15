package com.giathinh.uthconnect.data.repository

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookImageUploadOrderTest {
    @Test
    fun `uploads all photos before saving their paths`() = runTest {
        val events = mutableListOf<String>()
        uploadBookImagesThenSave(
            listOf("first", "second"),
            upload = {
                events += "upload:$it"
                "path:$it"
            },
            save = { events += "save:${it.joinToString()}" },
        )
        assertEquals(listOf("upload:first", "upload:second", "save:path:first, path:second"), events)
    }

    @Test
    fun `upload failure prevents saving the listing`() = runTest {
        var saved = false
        val result = runCatching {
            uploadBookImagesThenSave(
                listOf("first", "second"),
                upload = { if (it == "second") error("offline") else it },
                save = { saved = true },
            )
        }
        assertTrue(result.isFailure)
        assertTrue(!saved)
    }
}
