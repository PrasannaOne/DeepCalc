package com.prasannaone.deepcalc

import org.junit.Assert.assertEquals
import org.junit.Test

class SheetRowIdentityTest {
    @Test fun insertingARowDoesNotChangeExistingRowIdentities() {
        assertEquals(listOf(10L, 40L, 20L, 30L), insertStableRowId(listOf(10L, 20L, 30L), 1, 40L))
    }

    @Test fun deletingARowKeepsNeighbourIdentitiesStable() {
        assertEquals(listOf(10L, 30L), removeStableRowId(listOf(10L, 20L, 30L), 1, 99L))
    }

    @Test fun deletingTheOnlyRowCreatesAFreshIdentity() {
        assertEquals(listOf(99L), removeStableRowId(listOf(10L), 0, 99L))
    }

    @Test fun rowIdentityIndicesAreSafelyClamped() {
        assertEquals(listOf(10L, 20L, 99L), insertStableRowId(listOf(10L, 20L), 50, 99L))
        assertEquals(listOf(10L, 20L), removeStableRowId(listOf(10L, 20L, 30L), 50, 99L))
    }
}
