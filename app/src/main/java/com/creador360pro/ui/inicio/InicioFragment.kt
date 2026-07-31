package com.creador360pro.ui.inicio

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.creador360pro.R
import com.creador360pro.ui.editor.DesignEditorActivity
import com.creador360pro.ui.editor.VideoEditorActivity

class InicioFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_inicio, container, false)

        view.findViewById<View>(R.id.btnDiseno).setOnClickListener {
            startActivity(Intent(requireContext(), DesignEditorActivity::class.java))
        }

        view.findViewById<View>(R.id.btnVideo).setOnClickListener {
            startActivity(Intent(requireContext(), VideoEditorActivity::class.java))
        }

        return view
    }
}
