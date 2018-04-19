package example.io.busybox

import android.graphics.Color
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.View
import io.busybox.Busybox
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var disposable: Disposable? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        execute.setOnClickListener {
            val cmd = command.text.toString()
            if (cmd.isBlank()) return@setOnClickListener
            showLoading(true)
            val executor = if (su.isChecked) Busybox.SU else Busybox.SH
            disposable?.dispose()
            disposable = executor.execute(cmd).subscribe(this::onSuccess, this::onError)
        }
    }

    private fun onSuccess(lines: List<String>) {
        output.append(lines.joinToString("\n") + "\n")
        command.text = null
        showLoading(false)
    }

    private fun onError(error: Throwable) {
        val str = SpannableString((error.message ?: error.toString()) + "\n")
        str.setSpan(ForegroundColorSpan(Color.BLUE), 0, str.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        output.append(str)
        showLoading(false)
    }

    private fun showLoading(isLoading: Boolean) {
        execute.visibility = if (isLoading) View.GONE else View.VISIBLE
        progress.visibility = if (isLoading) View.VISIBLE else View.GONE
        command.isEnabled = !isLoading
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
        disposable = null
    }
}
