package com.example.gemmathepirate

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.InputData
import com.google.ai.edge.litertlm.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MODEL_PATH = "/data/local/tmp/llm/gemma_q8_ekv4096.litertlm"
private const val TAG = "SessionViewModel"

data class LlmModelInstance(val engine: Engine, var session: Session)

class SessionViewModel(application: Application) : AndroidViewModel(application) {

    private val _instance: LlmModelInstance
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _resultText = MutableLiveData<String>()
    val resultText: LiveData<String> = _resultText

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

            _instance = LlmModelInstance(engine, session)
        } catch (e: Exception) {
            Log.d(TAG, "Exception thrown during session creation: ${e.message}")
            throw Exception("Unable to initialize SessionViewModel")
        }
    }

    override fun onCleared() {
        super.onCleared()
        cleanUp()
    }

    fun runInferenceAsync(inputText: String) {
        val inputData = mutableListOf<InputData>()
        inputData.add(InputData.Text(inputText))
        viewModelScope.launch {
            _isLoading.value = true
            val result = withContext(Dispatchers.IO) {
                _instance.session.generateContent(inputData)
            }
            updateMainThreadWithInferenceResult(result)
        }
    }

    private fun updateMainThreadWithInferenceResult(data: String) {
        _resultText.value = data
        _isLoading.value = false
    }

    private fun cleanUp() {
        try {
            _instance.session.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close the session: ${e.message}")
        }

        try {
            _instance.engine.close()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to close the engine: ${e.message}")
        }
    }
}