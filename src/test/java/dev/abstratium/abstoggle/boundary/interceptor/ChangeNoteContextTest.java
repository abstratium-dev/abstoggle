package dev.abstratium.abstoggle.boundary.interceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChangeNoteContextTest {

    @Test
    void testGetChangeNote_initiallyNull() {
        ChangeNoteContext context = new ChangeNoteContext();
        assertNull(context.getChangeNote());
    }

    @Test
    void testSetChangeNote() {
        ChangeNoteContext context = new ChangeNoteContext();
        context.setChangeNote("test change note");
        assertEquals("test change note", context.getChangeNote());
    }

    @Test
    void testHasChangeNote_returnsFalse_whenNull() {
        ChangeNoteContext context = new ChangeNoteContext();
        assertFalse(context.hasChangeNote());
    }

    @Test
    void testHasChangeNote_returnsFalse_whenEmpty() {
        ChangeNoteContext context = new ChangeNoteContext();
        context.setChangeNote("");
        assertFalse(context.hasChangeNote());
    }

    @Test
    void testHasChangeNote_returnsFalse_whenBlank() {
        ChangeNoteContext context = new ChangeNoteContext();
        context.setChangeNote("   ");
        assertFalse(context.hasChangeNote());
    }

    @Test
    void testHasChangeNote_returnsTrue_whenPresent() {
        ChangeNoteContext context = new ChangeNoteContext();
        context.setChangeNote("valid change note");
        assertTrue(context.hasChangeNote());
    }

    @Test
    void testSetChangeNote_null() {
        ChangeNoteContext context = new ChangeNoteContext();
        context.setChangeNote("test");
        context.setChangeNote(null);
        assertNull(context.getChangeNote());
        assertFalse(context.hasChangeNote());
    }
}
