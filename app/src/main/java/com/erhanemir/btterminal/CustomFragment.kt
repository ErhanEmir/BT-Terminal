package com.erhanemir.btterminal

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erhanemir.btterminal.databinding.FragmentCustomBinding

class CustomFragment : Fragment() {

    private var b: FragmentCustomBinding? = null
    private val items = mutableListOf<Pair<String, String>>()
    private lateinit var adapter: CustomAdapter

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val binding = FragmentCustomBinding.inflate(inf, c, false)
        b = binding
        items.addAll(Prefs.loadCustom(requireContext()))
        adapter = CustomAdapter(items,
            onClick = { value ->
                val ok = BtManager.send(Prefs.withNewline(requireContext(), value))
                if (!ok) Toast.makeText(requireContext(), "Once baglan", Toast.LENGTH_SHORT).show()
            },
            onLongClick = { pos -> editOrDeleteDialog(pos) })
        binding.recycler.layoutManager = GridLayoutManager(requireContext(), 3)
        binding.recycler.adapter = adapter
        binding.addBtn.setOnClickListener { addDialog() }
        return binding.root
    }

    private fun persist() = Prefs.saveCustom(requireContext(), items)

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
                val label = labelIn.text.toString().ifEmpty { valueIn.text.toString() }
                val value = valueIn.text.toString()
                if (value.isEmpty()) {
                    Toast.makeText(ctx, "Gonderilecek deger bos olamaz", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                items.add(label to value)
                persist()
                adapter.notifyItemInserted(items.size - 1)
            }
            .setNegativeButton("Vazgec", null)
            .show()
    }

    private fun editOrDeleteDialog(pos: Int) {
        val ctx = requireContext()
        val (label, value) = items[pos]
        AlertDialog.Builder(ctx)
            .setTitle(label)
            .setMessage("Gonderir: $value")
            .setPositiveButton("Gonder") { _, _ ->
                val ok = BtManager.send(Prefs.withNewline(ctx, value))
                if (!ok) Toast.makeText(ctx, "Once baglan", Toast.LENGTH_SHORT).show()
            }
            .setNeutralButton("Sil") { _, _ ->
                items.removeAt(pos)
                persist()
                adapter.notifyItemRemoved(pos)
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    override fun onDestroyView() {
        b = null
        super.onDestroyView()
    }

    private class CustomAdapter(
        private val items: List<Pair<String, String>>,
        private val onClick: (String) -> Unit,
        private val onLongClick: (Int) -> Unit
    ) : RecyclerView.Adapter<CustomAdapter.H>() {

        class H(v: View) : RecyclerView.ViewHolder(v) {
            val btn: Button = v.findViewById(R.id.itemBtn)
        }

        override fun onCreateViewHolder(p: ViewGroup, t: Int): H {
            val v = LayoutInflater.from(p.context).inflate(R.layout.item_button, p, false)
            return H(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(h: H, pos: Int) {
            h.btn.text = items[pos].first
            h.btn.setOnClickListener { onClick(items[h.bindingAdapterPosition].second) }
            h.btn.setOnLongClickListener {
                onLongClick(h.bindingAdapterPosition)
                true
            }
        }
    }
}
