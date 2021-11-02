package ru.startandroid.develop.p1261mediaplayer

import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnCompletionListener
import android.media.MediaPlayer.OnPreparedListener
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.provider.MediaStore.Audio.Media.*
import android.util.Log
import android.widget.CheckBox
import java.io.IOException

class MainActivity : AppCompatActivity(), OnPreparedListener, OnCompletionListener {
    private lateinit var mediaPlayer: MediaPlayer
    lateinit var am: AudioManager
    lateinit var chbLoop: CheckBox

    /*
        В onCreate получаем AudioManager, находим на экране чекбокс и настраиваем так, чтобы он
            включал/выключал режим повтора для плеера.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        am = getSystemService(AUDIO_SERVICE) as AudioManager
        chbLoop = findViewById(R.id.chbLoop)
        chbLoop.setOnCheckedChangeListener { _, isChecked ->
            if (::mediaPlayer.isInitialized) mediaPlayer.isLooping = isChecked
        }
    }

    /*
        onClickStart – метод для обработки нажатий на кнопки верхнего ряда. Сначала мы освобождаем
            ресурсы текущего проигрывателя. Затем в зависимости от нажатой кнопки стартуем
            проигрывание.
     */
    fun onClickStart(view: View) {
        releaseMP()
        try {
            when(view.id) {
                R.id.btnStartHttp -> {
                    Log.d(LOG_TAG, "start HTTP")
                    mediaPlayer = MediaPlayer()
                    mediaPlayer.apply {
                        //setDataSource – задает источник данных для проигрывания
                        setDataSource(DATA_HTTP)
                        Log.d(LOG_TAG, "prepareAsync")
                        setOnPreparedListener(this@MainActivity)
                        /*
                            Далее используется метод prepare или prepareAsync (в паре с
                                OnPreparedListener). Эти методы подготавливают плеер к
                                проигрыванию. И, как понятно из названия, prepareAsync делает это
                                асинхронно, и, когда все сделает, сообщит об этом слушателю из
                                метода setOnPreparedListener. А метод prepare работает синхронно.
                                Соотвественно, если хотим прослушать файл из инета, то используем
                                prepareAsync, иначе наше приложение повесится, т.к. заблокируется
                                основной поток, который обслуживает UI.
                         */
                        prepareAsync()
                    }
                }

                R.id.btnStartStream -> {
                    Log.d(LOG_TAG, "start Stream")
                    mediaPlayer = MediaPlayer()
                    mediaPlayer.apply {
                        setDataSource(DATA_STREAM)
                        Log.d(LOG_TAG, "prepareAsync")
                        setOnPreparedListener(this@MainActivity)
                        prepareAsync()
                    }
                }

                R.id.btnStartRaw -> {
                    Log.d(LOG_TAG, "start Raw")
                    /*
                        В случае с raw-файлом мы используем метод create. В нем уже будет выполнен
                            метод prepare и нам остается только выполнить start.
                     */
                    mediaPlayer = MediaPlayer.create(this, R.raw.music)
                    mediaPlayer.start()
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }

        if (!::mediaPlayer.isInitialized) return

        /*
            Далее мы для плеера включаем/выключаем повтор (setLooping) в зависимости от текущего
                значения чекбокса. И вешаем слушателя (setOnCompletionListener), который получит
                уведомление, когда проигрывание закончится.
         */
        mediaPlayer.apply {
            isLooping = chbLoop.isChecked
            setOnCompletionListener(this@MainActivity)
        }
    }

    //В методе onClick мы обрабатываем нажатия на кнопки управления проигрывателем.
    fun onClick(view: View) {
        if (!::mediaPlayer.isInitialized) return
        mediaPlayer.apply {
            when(view.id) {
                //pause – приостанавливает проигрывание
                R.id.btnPause -> if (isPlaying) pause()
                //start – возобновляет проигрывание
                R.id.btnResume -> if (!isPlaying) start()
                //stop – останавливает проигрывание
                R.id.btnStop -> {
                    stop()
                    prepare()
                    seekTo(0)
                }
                //seekTo – переход к определенной позиции трека (в милисекундах)
                //getCurrentPosition – получить текущую позицию (в милисекундах)
                R.id.btnBackward -> seekTo(currentPosition - 3000)
                R.id.btnForward -> seekTo(currentPosition + 3000)
                R.id.btnInfo -> {
                    Log.d(LOG_TAG, "Playing $isPlaying")
                    //getDuration – общая продолжительность трека
                    Log.d(LOG_TAG, "Time $currentPosition/$duration")
                    //isLooping – включен ли режим повтора
                    Log.d(LOG_TAG, "Looping $isLooping")
                    //getStreamVolume – получить уровень громкости указанного потока
                    Log.d(LOG_TAG, "Volume ${am.getStreamVolume(AudioManager.STREAM_MUSIC)}")
                }
            }
        }
    }

    /*
        В методе releaseMP мы выполняем метод release. Он освобождает используемые проигрывателем
            ресурсы, его рекомендуется вызывать когда вы закончили работу с плеером. Более того,
            хелп рекомендует вызывать этот метод и при onPause/onStop, если нет острой
            необходимости держать объект.
     */
    private fun releaseMP() {
        if (::mediaPlayer.isInitialized) {
            try {
                mediaPlayer.release()
                //mediaPlayer = null
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    /*
        Сначала создаем константы-пути, которые будет использовать проигрыватель. Это файл в инете
            (DATA_HTTP), поток в инете (DATA_STREAM), файл на флэшке (DATA_SD) и Uri на мелодию из
            системы (DATA_URI).
     */
    companion object {
        const val LOG_TAG = "myLogs"
        const val DATA_HTTP = "http://dl.dropboxusercontent.com/u/6197740/explosion.mp3"
        const val DATA_STREAM = "http://online.radiorecord.ru:8101/rr_128"
    }

    //onPrepared – метод слушателя OnPreparedListener. Вызывается, когда плеер готов к проигрыванию
    override fun onPrepared(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "onPrepared")
        mp?.start()
    }

    //onCompletion – вызывается, когда достигнут конец проигрываемого содержимого.
    override fun onCompletion(mp: MediaPlayer?) {
        Log.d(LOG_TAG, "onCompletion")
    }

    //В методе onDestroy обязательно освобождаем ресурсы проигрывателя.
    override fun onDestroy() {
        super.onDestroy()
        releaseMP()
    }
}