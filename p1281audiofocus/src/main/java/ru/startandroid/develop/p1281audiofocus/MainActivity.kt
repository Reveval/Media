package ru.startandroid.develop.p1281audiofocus

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager.*
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi

const val LOG_TAG = "myLogs"

//Реализуем смену аудио-фокуса при проигрывании некоторых звуков
class MainActivity : AppCompatActivity(), MediaPlayer.OnCompletionListener {
    lateinit var audioManager: AudioManager
    lateinit var afListenerMusic: AFListener
    lateinit var afListenerSound: AFListener

    lateinit var audioFocusRequestMusic: AudioFocusRequest
    lateinit var audioFocusRequestSound: AudioFocusRequest

    lateinit var mpMusic: MediaPlayer
    lateinit var mpSound: MediaPlayer

    //В onCreate мы просто получаем AudioManager. Именно через него мы будем запрашивать фокус.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /*
        onClickMusic срабатывает при нажатии кнопки Music. Здесь мы создаем MediaPlayer и даем ему
            путь к файлу с музыкой. Методом setOnCompletionListener устанавливаем Activity, как
            получателя уведомления о окончании воспроизведения. Далее идет работа с фокусом.
            afListenerMusic – это слушатель (реализующий интерфейс OnAudioFocusChangeListener),
            который будет получать сообщения о потере/восстановлении фокуса. Он является
            экземпляром класса AFListener
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun onClickMusic(view: View) {
        mpMusic = MediaPlayer.create(this, R.raw.music)
        mpMusic.setOnCompletionListener(this)

        afListenerMusic = AFListener(mpMusic, "Music")
        //создаем объект типа AudioFocusRequest, который будет содержать в себе всю конфигурацию
        audioFocusRequestMusic = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN).apply {
            //устанавливаем слушателя, который будет получать сообщения о фокусе
            setOnAudioFocusChangeListener(afListenerMusic)
            //устанавливаем аудио - аттрибуты
            setAudioAttributes(
                AudioAttributes.Builder()
                    //ставим тип потока
                    .setLegacyStreamType(STREAM_MUSIC)
                    .build()
            )
        }.build()

        //Фокус запрашивается с помощью метода requestAudioFocus.
        val requestResult = audioManager.requestAudioFocus(audioFocusRequestMusic)
        Log.d(LOG_TAG, "Music request focus, result: $requestResult")

        mpMusic.start()
    }

    /*
        Метод onClickSound срабатывает при нажатии на любую из трех кнопок Sound. Здесь мы
            определяем, какая из трех кнопок была нажата. Тем самым мы в переменную durationHint
            пишем тип аудио-фокуса, который будем запрашивать. Далее создаем MediaPlayer, который
            будет воспроизводить наш звук взрыва из папки raw. Присваиваем ему слушателя окончания
            воспроизведения. Запрашиваем фокус с типом, который определили выше. Стартуем
            воспроизведение.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun onClickSound(view: View) {
        val durationHint = when(view.id) {
            R.id.btnPlaySoundG -> AUDIOFOCUS_GAIN
            R.id.btnPlaySoundGT -> AUDIOFOCUS_GAIN_TRANSIENT
            R.id.btnPlaySoundGTD -> AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            else -> AUDIOFOCUS_GAIN
        }

        mpSound = MediaPlayer.create(this, R.raw.explosion)
        mpSound.setOnCompletionListener(this)
        afListenerSound = AFListener(mpSound, "Sound")

        audioFocusRequestSound = AudioFocusRequest.Builder(durationHint).apply {
            setOnAudioFocusChangeListener(afListenerSound)
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setLegacyStreamType(STREAM_MUSIC)
                    .build()
            )
        }.build()

        val requestResult = audioManager.requestAudioFocus(audioFocusRequestSound)
        Log.d(LOG_TAG, "Sound request focus, result: $requestResult")
        mpSound.start()
    }

    /*
        Метод onCompletion, срабатывает по окончании воспроизведения. Мы определяем, какой именно
            MediaPlayer закончил играть и методом abandonAudioFocus сообщаем системе, что больше не
            претендуем на аудио-фокус. На вход методу передаем того же слушателя, который давали
            при запросе фокуса.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCompletion(mp: MediaPlayer?) {
        audioManager.run {
            if (mp == mpMusic) {
                Log.d(LOG_TAG, "Music: abandon focus")
                abandonAudioFocusRequest(audioFocusRequestMusic)
            } else {
                Log.d(LOG_TAG, "Sound: abandon focus")
                abandonAudioFocusRequest(audioFocusRequestSound)
            }
        }
    }

    //В onDestroy освобождаем ресурсы и отпускаем фокус.
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onDestroy() {
        super.onDestroy()
        if (::mpMusic.isInitialized) mpMusic.release()
        if (::mpSound.isInitialized) mpSound.release()
        if (::afListenerMusic.isInitialized) audioManager
            .abandonAudioFocusRequest(audioFocusRequestMusic)
        if (::afListenerSound.isInitialized) audioManager
            .abandonAudioFocusRequest(audioFocusRequestSound)
    }

    class AFListener(var mp: MediaPlayer, var label: String) : OnAudioFocusChangeListener {
        //Метод onAudioFocusChange получает на вход статус фокуса этого приложения.
        override fun onAudioFocusChange(focusChange: Int) {
            val event = when(focusChange) {
                /*
                    AUDIOFOCUS_LOSS – фокус потерян в результате того, что другое приложение
                        запросило фокус AUDIOFOCUS_GAIN. Т.е. нам дают понять, что другое
                        приложение собирается воспроизводить что-то долгое и просит нас пока
                        приостановить наше воспроизведение.
                 */
                AUDIOFOCUS_LOSS -> {
                    mp.pause()
                    "AUDIOFOCUS_LOSS"
                }
                /*
                    AUDIOFOCUS_LOSS_TRANSIENT - фокус потерян в результате того, что другое
                        приложение запросило фокус AUDIOFOCUS_GAIN_TRANSIENT. Нам дают понять, что
                        другое приложение собирается воспроизводить что-то небольшое и просит нас
                        пока приостановить наше воспроизведение
                 */
                AUDIOFOCUS_LOSS_TRANSIENT -> {
                    mp.pause()
                    "AUDIOFOCUS_LOSS_TRANSIENT"
                }
                /*
                    AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK - фокус потерян в результате того, что
                    другое приложение запросило фокус AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK. Нам дают
                    понять, что другое приложение собирается воспроизводить что-то небольшое, и мы
                    можем просто убавить звук, не приостанавливая воспроизведение
                 */
                AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                    mp.setVolume(0.5F, 0.5F)
                    "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK"
                }
                //AUDIOFOCUS_GAIN – другое приложение закончило воспроизведение, звук снова наш
                AUDIOFOCUS_GAIN -> {
                    if (!mp.isPlaying) {
                        mp.start()
                        mp.setVolume(1.0F, 1.0F)
                    }
                    "AUDIOFOCUS_GAIN"
                }
                else -> ""
            }
            Log.d(LOG_TAG, "$label on AudioFocusChange: $event")
        }
    }
}