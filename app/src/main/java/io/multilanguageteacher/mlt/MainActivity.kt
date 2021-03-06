package io.multilanguageteacher.mlt

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.room.Room
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import java.util.*
import com.google.gson.Gson



class GetWordResponse {
    val word: String = ""
}
class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private val CHANNEL_ID = "mlt"
    private val EXTRA_NOTIFICATION_ID = "mlt-snooze"
    var notificationId = 1
    lateinit var tts: TextToSpeech
    lateinit var db: AppDatabase
    private lateinit var functions: FirebaseFunctions

    override fun onInit(status: Int) {

        if (status == TextToSpeech.SUCCESS) {
            Log.i("TTS", "Initialisation ok")

            val result = tts.setLanguage(Locale.US)

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported")
            } else {
                val talkButton: Button = findViewById( R.id.button_talk)
                talkButton.setEnabled(true)
            }

        } else {
            Log.e("TTS", "Initilization Failed!")
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this, this)


        setContentView(R.layout.activity_main)

        // https://developer.android.com/guide/topics/ui/notifiers/notifications
        // https://developer.android.com/training/notify-user/build-notification
        // https://openclassrooms.com/fr/courses/4872916-creez-un-backend-scalable-et-performant-sur-firebase/4982776-diffusez-une-notification-via-l-interface-firebase
        createNotificationChannel()

        // https://developer.android.com/training/data-storage/room
        // https://developer.android.com/jetpack/androidx/releases/room
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "mlt"
        )
            .fallbackToDestructiveMigration() // this is only for testing
            .allowMainThreadQueries() // this is only for testing
            .build()

        // https://stackoverflow.com/questions/44429372/view-contents-of-database-created-with-room-persistence-library
        // todo https://android.jlelse.eu/5-steps-to-implement-room-persistence-library-in-android-47b10cd47b24
        // todo https://vogella.developpez.com/tutoriels/android/utilisation-base-donnees-sqlite/#LI-A
        // todo http://blog.harrix.org/article/6784
//        db.clearAllTables()
        db.wordDao().deleteAll()
        db.wordDao().insertAll(Word(1, "dress", "vestido", "robe"), Word(2, "skirt", "falda", "jupe"))


    }

    public override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }

    fun newWord(v: View?) {
        functions = FirebaseFunctions.getInstance("europe-west1")
        functions
            .getHttpsCallable("getWord")
            .call()
            .continueWith { task ->
                val response = Gson().fromJson(task.result?.data.toString(), GetWordResponse::class.java)
                val mEdit:EditText  = findViewById( R.id.editText_talk)
                mEdit.setText(response.word)

            }

    }
    fun talk(v: View?) {
        val mEdit:EditText  = findViewById( R.id.editText_talk)
        Log.d("MLT_DEBUG", "talk button pressed")
        Log.d("MLT_DEBUG", mEdit.text.toString())
        tts.speak(mEdit.text.toString(), TextToSpeech.QUEUE_FLUSH, null,"")
    }

    fun pressButton(v: View?) {
        val textTitle = "notif title"


        val textContent = "notif content"

        Log.d("MLT_DEBUG", "create button intent")
        val englishWord = "dress"
        val spanishWord = db.wordDao().findByEnglish(englishWord).spanish
        Log.d("MLT_DEBUG", "spanish of $englishWord is $spanishWord")

        // Create an explicit intent for an Activity in your app

        val AlertDetails = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.d("MLT_DEBUG", "content intent")
            }
        }
        val intent = Intent(this, AlertDetails::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val ACTION_SNOOZE = "io.multilanguageteacher.mlt.snooze"

        val MyBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {

                Log.d("MLT_DEBUG", "snooze intent")
            }
        }

        val snoozeIntent = Intent(this, MyBroadcastReceiver::class.java).apply {
            action = ACTION_SNOOZE
            putExtra(EXTRA_NOTIFICATION_ID, 0)
        }
        val snoozePendingIntent: PendingIntent =
            PendingIntent.getBroadcast(this, 0, snoozeIntent, 0)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            // https://romannurik.github.io/AndroidAssetStudio/icons-notification.html
            .setSmallIcon(R.drawable.ic_stat_golf_course)
            .setContentTitle(textTitle)
            .setContentText(textContent)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Much longer text that cannot fit one line...")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .addAction(
                R.drawable.ic_stat_golf_course, getString(R.string.snooze),
                snoozePendingIntent
            )


        notificationId += 1
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
