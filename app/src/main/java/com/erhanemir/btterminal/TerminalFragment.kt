package com.erhanemir.btterminal

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ScrollView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.erhanemir.btterminal.databinding.FragmentTerminalBinding

class TerminalFragment : Fragment() {

    private var b: FragmentTerminalBinding? = null
    private var hexMode = false

    override fun onCreateView(inf: LayoutInflater, c: ViewGroup?, s: Bundle?): View {
        val binding = FragmentTerminalBinding.inflate(inf, c, false)
        b = binding
        val act = activity as? MainActivity
        binding.logText.text = act?.logBuffer?.toString() ?: ""
        binding.newlineCb.isChecked = Prefs.newlineEnabled(requireContext())
        binding.newlineCb.setOnCheckedChangeListener { _, v ->
            Prefs.setNewlineEnabled(requireContext(), v)
        }
        binding.hexCb.setOnCheckedChangeListener { _, v -> hexMode = v }
        binding.sendBtn.setOnClickListener { doSend() }
        // Klavyedeki "gonder" (enter) tusu da gondersin, klavye kapanmasin
        binding.inputText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                doSend()
                true
            } else false
        }
        binding.clearBtn.setOnClickListener {
            act?.logBuffer?.clear()
            binding.logText.text = ""
        }
        binding.copyBtn.setOnClickListener {
            val cm = requireContext().getSystemService(ClipboardManager::class.java)
            cm?.setPrimaryClip(ClipData.newPlainText("bt-log", binding.logText.text))
            Toast.makeText(requireContext(), "Log kopyalandi", Toast.LENGTH_SHORT).show()
        }
        binding.logText.post { scrollDown() }
        binding.versionText.text = "BT Terminal v" + UpdateManager.currentVersion(requireContext())
        return binding.root
    }

    private fun doSend() {
        val binding = b ?: return
        val msg = binding.inputText.text.toString()
        if (msg.isEmpty()) return
        val ok = BtManager.send(Prefs.withNewline(requireContext(), msg))
        if (ok) {
            binding.inputText.text.clear()
            CmdHistory.pushTx(msg)
            appendText(">> $msg\n")
        } else {
            Toast.makeText(requireContext(), "Once baglan", Toast.LENGTH_SHORT).show()
        }
        // Klavyeyi acik tut: odagi geri al (butonlar focusable=false oldugu icin odak zaten kutuda).
        // Tek sefer yetmezse diye iki kademeli dene.
        binding.inputText.requestFocus()
        val imm = requireContext().getSystemService(InputMethodManager::class.java)
        binding.inputText.postDelayed({
            if (b == null) return@postDelayed
            binding.inputText.requestFocus()
            imm?.showSoftInput(binding.inputText, InputMethodManager.SHOW_IMPLICIT)
        }, 100)
        binding.inputText.postDelayed({
            if (b == null) return@postDelayed
            if (!imm!!.isActive(binding.inputText)) {
                binding.inputText.requestFocus()
                imm.showSoftInput(binding.inputText, InputMethodManager.SHOW_IMPLICIT)
            }
        }, 400)
    }

    fun appendText(t: String) {
        val binding = b ?: return
        val shown = if (hexMode) {
            t.toByteArray(Charsets.UTF_8).joinToString(" ") { "%02X".format(it) } + "\n"
        } else t
        binding.logText.append(shown)
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
