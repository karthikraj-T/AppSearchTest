@file:Suppress("UnstableApiUsage")

package com.example.appsearchtest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.SeekBar
import android.widget.Toast
import androidx.appsearch.app.*
import androidx.appsearch.localstorage.LocalStorage
import com.google.common.base.Function
import com.google.common.util.concurrent.AsyncFunction
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.concurrent.Executors
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var songNameList = mutableListOf<String>()
    private var songList = mutableListOf<Song>()
    lateinit var adapter: ArrayAdapter<String>

    lateinit var sessionFuture: ListenableFuture<AppSearchSession>
    val seekBarValue = arrayOf(1,2,3,4,5,6,7,8,9,10)
    var scoreValue = 0
    var isEdit = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        sessionFuture = openDatabase()

        // set Schema
        setSchema()

        // Search
        search_btn.setOnClickListener { search(et.text.toString()) }

        // ADD
        add.setOnClickListener {
            if (isEdit.not()) {
                val song = Song(
                    singer = singer.text.toString(),
                    song = songName.text.toString(),
                    rate = scoreValue,
                    id = Random.nextInt().toString(),
                    nameSpace = "user1"
                )
                addDocument(song)
            } else {
                showToast("updated")
                isEdit = false
            }
        }
        score.max = seekBarValue.size -1
        score.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                scoreValue = seekBarValue[progress]
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        adapter = ArrayAdapter(applicationContext, android.R.layout.simple_list_item_1, songNameList)
        list.adapter = adapter
        list.setOnItemClickListener { parent, view, position, id ->
            songName.setText(songList[position].song)
            singer.setText(songList[position].singer)
            score.progress = songList[position].rate
            isEdit = true
        }
    }

    private fun openDatabase() =
        LocalStorage.createSearchSession(LocalStorage.SearchContext.Builder(this.applicationContext, "appSearchDb").build())

    private fun setSchema() {
        val setSchemaRequest = SetSchemaRequest.Builder().addDocumentClasses(Song::class.java).build()
        val setSchema = Futures.transformAsync(sessionFuture,
            AsyncFunction<AppSearchSession, SetSchemaResponse> {
                it?.setSchema(setSchemaRequest) }, executor)
    }

    private fun addDocument(document: Any){
        val putRequest = PutDocumentsRequest.Builder().addDocuments(document).build()
        val putFuture = Futures.transformAsync(sessionFuture,
            AsyncFunction<AppSearchSession, AppSearchBatchResult<String, Void>>{
                it?.put(putRequest) }, executor)

        Futures.addCallback(putFuture
            ,object :FutureCallback<AppSearchBatchResult<String, Void>> {
                override fun onSuccess(result: AppSearchBatchResult<String, Void>?) {
                    result?.let {
                        if (it.isSuccess) {
                            showToast("success")
                            Log.e("success", it.successes.keys.size.toString())
                        } else {
                            showToast("failure")
                            Log.d("failure", it.failures.keys.size.toString())
                        }
                    }
                }

                override fun onFailure(t: Throwable) {
                    t.printStackTrace()
                }
            }, executor)
    }

    private fun search(input: String) {
        songNameList.clear()
        songList.clear()

        val searchSpec = SearchSpec.Builder()
            .addFilterSchemas()
            .addFilterNamespaces("").build()
        /*object : Function<AppSearchSession, SearchResults>{
            override fun apply(i: AppSearchSession?): SearchResults? {
                i?.search(input, searchSpec)
            }
        }*/
        val searchFuture = Futures.transform(sessionFuture,
            Function<AppSearchSession, SearchResults> { i ->
                i?.search(input, searchSpec)
            }, executor)

        Futures.addCallback(searchFuture, object : FutureCallback<SearchResults>{
            override fun onSuccess(result: SearchResults?) {
                iterateResult(result)
            }

            override fun onFailure(t: Throwable) { Toast.makeText(baseContext, t.message, Toast.LENGTH_LONG).show() }
        }, executor)

    }

    private fun iterateResult(results: SearchResults?) {
        val lienable = Futures.transform(results?.nextPage,
            Function<List<SearchResult>, Song> { it ->
                it?.forEach {
                    val genericDocument = it.genericDocument
                    val song = try {
                        if (genericDocument.schemaType == "Song") {
                            genericDocument.toDocumentClass(Song::class.java)
                        } else null
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                    song?.song?.let { it1 -> songNameList.add(it1) }
                    song?.let { it1 -> songList.add(it1) }
                }
                refreshAdapter()
                null
            }, executor)
    }

    private fun refreshAdapter() {
        runOnUiThread {
            adapter.notifyDataSetChanged()
        }
    }

    private fun showToast(msg: String) {
        runOnUiThread {
            songName.text?.clear()
            singer.text?.clear()
            score.progress = 0
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        }
    }
}
