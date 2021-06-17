// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.


package org.pytorch.demo.questionanswering

import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spannable
import android.text.style.StyleSpan
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.Module
import org.pytorch.Tensor
import java.io.*
import java.lang.Double.MAX_VALUE
import java.util.*
import java.util.regex.Pattern


internal class QAException(override var message: String) : Exception()

class MainActivity : AppCompatActivity(), Runnable {
    private var mModule: Module? = null

    private var mEditTextQuestion: EditText? = null
    private var mEditTextText: EditText? = null
    private var mTextViewAnswer: TextView? = null
    private var mButton: Button? = null

    private var mTokenIdMap: HashMap<String, Long?>? = null
    private var mIdTokenMap: HashMap<Long, String>? = null

    private val MODEL_INPUT_LENGTH = 360
    private val EXTRA_ID_NUM = 3
    private val CLS = "[CLS]"
    private val SEP = "[SEP]"
    private val PAD = "[PAD]"
    private val START_LOGITS = "start_logits"
    private val END_LOGITS = "end_logits"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        mButton = findViewById(R.id.btnAnswer)
        mEditTextText = findViewById(R.id.editTextText)
        mEditTextQuestion = findViewById(R.id.editTextQuestion)
        mTextViewAnswer = findViewById(R.id.tvAnswer)
        mEditTextText?.setText("There is a growing need to execute ML models on edge devices to reduce latency, preserve privacy and enable new interactive use cases. In the past, engineers used to train models separately. They would then go through a multi-step, error prone and often complex process to transform the models for execution on a mobile device. The mobile runtime was often significantly different from the operations available during training leading to inconsistent developer and eventually user experience. PyTorch Mobile removes these friction surfaces by allowing a seamless process to go from training to deployment by staying entirely within the PyTorch ecosystem. It provides an end-to-end workflow that simplifies the research to production environment for mobile devices. In addition, it paves the way for privacy-preserving features via Federated Learning techniques. PyTorch Mobile is in beta stage right now and in wide scale production use. It will soon be available as a stable release once the APIs are locked down. Key features of PyTorch Mobile: Available for iOS, Android and Linux; Provides APIs that cover common preprocessing and integration tasks needed for incorporating ML in mobile applications; Support for tracing and scripting via TorchScript IR; Support for XNNPACK floating point kernel libraries for Arm CPUs; Integration of QNNPACK for 8-bit quantized kernels. Includes support for per-channel quantization, dynamic quantization and more; Build level optimization and selective compilation depending on the operators needed for user applications, i.e., the final binary size of the app is determined by the actual operators the app needs; Support for hardware backends like GPU, DSP, NPU will be available soon.")
        mEditTextQuestion?.setText("What are the key features of pytorch mobile?")

        mButton?.setOnClickListener(View.OnClickListener {
            mButton?.setEnabled(false)
            val thread = Thread(this@MainActivity)
            thread.start()
        })
        try {
            val br = BufferedReader(InputStreamReader(assets.open("vocab.txt")))
            var line: String
            mTokenIdMap = HashMap()
            mIdTokenMap = HashMap()
            var count = 0L
            while (true) {
                val line = br.readLine()
                if (line != null) {
                    mTokenIdMap!![line] = count
                    mIdTokenMap!![count] = line
                    count++
                }
                else break
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(QAException::class)
    private fun tokenizer(question: String, text: String): LongArray {
        val tokenIdsQuestion = wordPieceTokenizer(question)
        if (tokenIdsQuestion.size >= MODEL_INPUT_LENGTH) throw QAException("Question too long")
        val tokenIdsText = wordPieceTokenizer(text)
        val inputLength = tokenIdsQuestion.size + tokenIdsText.size + EXTRA_ID_NUM
        val ids = LongArray(Math.min(MODEL_INPUT_LENGTH, inputLength))
        ids[0] = mTokenIdMap!![CLS]!!

        for (i in tokenIdsQuestion.indices) ids[i + 1] = tokenIdsQuestion[i]!!.toLong()
        ids[tokenIdsQuestion.size + 1] = mTokenIdMap!![SEP]!!
        val maxTextLength = Math.min(tokenIdsText.size, MODEL_INPUT_LENGTH - tokenIdsQuestion.size - EXTRA_ID_NUM)

        for (i in 0 until maxTextLength) {
            ids[tokenIdsQuestion.size + i + 2] = tokenIdsText[i]!!.toLong()
        }

        ids[tokenIdsQuestion.size + maxTextLength + 2] = mTokenIdMap!![SEP]!!
        return ids
    }

    private fun wordPieceTokenizer(questionOrText: String): List<Long?> {
        // for each token, if it's in the vocab.txt (a key in mTokenIdMap), return its Id
        // else do: a. find the largest sub-token (at least the first letter) that exists in vocab;
        // b. add "##" to the rest (even if the rest is a valid token) and get the largest sub-token "##..." that exists in vocab;
        // and c. repeat b.
        val tokenIds: MutableList<Long?> = ArrayList()
        val p = Pattern.compile("\\w+|\\S")
        val m = p.matcher(questionOrText)
        while (m.find()) {
            val token = m.group().toLowerCase()
            if (mTokenIdMap!!.containsKey(token)) tokenIds.add(mTokenIdMap!![token]) else {
                for (i in 0 until token.length) {
                    if (mTokenIdMap!!.containsKey(token.substring(0, token.length - i - 1))) {
                        tokenIds.add(mTokenIdMap!![token.substring(0, token.length - i - 1)])
                        var subToken = token.substring(token.length - i - 1)
                        var j = 0

                        while (j < subToken.length) {
                            if (mTokenIdMap!!.containsKey("##" + subToken.substring(0, subToken.length - j))) {
                                tokenIds.add(mTokenIdMap!!["##" + subToken.substring(0, subToken.length - j)])
                                subToken = subToken.substring(subToken.length - j)
                                j = subToken.length - j
                            } else if (j == subToken.length - 1) {
                                tokenIds.add(mTokenIdMap!!["##$subToken"])
                                break
                            } else j++
                        }
                        break
                    }
                }
            }
        }
        return tokenIds
    }

    override fun run() {
        val result = answer(mEditTextQuestion!!.text.toString(), mEditTextText!!.text.toString())
        runOnUiThread { mButton!!.isEnabled = true }
        if (result == null) return

        runOnUiThread {
            val imm = this.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
            var view = currentFocus
            if (view == null) view = View(this)
            imm.hideSoftInputFromWindow(view.windowToken, 0)

            val startIdx = mEditTextText!!.text.toString().toLowerCase().indexOf(result)
            if (startIdx == -1) {
                mTextViewAnswer!!.text = "Beat me!"
                return@runOnUiThread
            }

            mTextViewAnswer!!.text = result
            mEditTextText!!.setText(mEditTextText!!.text.toString()) // remove previous answer in bold
            mEditTextText!!.setSelection(startIdx, startIdx + result.length)
            val boldSpan = StyleSpan(Typeface.BOLD)
            val startSel = mEditTextText!!.selectionStart
            val endSel = mEditTextText!!.selectionEnd
            val flag = Spannable.SPAN_INCLUSIVE_INCLUSIVE
            mEditTextText!!.text.setSpan(boldSpan, startSel, endSel, flag)
        }
    }

    fun assetFilePath(context: Context, assetName: String?): String? {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        context.assets.open(assetName!!).use { `is` ->
            FileOutputStream(file).use { os ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (`is`.read(buffer).also { read = it } != -1) {
                    os.write(buffer, 0, read)
                }
                os.flush()
            }
            return file.absolutePath
        }
    }

    private fun answer(question: String, text: String): String? {
        if (mModule == null) {
            mModule = LiteModuleLoader.load(this.assetFilePath(this, "qa360_quantized.ptl"))
        }

        try {
            val tokenIds = tokenizer(question, text)
            val inTensorBuffer = Tensor.allocateLongBuffer(MODEL_INPUT_LENGTH)
            for (n in tokenIds) inTensorBuffer.put(n.toLong())
            for (i in 0 until MODEL_INPUT_LENGTH - tokenIds.size) mTokenIdMap!![PAD]?.let { inTensorBuffer.put(it) }

            val inTensor = Tensor.fromBlob(inTensorBuffer, longArrayOf(1, MODEL_INPUT_LENGTH.toLong()))
            val outTensors = mModule!!.forward(IValue.from(inTensor)).toDictStringKey()
            val startTensor = outTensors[START_LOGITS]!!.toTensor()
            val endTensor = outTensors[END_LOGITS]!!.toTensor()

            val starts = startTensor.dataAsFloatArray
            val ends = endTensor.dataAsFloatArray
            val answerTokens: MutableList<String?> = ArrayList()
            val start = argmax(starts)
            val end = argmax(ends)
            for (i in start until end + 1) answerTokens.add(mIdTokenMap!![tokenIds[i]])

            return java.lang.String.join(" ", answerTokens).replace(" ##".toRegex(), "").replace("\\s+(?=\\p{Punct})".toRegex(), "")
        } catch (e: QAException) {
            runOnUiThread { mTextViewAnswer!!.text = e.message }
        }
        return null
    }

    private fun argmax(array: FloatArray): Int {
        var maxIdx = 0
        var maxVal: Double = -MAX_VALUE
        for (j in array.indices) {
            if (array[j] > maxVal) {
                maxVal = array[j].toDouble()
                maxIdx = j
            }
        }
        return maxIdx
    }
}