package com.skillsprout.billing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Requires a deployed publisher HTTPS endpoint; never sends child learning records. */
class HttpsReceiptVerifier(private val endpoint:URL):ReceiptVerifier {
    init {require(endpoint.protocol=="https" && endpoint.userInfo==null && endpoint.ref==null && endpoint.query==null) {"Use a credential-free HTTPS verification endpoint"}}
    override suspend fun verify(amazonUserId:String,receiptId:String,sku:String):SignedAccess=withContext(Dispatchers.IO) {
        val connection=endpoint.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout=8000;connection.readTimeout=10000;connection.instanceFollowRedirects=false
            connection.requestMethod="POST";connection.doOutput=true
            connection.setRequestProperty("Content-Type","application/json");connection.setRequestProperty("Accept","application/json")
            val bytes=JSONObject().put("userId",amazonUserId).put("receiptId",receiptId).put("sku",sku).toString().toByteArray(Charsets.UTF_8)
            require(bytes.size<=8192)
            connection.setFixedLengthStreamingMode(bytes.size)
            connection.outputStream.use {it.write(bytes)}
            if(connection.responseCode==403) throw ReceiptInactiveException()
            check(connection.responseCode==200) {"Receipt service unavailable"}
            val output=java.io.ByteArrayOutputStream()
            connection.inputStream.use {stream->
                val buffer=ByteArray(4096)
                while(true) {val count=stream.read(buffer);if(count<0)break;require(output.size()+count<=16384);output.write(buffer,0,count)}
            }
            val result=JSONObject(output.toString("UTF-8"))
            SignedAccess(result.getString("payloadBase64"),result.getString("signatureBase64"))
        }finally {connection.disconnect()}
    }
}
