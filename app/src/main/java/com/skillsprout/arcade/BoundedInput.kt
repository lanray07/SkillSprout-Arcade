package com.skillsprout.arcade

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

internal object PackReadLimits {
    const val PAYLOAD=5_000_000
    const val SIGNED=7_000_000
    const val MANIFEST=65_536
    const val PUBLIC_KEY=65_536
    const val DIGEST=64
}

/** Reads at most limit + one detection byte. The caller owns and closes the stream. */
internal fun InputStream.readBytesBounded(limit:Int,checkActive:()->Unit={}):ByteArray {
    require(limit>=0 && limit<Int.MAX_VALUE)
    val output=ByteArrayOutputStream();val buffer=ByteArray(8192)
    while(true) {
        checkActive()
        val count=read(buffer,0,minOf(buffer.size,limit-output.size()+1))
        if(count<0) break
        if(count==0) {
            checkActive()
            val one=read();if(one<0) break
            if(output.size()==limit) throw IOException("Content input exceeds size limit")
            output.write(one)
        } else {
            if(output.size()+count>limit) throw IOException("Content input exceeds size limit")
            output.write(buffer,0,count)
        }
    }
    checkActive()
    return output.toByteArray()
}
