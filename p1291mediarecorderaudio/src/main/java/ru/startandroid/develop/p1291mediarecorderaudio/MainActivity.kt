package ru.startandroid.develop.p1291mediarecorderaudio

import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.annotation.RequiresApi
import java.io.File

const val FILE_NAME = "record.3gpp"

//Напишем приложение для записи звука и его воспроизведения
class MainActivity : AppCompatActivity() {
    lateinit var mediaRecorder: MediaRecorder
    lateinit var mediaPlayer: MediaPlayer
    lateinit var file: File

    //создаем объект типа File и помещаем туда имя файла
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        file = File(getExternalFilesDir(null), FILE_NAME)
    }

    /*
        В recordStart мы избавляемся от старого рекордера. Затем удаляем файл для записи, если он
            уже существует. Далее создаем и настраиваем рекордер используя ряд методов.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun recordStart(view: View) {
        try {
            releaseRecorder()

            if (file.exists()) {
                file.delete()
            } else {
                file = File(getExternalFilesDir(null), FILE_NAME)
            }

            mediaRecorder = MediaRecorder()
            mediaRecorder.apply {
                //Указываем источник звука – микрофон (MIC).
                setAudioSource(MediaRecorder.AudioSource.MIC)
                //setOutputFormat. Указываем формат – 3GPP (THREE_GPP).
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                //Указываем кодек для сжатия аудио - AMR_NB
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                //указываем файл, в который будет записано аудио
                setOutputFile(file)
                /*
                    После всех настроек вызываем метод prepare, который подготовит рекордер к записи и
                        стартуем запись методом start.
                 */
                prepare()
                start()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    /*
        В recordStop останавливаем запись методом stop. После этого метода необходимо заново
            настроить рекордер, если вы снова хотите его использовать. Просто снова вызвать start
            не получится. На схеме это показано. Кстати, метод reset также сбрасывает все настройки
            рекордера и после него необходимо заново указывать источник. формат, кодек, файл. Но
            объект новый создавать необязательно.
     */
    fun recordStop(view: View) {
        if (::mediaRecorder.isInitialized ) mediaRecorder.stop()
    }

    //В playStart и playStop стартуем и останавливаем воспроизведение записанного файла.
    fun playStart(view: View) {
        try {
            releasePlayer()
            mediaPlayer = MediaPlayer()
            mediaPlayer.apply {
                mediaPlayer.
                setDataSource(file.path)
                prepare()
                start()
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    fun playStop(view: View) {
        if (::mediaPlayer.isInitialized) mediaPlayer.stop()
    }

    /*
        В методе releaseRecorder мы освобождаем все ресурсы рекордера методом release. После этого
            объект уже нельзя использовать и необходимо создавать и настраивать новый.
     */
    private fun releaseRecorder() {
        if (::mediaRecorder.isInitialized) mediaRecorder.release()
    }

    private fun releasePlayer() {
        if (::mediaPlayer.isInitialized) mediaPlayer.release()
    }

    override fun onDestroy() {
        super.onDestroy()
        releasePlayer()
        releaseRecorder()
    }
}