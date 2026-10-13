package com.erhanemir.btterminal

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erhanemir.btterminal.databinding.FragmentKeypadBinding
import com.google.android.material.button.MaterialButton

/** Tek keypad ekrani: 4x4 / 3x3 / Ozel secmeli. */
class KeypadFragment : Fragment() {

    private var b: FragmentKeypadBinding? = null
    private val customItems = mutableListOf<Pair<String, String>>()
    private lateinit var customAdapter: CustomAdapter

    private val labels44 = arrayOf("1", "2", "3", "A", "4", "5", "6", "B", "7", "8", "9", "C", "*", "0", "#", "D")
    private val labels33 = arrayOf("1", "2", "3", "4", "5", "6", "7", "8", "9")

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val binding = FragmentKeypadBinding.inflate(inf, c, false)
        b = binding

        customItems.addAll(Prefs.loadCustom(requireContext()))
        customAdapter = CustomAdapter(customItems,
            onClick = { value -> sendValue(value) },
            onLongClick = { pos -> editOrDeleteDialog(pos) })
        binding.recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recycler.adapter = customAdapter
        binding.addBtn.setOnClickListener { addDialog() }

        binding.modeToggle.check(R.id.mode44)
        binding.modeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.mode44 -> showGrid(labels44, 4)
                R.id.mode33 -> showGrid(labels33, 3)
                R.id.modeCustom -> showCustom()
            }
        }
        showGrid(labels44, 4)
        CmdHistory.onChange = { refreshMini() }
        refreshMini()
        return binding.root
    }

    private fun refreshMini() {
        val binding = b ?: return
        binding.rxMini.text = CmdHistory.rx.joinToString("\n").ifEmpty { "—" }
        binding.txMini.text = CmdHistory.tx.joinToString("\n").ifEmpty { "—" }
    }

    private fun showGrid(labels: Array<String>, cols: Int) {
        val binding = b ?: return
        binding.gridScroll.visibility = View.VISIBLE
        binding.customBox.visibility = View.GONE
        binding.grid.removeAllViews()
        binding.grid.columnCount = cols
        for (label in labels) {
            binding.grid.addView(makeKeyButton(label), gridParams())
        }
    }

    private fun showCustom() {
        val binding = b ?: return
        binding.gridScroll.visibility = View.GONE
        binding.customBox.visibility = View.VISIBLE
    }

    private fun gridParams(): GridLayout.LayoutParams =
        GridLayout.LayoutParams(
            GridLayout.spec(GridLayout.UNDEFINED, 1f),
            GridLayout.spec(GridLayout.UNDEFINED, 1f)
        ).apply {
            width = 0
            setMargins(8, 8, 8, 8)
        }

    private fun makeKeyButton(label: String): MaterialButton =
        MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            text = label
            textSize = 22f
            isSingleLine = true
            ellipsize = android.text.TextUtils.TruncateAt.END
            minHeight = 170
            cornerRadius = 28
            strokeWidth = 3
            strokeColor = androidx.appcompat.content.res.AppCompatResources.getColorStateList(context, R.color.neon)
            setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.text_main))
            setOnClickListener { v ->
                v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(60)
                    .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(60).start() }
                    .start()
                sendValue(label)
            }
        }

    private fun sendValue(value: String) {
        val ok = BtManager.send(Prefs.withNewline(requireContext(), value))
        if (!ok) Toast.makeText(requireContext(), "Once baglan", Toast.LENGTH_SHORT).show()
        else CmdHistory.pushTx(value)
    }

    private fun persist() = Prefs.saveCustom(requireContext(), customItems)

    private fun addDialog() {
        val ctx = requireContext()
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val labelIn = EditText(ctx).apply { hint = "Buton adi (orn: LED Ac)" }
        val valueIn = EditText(ctx).apply { hint = "Gonderilecek (orn: 1)" }
        box.addView(labelIn)
        box.addView(valueIn)
        AlertDialog.Builder(ctx)
            .setTitle("Buton ekle")
            .setView(box)
            .setPositiveButton("Ekle") { _, _ ->
                val value = valueIn.text.toString()
                if (value.isEmpty()) {
                    Toast.makeText(ctx, "Gonderilecek deger bos olamaz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                customItems.add(labelIn.text.toString().ifEmpty { value } to value)
                persist()
                customAdapter.notifyItemInserted(customItems.size - 1)
            }
            .setNegativeButton("Vazgec", null)
            .show()
    }

    private fun editOrDeleteDialog(pos: Int) {
        val ctx = requireContext()
        val (label, value) = customItems[pos]
        AlertDialog.Builder(ctx)
            .setTitle(label)
            .setMessage("Gonderir: $value")
            .setPositiveButton("Gonder") { _, _ -> sendValue(value) }
            .setNeutralButton("Sil") { _, _ ->
                customItems.removeAt(pos)
                persist()
                customAdapter.notifyItemRemoved(pos)
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    override fun onDestroyView() {
        if (CmdHistory.onChange != null) CmdHistory.onChange = null
        b = null
        super.onDestroyView()
    }

    private class CustomAdapter(
        private val items: List<Pair<String, String>>,
        private val onClick: (String) -> Unit,
        private val onLongClick: (Int) -> Unit
    ) : RecyclerView.Adapter<CustomAdapter.H>() {

        class H(v: View) : RecyclerView.ViewHolder(v) {
            val btn: MaterialButton = v.findViewById(R.id.itemBtn)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): H {
            val v = LayoutInflater.from(p.context).inflate(R.layout.item_button, p, false)
            return H(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: H, pos: Int) {
            h.btn.text = items[pos].first
            h.btn.setOnClickListener { v ->
                v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(60)
                    .withEndAction { v.animate().scaleX(1f).scaleY(1f).setDuration(60).start() }
                    .start()
                onClick(items[h.bindingAdapterPosition].second)
            }
            h.btn.setOnLongClickListener {
                onLongClick(h.bindingAdapterPosition)
                true
            }
        }
    }
}
