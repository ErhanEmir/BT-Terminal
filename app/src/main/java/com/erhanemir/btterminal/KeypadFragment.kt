package com.erhanemir.btterminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.erhanemir.btterminal.databinding.FragmentKeypadBinding

class KeypadFragment : Fragment() {

    companion object {
        private const val A_LABELS = "labels"
        private const val A_COLS = "cols"
        fun newInstance(labels: Array<String>, cols: Int) = KeypadFragment().apply {
            arguments = Bundle().apply {
                putStringArray(A_LABELS, labels)
                putInt(A_COLS, cols)
            }
        }
    }

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val binding = FragmentKeypadBinding.inflate(inf, c, false)
        val labels = requireArguments().getStringArray(A_LABELS) ?: emptyArray()
        val cols = requireArguments().getInt(A_COLS, 4)
        binding.grid.columnCount = cols
        for (label in labels) {
            val btn = Button(requireContext()).apply {
                text = label
                textSize = 22f
                minHeight = 170
                setOnClickListener { onKey(label) }
            }
            val p = GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED, 1f),
                GridLayout.spec(GridLayout.UNDEFINED, 1f)
            ).apply {
                width = 0
                setMargins(8, 8, 8, 8)
            }
            binding.grid.addView(btn, p)
        }
        return binding.root
    }

    private fun onKey(label: String) {
        val ok = BtManager.send(Prefs.withNewline(requireContext(), label))
        if (!ok) Toast.makeText(requireContext(), "Once baglan", Toast.LENGTH_SHORT).show()
    }
}
