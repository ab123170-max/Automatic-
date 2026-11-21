package com.autoswipe.ai.studio.service

import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.autoswipe.ai.studio.detection.AccessibilityNodeSnapshot
import com.autoswipe.ai.studio.detection.NodeBounds
import com.autoswipe.ai.studio.detection.ProgressRange

object AccessibilityTreeExtractor {

    private const val MAX_DEPTH = 18
    private const val MAX_NODES = 300

    fun extract(root: AccessibilityNodeInfo?): AccessibilityNodeSnapshot? {
        if (root == null) return null
        val counter = intArrayOf(0)
        return extractInternal(root, depth = 0, counter = counter)
    }

    private fun extractInternal(
        node: AccessibilityNodeInfo,
        depth: Int,
        counter: IntArray
    ): AccessibilityNodeSnapshot {
        counter[0] += 1

        val rect = Rect()
        node.getBoundsInScreen(rect)

        val range = node.rangeInfo?.let { info ->
            ProgressRange(
                current = info.current,
                min = info.min,
                max = info.max
            )
        }

        val children = if (depth < MAX_DEPTH && counter[0] < MAX_NODES) {
            val childCount = node.childCount.coerceAtMost(40)
            val list = ArrayList<AccessibilityNodeSnapshot>(childCount)
            for (i in 0 until childCount) {
                if (counter[0] >= MAX_NODES) break
                val child = try {
                    node.getChild(i)
                } catch (_: Exception) {
                    null
                } ?: continue

                try {
                    list.add(extractInternal(child, depth + 1, counter))
                } finally {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        try {
                            child.recycle()
                        } catch (_: Exception) {
                        }
                    }
                }
            }
            list
        } else {
            emptyList()
        }

        return AccessibilityNodeSnapshot(
            className = node.className?.toString().orEmpty(),
            viewIdResourceName = node.viewIdResourceName,
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            isClickable = node.isClickable,
            isVisibleToUser = node.isVisibleToUser,
            isScrollable = node.isScrollable,
            bounds = NodeBounds(rect.left, rect.top, rect.right, rect.bottom),
            progressRange = range,
            children = children
        )
    }
}
