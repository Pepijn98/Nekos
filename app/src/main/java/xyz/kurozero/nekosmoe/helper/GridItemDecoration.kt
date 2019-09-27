package xyz.kurozero.nekosmoe.helper

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class GridItemDecoration(gridSpacingPx: Int, gridSize: Int) : RecyclerView.ItemDecoration() {
    private var mSizeGridSpacingPx: Int = gridSpacingPx
    private var mGridSize: Int = gridSize
    private var mNeedLeftSpacing = false

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val frameWidth = ((parent.width - mSizeGridSpacingPx.toFloat() * (mGridSize - 1)) / mGridSize).toInt()
        val padding = parent.width / mGridSize - frameWidth
        val itemPosition = (view.layoutParams as RecyclerView.LayoutParams).viewAdapterPosition
        with(outRect) {
            top = mSizeGridSpacingPx
            when {
                itemPosition % mGridSize == 0 -> {
                    left = 0
                    right = padding
                    mNeedLeftSpacing = true
                }
                (itemPosition + 1) % mGridSize == 0 -> {
                    mNeedLeftSpacing = false
                    right = 0
                    left = padding
                }
                mNeedLeftSpacing -> {
                    mNeedLeftSpacing = false
                    left = mSizeGridSpacingPx - padding
                    right = if ((itemPosition + 2) % mGridSize == 0) mSizeGridSpacingPx - padding else mSizeGridSpacingPx / 2
                }
                (itemPosition + 2) % mGridSize == 0 -> {
                    mNeedLeftSpacing = false
                    left = mSizeGridSpacingPx / 2
                    right = mSizeGridSpacingPx - padding
                }
                else -> {
                    mNeedLeftSpacing = false
                    left = mSizeGridSpacingPx / 2
                    right = mSizeGridSpacingPx / 2
                }
            }
            bottom = 0
        }
    }
}