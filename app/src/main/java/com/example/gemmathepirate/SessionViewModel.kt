package com.example.gemmathepirate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.Session

private const val MODEL_PATH = "/data/local/tmp/llm/gemma_q8_ekv4096.litertlm"
private const val TAG = "SessionViewModel"

data class LlmModelInstance(val engine: Engine, var session: Session)

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private var instance: LlmModelInstance

    init {
        val engineConfig = EngineConfig(
            modelPath = MODEL_PATH,
            cacheDir = application.getExternalFilesDir(null)!!.path
        )

        try {
            val engine = Engine(engineConfig)
            engine.initialize()
            val session = engine.createSession()
            Log.d(TAG, "Session created")

            instance = LlmModelInstance(engine, session)
        } catch (e: Exception) {
            Log.d(TAG, "Exception thrown during session creation: ${e.message}")
            throw Exception("Unable to initialize SessionViewModel")
        }
    }

    suspend fun startInference(inputText: String): String {
        val inputData = mutableListOf<InputData>()
        inputData.add(InputData.Text(inputText))
        return instance.session.generateContent(inputData)
    }

    fun cleanUp() {
        try {
            instance.session.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close the session: ${e.message}")
        }

        try {
            instance.engine.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close the engine: ${e.message}")
        }
    }
}