package com.rk.shell

import android.annotation.SuppressLint
import com.rk.libcommons.child
import java.io.File

//Note avoid using android apis as much as possible
class Installer {
    companion object{
        @JvmStatic
        fun main(args: Array<String>){
            val workingDir = "/data/local/tmp/ReTerminal"
            val downloadDir = "/sdcard/Download/ReTerminal"

            if(File(workingDir).exists().not()){
                File(workingDir).mkdirs()
            }

            val alpineDir = "$workingDir/alpine"

            moveFileIfNeeded("$downloadDir/libtalloc.so.2", "$workingDir/libtalloc.so.2")
            moveFileIfNeeded("$downloadDir/proot", "$workingDir/proot")
            extractTarIfNeeded(File(downloadDir).child("alpine.tar.gz"), File(alpineDir))

            val argsList = mutableListOf("--kill-on-exit", "-w", File(".").absolutePath)
            val systemMounts = listOf(
                "/apex", "/odm", "/product", "/system", "/system_ext", "/vendor",
                "/linkerconfig/ld.config.txt",
                "/linkerconfig/com.android.art/ld.config.txt",
                "/plat_property_contexts", "/property_contexts"
            )

            for (mnt in systemMounts) {
                val path = File(mnt)
                if (path.exists()) argsList.addAll(listOf("-b", path.absolutePath))
            }

            argsList.addAll(listOf("-b", "/sdcard", "-b", "/storage", "-b", "/dev", "-b", "/data", "-b", "/sys"))
            argsList.addAll(listOf("-b", "/dev/urandom:/dev/random", "-b", "/proc"))

            val fdMappings = listOf("0" to "/dev/stdin", "1" to "/dev/stdout", "2" to "/dev/stderr")
            for ((fd, dev) in fdMappings) {
                if (File("/proc/self/fd/$fd").exists()) argsList.addAll(listOf("-b", "/proc/self/fd/$fd:$dev"))
            }

            if (!File("$alpineDir/tmp").exists()) {
                File("$alpineDir/tmp").mkdirs()
                File("$alpineDir/tmp").setExecutable(true, false)
            }

            argsList.addAll(listOf("-b", "$alpineDir/tmp:/dev/shm", "-r", alpineDir, "-0", "--link2symlink", "--sysvipc", "-L"))
            println(argsList.joinToString(" "))
        }

        fun moveFileIfNeeded(source: String, destination: String) {
            val srcFile = File(source)
            val destFile = File(destination)
            if (!destFile.exists() && srcFile.exists()) {
                srcFile.copyTo(destFile, overwrite = true)
                destFile.setExecutable(true)
            }
        }

        fun extractTarIfNeeded(tarFile: File, destFile: File) {
            if(destFile.exists().not() || destFile.listFiles()!!.isEmpty()){
                if (destFile.exists().not()){
                    destFile.mkdirs()
                }
                executeShell("tar -xvf ${tarFile.absolutePath} -C ${destFile.absolutePath}",useShell = true)
                executeShell("mkdir -p ${destFile.child("tmp").absolutePath}")
                executeShell("chmod +x ${destFile.child("tmp").absolutePath}")
                destFile.child("etc/resolv.conf").writeText("""
                    nameserver 1.1.1.1
                    nameserver 8.8.8.8
                """.trimIndent())
                destFile.child("etc/motd").writeText("""
                    Welcome to Alpine!

                    The Alpine Wiki contains a large amount of how-to guides and general
                    information about administrating Alpine systems.
                    See <https://wiki.alpinelinux.org/>.
                    
                    Note: This reTerminal session is running with elevated permissions either as 'shell' or 'root'
                    The developer of reTerminal will no be responsible for any kind damages
                    
                    You may change this message by editing /etc/motd.
                """.trimIndent())
            }
        }

        @SuppressLint("NewApi")
        fun executeShell(command: String,useShell: Boolean = true) {
            if (useShell){
                ProcessBuilder("sh", "-c", command).inheritIO().start().waitFor()
            }else{
                ProcessBuilder(command).inheritIO().start().waitFor()
            }
        }
    }
}