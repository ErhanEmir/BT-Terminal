package com.erhanemir.btterminal

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.erhanemir.btterminal.databinding.FragmentTerminalBinding

class TerminalFragment : Fragment() {

    private var b: FragmentTerminalBinding? = null

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val binding = FragmentTerminalBinding.inflate(inf, c, false)
        b = binding
        val act = activity as? MainActivity
        binding.logText.text = act?.logBuffer?.toString() ?: ""
        binding.newlineCb.isChecked = Prefs.newlineEnabled(requireContext())
        binding.newlineCb.setOnCheckedChangeListener { _, v ->
            Prefs.setNewlineEnabled(requireContext(), v)
        }
        binding.sendBtn.setOnClickListener {
            val msg = binding.inputText.text.toString()
            if (msg.isEmpty()) return@setOnClickListener
            val ok = BtManager.send(Prefs.withNewline(requireContext(), msg))
            if (ok) {
                binding.inputText.text.clear()
                appendText(">> $msg\n")
            } else {
                Toast.makeText(requireContext(), "Once baglan", Toast.LENGTH_SHORT).show()
            }
        }
        binding.clearBtn.setOnClickListener {
            act?.logBuffer?.clear()
            binding.logText.text = ""
        }
        binding.logText.post { scrollDown() }
        return binding.root
    }

    fun appendText(t: String) {
        val binding = b ?: return
        binding.logText.append(t)
        if (binding.logText.length() > 30000) {
            binding.logText.text = binding.logText.text.substring(binding.logText.length() - 30000)
        }
        scrollDown()
    }

    private fun scrollDown() {
        val binding = b ?: return
        (binding.logText.parent as? ScrollView)?.post {
            (binding.logText.parent as ScrollView).fullScroll(View.FOCUS_DOWN)
        }
    }

    override fun onDestroyView() {
        b = null
        super.onDestroyView()
    }
}
