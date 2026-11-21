package com.autoswipe.ai.studio.detection

data class ProgressRange(
    val current: Float,
    val min: Float,
    val max: Float
) {
    val ratio: Float?
        get() {
            val span = max - min
            if (span <= 0f) return null
            return ((current - min) / span).coerceIn(0f, 1f)
        }
}

data class NodeBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
}

data class AccessibilityNodeSnapshot(
    val className: String = "",
    val viewIdResourceName: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val isClickable: Boolean = false,
    val isVisibleToUser: Boolean = true,
    val isScrollable: Boolean = false,
    val bounds: NodeBounds = NodeBounds(0, 0, 0, 0),
    val progressRange: ProgressRange? = null,
    val children: List<AccessibilityNodeSnapshot> = emptyList()
) {
    fun flatten(maxNodes: Int = 350): List<AccessibilityNodeSnapshot> {
        val result = ArrayList<AccessibilityNodeSnapshot>(64)
        val queue = ArrayDeque<AccessibilityNodeSnapshot>()
        queue.add(this)
        while (queue.isNotEmpty() && result.size < maxNodes) {
            val node = queue.removeFirst()
            result.add(node)
            for (child in node.children) {
                if (result.size + queue.size >= maxNodes) break
                queue.add(child)
            }
        }
        return result
    }
}
