package com.prasannaone.deepcalc

/** Insert a fresh stable identity without renumbering existing sheet rows. */
internal fun insertStableRowId(ids: List<Long>, index: Int, newId: Long): List<Long> =
    ids.toMutableList().apply { add(index.coerceIn(0, size), newId) }

/** Remove one row identity, replacing the final row with a fresh identity if needed. */
internal fun removeStableRowId(ids: List<Long>, index: Int, replacementId: Long): List<Long> =
    if (ids.size <= 1) {
        listOf(replacementId)
    } else {
        ids.filterIndexed { currentIndex, _ -> currentIndex != index.coerceIn(0, ids.lastIndex) }
    }
