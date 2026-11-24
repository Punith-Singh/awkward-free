package com.punith.awkwardfree.model

/**
 * Undo only records that this action changed or removed. Added recurring occurrences
 * are not removed. If any affected record changed later, refuse the entire undo.
 */
internal class DueUndo private constructor(
    private val original: List<Due>,
    private val expected: Map<String, Due?>
) {
    fun restore(current: List<Due>): List<Due>? {
        val byId = current.associateBy { it.id }
        if (expected.any { (id, after) -> byId[id] != after }) return null
        val changedIds = expected.keys
        val originals = original.filter { it.id in changedIds }.associateBy { it.id }
        val restored = current.map { originals[it.id] ?: it }.toMutableList()
        original.forEachIndexed { index, due ->
            if (due.id in changedIds && restored.none { it.id == due.id }) {
                restored.add(index.coerceAtMost(restored.size), due)
            }
        }
        return restored
    }

    companion object {
        fun between(before: List<Due>, after: List<Due>): DueUndo? {
            val byId = after.associateBy { it.id }
            val changed = before.filter { byId[it.id] != it }
            if (changed.isEmpty()) return null
            return DueUndo(before, changed.associate { it.id to byId[it.id] })
        }
    }
}
