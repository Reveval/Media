package ru.startandroid.develop.p1271soundpool

import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi

const val LOG_TAG = "myLogs"
const val MAX_STREAMS = 5

class MainActivity : AppCompatActivity(), SoundPool.OnLoadCompleteListener {
    lateinit var sp: SoundPool
    var soundIdShot = 0
    var soundIdExplosion = 0

    var streamIdShot = 0
    var streamIdExplosion = 0

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //создаем SoundPool
        sp = SoundPool.Builder().apply {
            //передаем максимальное количество одновременно воспроизводимых файлов
            setMaxStreams(MAX_STREAMS)
            //создаем объект с аттрибутами для SoundPool
            val attr = AudioAttributes.Builder().apply {
                //указываем аудиопоток, который будет использоваться
                setLegacyStreamType(AudioManager.STREAM_MUSIC)
            }.build()
            //сетаем аттрибуты к SoundPool
            setAudioAttributes(attr)
        }.build()

        /*
            Методом setOnLoadCompleteListener мы устанавливаем слушателя загрузки. Загрузка
                аудио-файлов происходит асинхронно, и по ее окончании срабатывает метод
                onLoadComplete этого слушателя.
         */
        sp.setOnLoadCompleteListener(this)

        /*
            Чтобы загрузить файл из raw, необходимо указать Context, ID raw-файла и приоритет.
                Приоритет пока что также игнорируется системой, рекомендуется передавать туда 1.
                Метод load возвращает ID загруженного файла.
         */
        soundIdShot = sp.load(this, R.raw.shot, 1)
        Log.d(LOG_TAG, "soundIdShot = $soundIdShot")

        try {
            /*
                Чтобы загрузить файл из assets используем другую реализацию метода load, которая на
                    вход требует AssetFileDescriptor и приоритет. AssetFileDescriptor можно
                    получить, используя метод openFd класса AssetManager, указав имя файла.
                    Приоритет снова никакой роли не играет, передаем 1.
             */
            soundIdExplosion = sp.load(assets.openFd("explosion.ogg"), 1)
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        Log.d(LOG_TAG, "soundIdExplosion = $soundIdExplosion")
    }

    /*
        В методе onClick запускаем воспроизведение файлов. Для этого используется метод play. На
            вход он требует ряд параметров:
                - ID файла. Тот самый, который мы получили от метода load.
                - громкость левого канала (от 0.0 до 1.0)
                - громкость правого канала (от 0.0 до 1.0)
                - приоритет. Этот приоритет уже не декоративный, а вполне себе используемый. Далее
                    увидим, где он нужен.
                - количество повторов. Т.е. файл будет воспроизведен один раз точно + то количество
                    раз, которое вы здесь укажете
                - скорость воспроизведения. Можно задать от половины нормальной скорости до
                    двойной (0.5 - 2).
            Метод play возвращает ID потока, используемого для проигрывания файла. Этот ID можно
            использовать для дальнейшего изменения настроек в процессе проигрывания файла, а также
            для паузы.
     */
    fun onClick(view: View) {
        sp.run {
            play(soundIdShot, 0.1F, 1F, 0,0, 1F)
            play(soundIdExplosion, 1F, 1F, 0, 0, 1F)
        }
    }

    /*
        Метод onLoadComplete слушателя OnLoadCompleteListener выполняется, когда SoundPool
            загружает файл. На вход вы получаете сам SoundPool, ID файла (тот же, что и load
            возвращал) и статус (0, если успешно)
     */
    override fun onLoadComplete(soundPool: SoundPool?, sampleId: Int, status: Int) {
        Log.d(LOG_TAG, "onLoadComplete, sampleId = $sampleId, status = $status")
    }
}