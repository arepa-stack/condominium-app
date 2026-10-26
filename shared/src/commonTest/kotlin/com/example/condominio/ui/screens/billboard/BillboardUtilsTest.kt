package com.example.condominio.ui.screens.billboard

import androidx.compose.ui.platform.UriHandler
import com.example.condominio.data.model.AnnouncementCategory
import com.example.condominio.ui.components.announcementCategoryLabelRes
import com.example.condominio.ui.components.formatDecisionDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BillboardUtilsTest {
    // --- E-C1: tipo de adjunto ---

    @Test fun `known image extensions are images`() {
        assertTrue(isImage("https://x.co/a/foto.JPG?token=1"))
        assertTrue(isImage("https://x.co/a/foto.png"))
    }

    @Test fun `url without extension is not treated as an image`() {
        assertFalse(isImage("https://x.co/storage/3f2a9c10"))
    }

    @Test fun `pdf and other files are not images`() {
        assertFalse(isImage("https://x.co/a/acta.pdf?token=1"))
        assertFalse(isImage("https://x.co/a/datos.xlsx"))
        assertTrue(isPdf("https://x.co/a/acta.PDF?token=1"))
    }

    @Test fun `file name is percent decoded`() {
        assertEquals("Acta de asamblea.pdf", attachmentFileName("https://x.co/a/Acta%20de%20asamblea.pdf?token=abc"))
        assertEquals("Reunión.pdf", attachmentFileName("https://x.co/a/Reuni%C3%B3n.pdf"))
    }

    @Test fun `file name stays as is when the escape is malformed or absent`() {
        assertEquals("100%.pdf", attachmentFileName("https://x.co/a/100%.pdf"))
        assertEquals("plano.pdf", attachmentFileName("https://x.co/a/plano.pdf"))
        assertEquals("", attachmentFileName("https://x.co/a/"))
    }

    // --- APP-M3: solo http/https ---

    @Test fun `only http and https links are safe`() {
        assertTrue(isSafeExternalUrl("https://example.com/a"))
        assertTrue(isSafeExternalUrl("  HTTP://example.com"))
        assertFalse(isSafeExternalUrl("javascript:alert(1)"))
        assertFalse(isSafeExternalUrl("file:///etc/passwd"))
        assertFalse(isSafeExternalUrl("intent://scan#Intent;end"))
        assertFalse(isSafeExternalUrl("mailto:a@b.c"))
        assertFalse(isSafeExternalUrl(""))
    }

    @Test fun `safe handler forwards only safe links`() {
        val opened = mutableListOf<String>()
        val base = object : UriHandler { override fun openUri(uri: String) { opened += uri } }
        val safe = safeUriHandler(base)
        safe.openUri("javascript:alert(1)")
        safe.openUri("https://example.com")
        assertEquals(listOf("https://example.com"), opened)
    }

    // --- C-01: etiquetas de categoria por recurso ---

    @Test fun `every category has its own label resource`() {
        val all = AnnouncementCategory.entries.map { announcementCategoryLabelRes(it) }
        assertEquals(AnnouncementCategory.entries.size, all.toSet().size)
    }

    // --- C-08: fechas dd/MM/yyyy ---

    @Test fun `announcement dates are dd MM yyyy`() =
        assertEquals("05/03/2026", formatDecisionDate("2026-03-05T12:00:00Z", TimeZone.UTC))

    @Test fun `date uses the given zone`() =
        assertEquals("06/03/2026", formatDecisionDate("2026-03-05T23:30:00Z", TimeZone.of("Asia/Tokyo")))
}
