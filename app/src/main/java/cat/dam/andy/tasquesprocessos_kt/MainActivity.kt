package cat.dam.andy.tasquesprocessos_kt

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.FutureTask
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class StatePeriodicTaskContainer {
    var periodicTaskRunning: Boolean = false
}

class MainActivity : AppCompatActivity() {
    private lateinit var btnColors: Button
    private lateinit var tvColors: TextView
    private lateinit var btnFuture: Button
    private lateinit var tvFuture: TextView
    private lateinit var btnProgress: Button
    private lateinit var pbProgressBar: ProgressBar
    private lateinit var tvProgress: TextView
    private lateinit var btnPeriodicTask: Button
    private lateinit var tvPeriodicTask: TextView
    private lateinit var btnDelayedTask: Button
    private lateinit var tvDelayedTask: TextView
    private lateinit var btnMultipleThreads: Button
    private lateinit var tvMultipleThreads: TextView
    private lateinit var tvLoadimage: TextView
    private lateinit var btnLoadImage: Button
    private lateinit var ivAsync: ImageView
    private var colorStop = true
    private val colors = arrayOf(
        R.color.green,
        R.color.maroon,
        R.color.fuchsia,
        R.color.navy
    )
    private var currentColor = 0
    private lateinit var colorChangeExecutor: ScheduledExecutorService
    private val statePeriodicTaskContainer = StatePeriodicTaskContainer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initListeners()
    }

    override fun onDestroy() {
        // Atura la tasca de canvi de color si està en marxa
        stopColorChangeTask()
        // Atura altres executors o tasques aquí...
        super.onDestroy()
    }


    fun initViews() {
        btnColors = findViewById<Button>(R.id.btn_colors)
        tvColors = findViewById<TextView>(R.id.tv_colors)
        btnFuture = findViewById<Button>(R.id.btn_future)
        tvFuture = findViewById<TextView>(R.id.tv_future)
        btnProgress = findViewById<Button>(R.id.btn_progress)
        pbProgressBar = findViewById<ProgressBar>(R.id.pb_progressBar)
        tvProgress = findViewById<TextView>(R.id.tv_progress)
        btnPeriodicTask = findViewById<Button>(R.id.btn_periodic_task)
        tvPeriodicTask = findViewById<TextView>(R.id.tv_periodic_task)
        btnDelayedTask = findViewById<Button>(R.id.btn_delayed_task)
        tvDelayedTask = findViewById<TextView>(R.id.tv_delayed_task)
        btnMultipleThreads = findViewById<Button>(R.id.btn_multiple_threads)
        tvMultipleThreads = findViewById<TextView>(R.id.tv_multiple_threads)
        tvLoadimage = findViewById<TextView>(R.id.tv_loadImage)
        btnLoadImage = findViewById<Button>(R.id.btn_loadImage)
        ivAsync = findViewById<ImageView>(R.id.iv_async)
    }

    @SuppressLint("SetTextI18n")
    @OptIn(DelicateCoroutinesApi::class)
    fun initListeners() {
        btnColors.setOnClickListener { v: View? ->
            colorStop = !colorStop
            if (!colorStop) {
                startColorChangeTask()
            } else {
                stopColorChangeTask()
            }
        }

        btnFuture.setOnClickListener { v: View? ->
            tvFuture.text = getString(R.string.task_in_queue)
            val futureTask = FutureTask(Callable {
                runOnUiThread {
                    tvFuture.text = getString(R.string.task_running)
                }
                doFakeWork()
                getString(R.string.task_completed)
            })

            val executor = Executors.newScheduledThreadPool(1)
            executor.schedule(futureTask, 3, TimeUnit.SECONDS)

            Thread {
                val result = futureTask.get()
                runOnUiThread {
                    try {
                        tvFuture.text = result
                    } catch (e: Exception) {
                        tvFuture.text = "Error: ${e.message}"
                    }
                }
            }.start()
        }

        btnProgress.setOnClickListener { v: View? ->
            val runnable = Runnable {
                for (i in 0..10) {
                    doFakeWork()
                    pbProgressBar.post {
                        tvProgress.text = getString(R.string.updating_progress, i)
                        pbProgressBar.progress = i
                    }
                }
                pbProgressBar.post {
                    tvProgress.text = getString(R.string.task_completed_message)
                    pbProgressBar.progress = 0
                }
            }
            Thread(runnable).start()
        }

        btnPeriodicTask.setOnClickListener { v: View? ->
            tascaPeriodica(statePeriodicTaskContainer, tvPeriodicTask, btnPeriodicTask)
        }

        btnDelayedTask.setOnClickListener { v: View? ->
            tascaAmbRetard(tvDelayedTask)
        }

        btnMultipleThreads.setOnClickListener { v: View? ->
            executorMultiplesFils(tvMultipleThreads)
        }

        btnLoadImage.setOnClickListener { v: View? ->
            downloadImage(tvLoadimage, ivAsync)
        }
    }

    private fun startColorChangeTask() {
        colorChangeExecutor = Executors.newSingleThreadScheduledExecutor()
        colorChangeExecutor.scheduleAtFixedRate(
            {
                runOnUiThread {
                    currentColor = (currentColor + 1) % colors.size
                    tvColors.text = getString(R.string.color_text, currentColor)
                    btnColors.setBackgroundColor(ContextCompat.getColor(this, colors[currentColor]))
                }
            },
            0, 1, TimeUnit.SECONDS
        )
    }

    private fun stopColorChangeTask() {
        colorChangeExecutor.shutdown()
    }

    private fun doFakeWork() {
        SystemClock.sleep(3000)
    }
}


@SuppressLint("SetTextI18n")
@OptIn(DelicateCoroutinesApi::class)
private fun downloadImage(progressTextView: TextView, imageView: ImageView) {
    val startTime = System.currentTimeMillis() // Temps d'inici

    progressTextView.text = "Un moment, si us plau.\nS'està baixant la imatge..."
    imageView.visibility = View.GONE

    GlobalScope.launch(Dispatchers.IO) {
        // Allargar la tasca durant 3 segons (feina intensiva)
        delay(3000)

        // Decodificar la imatge
        val bitmap: Bitmap? =
            loadImageFromUrl("https://agora.xtec.cat/iespladelestany/wp-content/uploads/usu35/2015/11/P1420828.jpg")

        withContext(Dispatchers.Main) {
            if (bitmap != null) {
                // Mostrar la imatge després de la descàrrega (aquí pots carregar la teva imatge real)
                imageView.visibility = View.VISIBLE
                imageView.setImageBitmap(bitmap)
                // Temps final i calcular la diferència
                val endTime = System.currentTimeMillis()
                val elapsedTime = endTime - startTime
                val milliseconds = elapsedTime
                // Actualitzar la interfície d'usuari amb el temps de descàrrega
                progressTextView.text = "Imatge descarregada en $milliseconds milisegons"
            } else {
                // Si la imatge és null, amaga l'ImageView
                imageView.visibility = View.GONE
                progressTextView.text = "No s'ha pogut descarregar la imatge"
            }
        }
    }
}

private fun loadImageFromUrl(url: String): Bitmap? {
    try {
        val connection = URL(url).openConnection()
        connection.connect()
        val inputStream = connection.getInputStream()
        val bitmap = BitmapFactory.decodeStream(inputStream)
        Log.d("ImageLoad", "Imatge descarregada amb èxit")
        return bitmap
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("SetTextI18n")
private fun tascaPeriodica(
    statePeriodicTaskContainer: StatePeriodicTaskContainer,
    tvPeriodicTask: TextView,
    btnPeriodicTask: Button
) {
    if (!statePeriodicTaskContainer.periodicTaskRunning) {
        // Inicia la tasca aquí si cal
        statePeriodicTaskContainer.periodicTaskRunning = true
        var tempsAcumulatSegons = 0L

        //Executa tasca cada 5 segons
        val executor = Executors.newScheduledThreadPool(1)
        val initialDelay = 0L
        val period = 5L

        val updateTask: () -> Unit = {
            tempsAcumulatSegons += 5
            val hores = tempsAcumulatSegons / 3600
            val minuts = tempsAcumulatSegons % 3600 / 60
            val segons = tempsAcumulatSegons % 60

            GlobalScope.launch(Dispatchers.Main) {
                tvPeriodicTask.text = "Executant tasca: $hores h $minuts m $segons s"
            }
        }

        val periodicTask =
            executor.scheduleAtFixedRate(updateTask, initialDelay, period, TimeUnit.SECONDS)

        // Actualitza l'aspecte del botó
        btnPeriodicTask.text = "Atura tasca"

        btnPeriodicTask.setOnClickListener { v: View? ->
            // Atura la tasca aquí si cal
            statePeriodicTaskContainer.periodicTaskRunning = false
            periodicTask.cancel(false)
            btnPeriodicTask.isEnabled = false
            btnPeriodicTask.text = "Tasca aturada"
        }
    } else {
        //reinicia la tasca
        // Ja està en marxa, pots reiniciar-lo aquí si cal
        // ...
    }
}


@OptIn(DelicateCoroutinesApi::class)
@SuppressLint("SetTextI18n")
private fun tascaAmbRetard(tvDelayedTask: TextView) {
    val scheduledExecutorService = Executors.newScheduledThreadPool(1)
    val task = Runnable {
        GlobalScope.launch(Dispatchers.Main) {
            tvDelayedTask.text = "Executant tasca a: " + System.nanoTime()
        }
    }
    GlobalScope.launch(Dispatchers.Main) {
        tvDelayedTask.text =
            "Enviant tasca a: " + System.nanoTime() + " per ser executada després de 5s."
    }
    scheduledExecutorService.schedule(task, 5, TimeUnit.SECONDS)
    scheduledExecutorService.shutdown()
}

@SuppressLint("SetTextI18n")
@OptIn(DelicateCoroutinesApi::class)
private fun executorMultiplesFils(tvMultipleThreads: TextView) {
    GlobalScope.launch(Dispatchers.Main) {
        tvMultipleThreads.text = "Dins : " + Thread.currentThread().name
        tvMultipleThreads.append("\nCreant ExecutorService amb un pool de 2 fils")
    }
    val executorService = Executors.newFixedThreadPool(2)
    val task1 = Runnable {
        GlobalScope.launch(Dispatchers.Main) {
            tvMultipleThreads.append("\nExecutant tasca 1 dins: " + Thread.currentThread().name)
        }
        try {
            TimeUnit.SECONDS.sleep(2)
        } catch (ex: InterruptedException) {
            throw IllegalStateException(ex)
        }
    }
    val task2 = Runnable {
        GlobalScope.launch(Dispatchers.Main) {
            tvMultipleThreads.append("\nExecutant tasca 2 dins: " + Thread.currentThread().name)
        }
        try {
            TimeUnit.SECONDS.sleep(4)
        } catch (ex: InterruptedException) {
            throw IllegalStateException(ex)
        }
    }
    val task3 = Runnable {
        GlobalScope.launch(Dispatchers.Main) {
            tvMultipleThreads.append("\nExecutant tasca 3 dins: " + Thread.currentThread().name)
        }
        try {
            TimeUnit.SECONDS.sleep(3)
        } catch (ex: InterruptedException) {
            throw IllegalStateException(ex)
        }
    }
    GlobalScope.launch(Dispatchers.Main) {
        tvMultipleThreads.append("\nEnviant tasques a executar...")
    }
    executorService.submit(task1)
    executorService.submit(task2)
    executorService.submit(task3)
    executorService.shutdown()
}


