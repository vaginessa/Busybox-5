package io.busybox

import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.io.IOUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset

object Busybox {
    private val path: String
        get() {
            val file = File(ContextProvider.baseContext.filesDir, "busybox")
            if (!file.exists()) FileOutputStream(file).use {
                IOUtils.copy(ContextProvider.baseContext.assets.open(file.name), it)
            }
            if (!file.canExecute() && !file.setExecutable(true))
                throw IOException("Can't init $file")
            return file.absolutePath.replace(Regex("\\s"), "\\ ")
        }

    val SU: Executor = ShellExecutor("su")
    val SH: Executor = ShellExecutor("sh")

    private class ShellExecutor(private val shell: String) : Executor {
        override fun execute(commands: Set<String>): Single<List<String>> = Single.fromCallable {
            val process = Runtime.getRuntime().exec(shell)
            process.outputStream.use { stream ->
                commands.forEach { IOUtils.write("$path $it\n", stream, Charset.defaultCharset()) }
                IOUtils.write("exit\n", stream, Charset.defaultCharset())
            }
            process.waitFor()
            val error = IOUtils.toString(process.errorStream, Charset.defaultCharset()).trim()
            if (error.isNotEmpty()) throw IOException(error)
            IOUtils.toString(process.inputStream, Charset.defaultCharset()).trim().split("\n")
        }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    }

    interface Executor {
        fun execute(commands: Set<String>): Single<List<String>>
        fun execute(command: String, vararg commands: String): Single<List<String>> {
            return execute(setOf(command) + commands)
        }
    }
}