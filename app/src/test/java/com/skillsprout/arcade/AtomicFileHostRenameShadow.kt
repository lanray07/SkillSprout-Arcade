package com.skillsprout.arcade

import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

/** API 29+ AtomicFile uses File.renameTo; Windows refuses replacement of an existing
 * target. Model Android's same-filesystem POSIX replacement using the host NIO call.
 * Production code and physical-device tests use the real Android implementation. */
@Implements(value=AtomicFile::class,minSdk=29,callThroughByDefault=true)
class AtomicFileHostRenameShadow {
    companion object {
        @JvmStatic @Implementation
        fun rename(source:File,target:File) {
            if(target.isDirectory && !target.delete()) {Log.e("AtomicFile","Could not remove test target directory");return}
            try {Files.move(source.toPath(),target.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING)}
            catch(error:IOException) {Log.e("AtomicFile","Could not replace test target",error)}
        }
    }
}
