package com.example.RSoDE

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.CacheResponse
import kotlin.random.Random

//TODO: design idle screen
//TODO: Lay out code for card system
var startedProperly: Boolean = false
var ghosts = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
var ghostJSON:JSONObject = JSONObject()
class NFCErrorDialog: DialogFragment(){
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage("That ain't no valid token or board space!")
                .setTitle("NFC Error")
                .setPositiveButton("Yee Haw!") { dialog, id -> }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class infoBox(description: String) : DialogFragment(){
    val desc = description
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            builder.setMessage(desc)
                .setTitle("Information")
                .setPositiveButton("ok") { dialog, id -> }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class winLossMessage(success: Boolean, response: String) : DialogFragment(){
    val wl = success
    val resp = response
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            // Use the Builder class for convenient dialog construction
            val builder = AlertDialog.Builder(it)
            if (wl) {
                builder.setMessage("$resp \nSuccess, gain 1 point")
                    .setTitle("Win message")
            }else{
                builder.setMessage("$resp \nFailure, lose 1 point")
                    .setTitle("Loss message")
            }
                .setPositiveButton("ok") { dialog, id -> }
            // Create the AlertDialog object and return it
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}

class MainActivity : AppCompatActivity() {
    private val mHideHandler = Handler()
    private val mHidePart2Runnable = Runnable {
        // Delayed removal of status and navigation bar

        // Note that some of these constants are new as of API 16 (Jelly Bean)
        // and API 19 (KitKat). It is safe to use them, as they are inlined
        // at compile-time and do nothing on earlier devices.
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LOW_PROFILE or
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }
    private val mShowPart2Runnable = Runnable {
        // Delayed display of UI elements
        supportActionBar?.show()
        dialogueOpts.visibility = View.VISIBLE
    }
    private var mVisible: Boolean = false
    private val mHideRunnable = Runnable { hide() }
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        mVisible = true
        if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED) {
            if (!startedProperly){
                init()
                startedProperly = true
            }
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)?.also { rawMessages ->
                val messages: List<NdefMessage> = rawMessages.map { it as NdefMessage }
                for (message: NdefMessage in messages) {
                    val record: List<NdefRecord> = message.records.asList()
                    val payload: ByteArray = record[0].payload
                    val payloadString = String(payload, charset("US-ASCII"))
                    if (payloadString.substring(1..12) == "enGhostToken") {
                        //Extract index number from tag data
                        val tagIndexNum = payloadString.substring(13 until payloadString.length).toInt()
                        //Get ghost assigned to tag index
                        val ghostIndexNum = ghosts[tagIndexNum]
                        //Load ghost from JSON data
                        val ghost: JSONObject = ghostJSON.getJSONObject(ghostIndexNum.toString())
                        println(ghost)
                        //Plug JSON into dialogue UI
                        nameBox.text = ghost["name"].toString()
                        val dialogueOpts = ghost.getJSONObject("dialogOpts")
                        val charm = dialogueOpts.getJSONArray("charm").getJSONObject(Random.nextInt(0, dialogueOpts.getJSONArray("charm").length()))
                        val stern = dialogueOpts.getJSONArray("stern").getJSONObject(Random.nextInt(0, dialogueOpts.getJSONArray("stern").length()))
                        val respect = dialogueOpts.getJSONArray("rspct").getJSONObject(Random.nextInt(0, dialogueOpts.getJSONArray("rspct").length()))
                        dialogue1.text = charm.getString("opt")
                        dialogue1.setOnTouchListener {v, event ->
                            val fragment = winLossMessage(ghost.getString("prefRespType") == "charm", charm.getString("resp"))
                            fragment.show(supportFragmentManager, "winlossmessage")
                            dialogueMainBox.visibility = View.INVISIBLE
                            true
                        }
                        dialogue2.text = stern.getString("opt")
                        dialogue2.setOnTouchListener {v, event ->
                            val fragment = winLossMessage(ghost.getString("prefRespType") == "stern", stern.getString("resp"))
                            fragment.show(supportFragmentManager, "winlossmessage")
                            dialogueMainBox.visibility = View.INVISIBLE
                            true
                        }
                        dialogue3.text = respect.getString("opt")
                        dialogue3.setOnTouchListener {v, event ->
                            val fragment = winLossMessage(ghost.getString("prefRespType") == "rspct", respect.getString("resp"))
                            fragment.show(supportFragmentManager, "winlossmessage")
                            dialogueMainBox.visibility = View.INVISIBLE
                            true
                        }

                        dialogueMainBox.visibility = View.VISIBLE
                        topBar.info.setOnTouchListener { v, event ->
                            val fragment = infoBox(ghost.getString("desc"))
                            fragment.show(supportFragmentManager, "description")
                            true
                        }

                    } else {
                        if (payloadString.substring(1..12) == "enBoardSpace") {
                            //Handle cards
                            val card = Random.nextInt(0, 100)
                            fullscreen_content.text = "Card #$card"
                            fullscreen_content.visibility = View.VISIBLE
                        }else{
                            //If NFC tag isn't part of the game, complain to user about it.
                            val fragment = NFCErrorDialog()
                            fragment.show(supportFragmentManager, "nfcError")
                        }
                    }
                }
            }
        }else{
            startedProperly = true
            init()
        }
        fullscreen_content.text = "Dialogue sequence here"
        // Set up the user interaction to manually show or hide the system UI.
        fullscreen_content.setOnClickListener { toggle() }
        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
    }

    private fun init(){
        //Get JSON for ghosts
        fun getAssetJsonData(context: Context): JSONObject {
            var json: String? = null
            json = try {
                val `is`: InputStream = context.assets.open("ghosts.json")
                val size: Int = `is`.available()
                val buffer = ByteArray(size)
                `is`.read(buffer)
                `is`.close()
                String(buffer, charset("UTF-8"))
            } catch (ex: IOException) {
                ex.printStackTrace()
                return JSONObject()
            }
            Log.e("data", json)
            return JSONObject(json)
        }
        ghostJSON = getAssetJsonData(applicationContext)
        //randomize ghost associated with each token
        ghosts = List(12) { Random.nextInt(0, ghostJSON.length())}
        println(ghosts)
        println(ghostJSON)
    }
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100)
    }

    private fun toggle() {
        if (mVisible) {
            hide()
        } else {
            show()
        }
    }

    private fun hide() {
        // Hide UI first
        supportActionBar?.hide()
        fullscreen_content.visibility = View.GONE
        mVisible = false

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable)
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    private fun show() {
        // Show the system bar
        fullscreen_content.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        mVisible = true

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable)
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY.toLong())
    }

    /**
     * Schedules a call to hide() in [delayMillis], canceling any
     * previously scheduled calls.
     */
    private fun delayedHide(delayMillis: Int) {
        mHideHandler.removeCallbacks(mHideRunnable)
        mHideHandler.postDelayed(mHideRunnable, delayMillis.toLong())
    }

    companion object {
        /**
         * Whether or not the system UI should be auto-hidden after
         * [AUTO_HIDE_DELAY_MILLIS] milliseconds.
         */
        private val AUTO_HIDE = true

        /**
         * If [AUTO_HIDE] is set, the number of milliseconds to wait after
         * user interaction before hiding the system UI.
         */
        private val AUTO_HIDE_DELAY_MILLIS = 3000

        /**
         * Some older devices needs a small delay between UI widget updates
         * and a change of the status and navigation bar.
         */
        private val UI_ANIMATION_DELAY = 300
    }
}
