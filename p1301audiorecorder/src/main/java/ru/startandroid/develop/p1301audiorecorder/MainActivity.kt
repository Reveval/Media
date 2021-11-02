package ru.startandroid.develop.p1301audiorecorder

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View

const val LOG_TAG = "myLogs"
const val MY_BUFFER_SIZE = 8192

//Напишем приложение, которое во время работы будет записывать аудио

class MainActivity : AppCompatActivity() {
    lateinit var audioRecord: AudioRecord
    var isReading = false

    /*
        В onCreate мы вызываем свой метод создания AudioRecorder и выводим в лог состояние
            созданного объекта. Состояние можно получить методом getState. Может быть всего два
            состояния: STATE_INITIALIZED и STATE_UNINITIALIZED. Означают они соответственно то,
            что AudioRecorder к работе готов или не готов.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createAudioRecorder()

        Log.d(LOG_TAG, "init state = ${audioRecord.state}")
    }

    /*
        В createAudioRecorder создаем AudioRecorder. Для этого нам понадобится несколько входных
            параметров:
     */
    private fun createAudioRecorder() {
        //сэмплрейт
        val sampleRate = 8000
        //режим каналов моно/стерео
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        //формат аудио
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        /*
            Чтобы узнать размер буфера, есть метод getMinBufferSize. Он, исходя из переданных ему
                на вход данных о формате аудио, возвращает минимально-возможный размер буфера, с
                которым сможет работать AudioRecorder
         */
        val minInternalBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig,
            audioFormat)

        /*
            Мы получаем минимальный размер и в переменную internalBufferSize помещаем этот размер,
                умноженный на 4.
         */
        val internalBufferSize = minInternalBufferSize * 4
        Log.d(LOG_TAG, "minInternalBufferSize = $minInternalBufferSize" +
                ", internalBufferSize = $internalBufferSize" +
                ", myBufferSize = $MY_BUFFER_SIZE")
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channelConfig,
            audioFormat, internalBufferSize)
    }

    /*
        В методе recordStart стартуем запись методом startRecording. С помощью метода
            getRecordingState получаем статус - идет запись или нет. Вариантов тут два:
            RECORDSTATE_RECORDING (запись идет) и RECORDSTATE_STOPPED (запись остановлена).
     */
    fun recordStart(view: View) {
        Log.d(LOG_TAG, "record start")
        audioRecord.startRecording()
        val recordingState = audioRecord.recordingState
        Log.d(LOG_TAG, "recordingState = $recordingState")
    }

    //В recordStop останавливаем запись методом stop.
    fun recordStop(view: View) {
        Log.d(LOG_TAG, "record stop")
        audioRecord.stop()
    }

    fun readStart(view: View) {
        Log.d(LOG_TAG, "read start")
        /*
            Ставим метку isReading в true. Она будет означать, что мы сейчас находимся в режиме
                чтения данных из AudioRecorder.
         */
        isReading = true
        //Далее создаем новый поток и чтение выполняем в нем, чтобы не занимать основной поток.
        Thread(Runnable {
            if (!::audioRecord.isInitialized) return@Runnable

            val myBuffer = ByteArray(MY_BUFFER_SIZE)
            var readCount: Int
            var totalCount = 0
            /*
                Мы создаем свой буфер размером myBufferSize и читаем в него данные методом read.
                    Это происходит в цикле, который проверяет, что мы в режиме чтения.
             */
            while (isReading) {
                /*
                    Метод read на вход принимает массив (в который будут помещены данные), отступ
                        (если вам надо прочесть данные не сначала, а с какой-то позиции), и размер
                        порции получаемых данных. В readCount метод read возвращает число байт,
                        которые он нам отдал. В totalCount мы суммируем общее количество полученных
                        байтов.
                 */
                readCount = audioRecord.read(myBuffer, 0, MY_BUFFER_SIZE)
                //В totalCount мы суммируем общее количество полученных байтов.
                totalCount += readCount
                Log.d(LOG_TAG, "readCount = $readCount, totalCount = $totalCount")
            }
        }).start()
    }

    /*
        В методе readStop мы выключаем режим чтения, присваивая переменной isReading значение
            false. Поток из readStart прочтет это значение, выйдет из цикла и завершит свою работу.
     */
    fun readStop(view: View) {
        Log.d(LOG_TAG, "read stop")
        isReading = false
    }

    //В onDestroy выключаем режим чтения и методом release освобождаем ресурсы, занятые AudioRecord.
    override fun onDestroy() {
        super.onDestroy()
        isReading = false
        if (::audioRecord.isInitialized) audioRecord.release()
    }
}