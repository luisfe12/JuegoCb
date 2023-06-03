package com.example.juegocaballos

import android.app.Application
import android.content.ContentValues
import android.content.ContextParams
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Point
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.test.runner.screenshot.ScreenCapture
import androidx.test.runner.screenshot.Screenshot.capture
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.stripe.android.PaymentConfiguration
import java.io.File
import java.io.FileOutputStream
import java.sql.Time
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.typeOf

class MainActivity : AppCompatActivity() {

    //variables para anuncio de toda la pantalla
    private var mInterstitialAd: InterstitialAd? = null
    //private final var TAG = "MainActivity"

    //variable apra comporbar que el anuncio se cargo y evitar sobrecargas
    private var unloadedAd: Boolean = true;
    //capturar la imagen
    private var bitmap: Bitmap? = null;
    //variable que comprueba si seguimos jugando
    private var gaming: Boolean = true;

    private var mHandler: Handler? = null;
    private var string_share: String = "";
    //variable apr ael control de los segundos
    private var timeInSeconds: Long = 0;
    private var width_bonus: Int = 0;

    //
    private var cellSelected_x:Int = 0;
    private var cellSelected_y:Int = 0;

    private var scoreLevel: Int = 1;
    //varibale apra saber si pasamos de nivel
    private var nextLevel:Boolean = false;
    private var level: Int = 1;
    //movimientos requeridos para que aparezca un bonus
    private var movesRequire: Int = 0;
    //moviemientos por nivel
    private var leveMoves:Int = 0;
    //cantidade  movimientos disponibles
    private var moves: Int = 0;
    private var lives: Int = 1;
    private var score_lives: Int = 1;

    //cantidad de opciones
    private var options:Int =  0;
    //cantidad de bonus
    private var bonus: Int = 0;

    private var checkMovement: Boolean = true;
    //variables d ecolores
    private var nameColorBlack: String = "black_cell";
    private var nameColorWhite: String = "white_cel";
    
    private lateinit var board: Array<IntArray>;

    private var optionBlack = R.drawable.option_black;
    private var optionWhite = R.drawable.option_white;

    //para el guardado de datos
    private lateinit var sharedPreference: SharedPreferences;
    private lateinit var editor: SharedPreferences.Editor;
    private var premium: Boolean = false; ///saber sie s premium

    //ultimo nivel
    private var LASTLEVEL: Int = 1;

    //variables apra reproducir sonidos
    private lateinit var mpMovement: MediaPlayer;
    private lateinit var mpBonus: MediaPlayer;
    private lateinit var mpGameOver: MediaPlayer;
    private lateinit var mpYouWin: MediaPlayer;


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //inciamos los sonidos o se alos cargamos
        initSounds();
        //inicalizar la pantalla
        initScreenGame();
        //inciamos el sharedpreferences
        initPreferences();
        //funcion para las publicidades
        //initAds(); //solo si no es premium
        //starGame();

    }

    //funcion par ainciar sesion
    private fun initSounds(){
        mpMovement = MediaPlayer.create(this, R.raw.sound_short);
        mpMovement.isLooping = false;

        mpBonus = MediaPlayer.create(this, R.raw.wonderful);
        mpBonus.isLooping = false;

        mpGameOver = MediaPlayer.create(this, R.raw.bad);
        mpGameOver.isLooping = false;

        mpYouWin = MediaPlayer.create(this, R.raw.goats);
        mpYouWin.setLooping(false);
    }

    //funcion para reanudar el juego
    override fun onResume() {
        super.onResume();
        checkPremium(); //ver si el usuario es premium
        starGame();
    }

    private fun initPreferences(){
        //COMPATIMOS LAS PREFERENCIAS
        sharedPreference = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        editor = sharedPreference.edit();
    }

    private fun checkPremium(){
        premium = sharedPreference.getBoolean("PREMIUM", false);//obtendra un premium o sino un null

        if (premium){//en casod e ser premium aplicamos los cambios

            LASTLEVEL = 4;
            //cargamso el nivel donde quedo
            level = sharedPreference.getInt("LEVEL", 1);
            //removemos la barra de premium, los banner y los padding de los anuncios del scroll view
            var lyPremium: LinearLayout = findViewById(R.id.lyPremium);
            lyPremium.removeAllViews();

            var lyAdsBanner:LinearLayout = findViewById(R.id.lyAdsBaner);
            lyAdsBanner.removeAllViews();

            var svGame: ScrollView = findViewById(R.id.svGame); //le quitamso el padding al scroll view
            svGame.setPadding(0,0,0,0);

            //cambiamos los colores de las casillas
            var tvLiveData: TextView = findViewById(R.id.tvLiveData);
            tvLiveData.background = getDrawable(R.drawable.bg_data_buttom_contrast_premium);

            var tvLiveTitle: TextView = findViewById(R.id.tvLevelTitle);
            tvLiveTitle.background = getDrawable(R.drawable.bg_data_top_contrast_premium);

            var vNewBonus: View = findViewById(R.id.vNewBonus);
            vNewBonus.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier("contrast_data_premium", "color", packageName)));

            nameColorBlack = "black_cell_premium";
            nameColorWhite = "white_cell_premium";

            optionBlack = R.drawable.option_black_premium;
            optionWhite = R.drawable.option_white_premium;
        }
        else{//de no ser premium dejamos anuncios y no modificamos nada
            initAds();
        }
    }

    private fun initAds(){
        MobileAds.initialize(this) {} //para las publicidades
        //fragmento donde aprecera la publicidad
        val adView = AdView(this)

        adView.setAdSize(AdSize.BANNER);

        adView.adUnitId = "ca-app-pub-3940256099942544/6300978111"

        //añadimos la vista a la seccion de publididad
        var lyAdsBanner: LinearLayout = findViewById(R.id.lyAdsBaner);
        lyAdsBanner.addView(adView);
        //cargamos el anuncio en el banner
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    //funcion para el boron de pago
    fun launchPaymentCard(vista: View){
        callPayment();
    }

    //funcion donde se realiza el pago
    private fun callPayment(){
        //variable para la clave de prubas
        var keyStripePayment: String = "pk_test_TYooMQauvdEDq54NiTphI7jx";
        PaymentConfiguration.init(applicationContext, keyStripePayment);

        //creamos el intent para apsar ela ctivity
        var intent: Intent = Intent(this, CheckoutActivity::class.java);
        intent.putExtra("level", level);//guardar nivel del jugador
        startActivity(intent);
    }

    //mostrar ese anuncio
    private fun showInerstitial(){

        if (mInterstitialAd != null) {
            unloadedAd = true //descargar el mensaje despues de mostrarlo
            mInterstitialAd?.fullScreenContentCallback = object: FullScreenContentCallback() {
                override fun onAdClicked() {
                    // Called when a click is recorded for an ad.
                    //Log.d(TAG, "Ad was clicked.")
                }

                override fun onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    //Log.d(TAG, "Ad dismissed fullscreen content.")
                    mInterstitialAd = null
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    // Called when ad fails to show.
                    //Log.e(TAG, "Ad failed to show fullscreen content.")
                    //mInterstitialAd = null
                }

                override fun onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    //Log.d(TAG, "Ad recorded an impression.")
                }

                override fun onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    //Log.d(TAG, "Ad showed fullscreen content.")
                    mInterstitialAd = null
                }
            }
            mInterstitialAd?.show(this)
        }
        //di hay anuncio ahcemos esto

    }
    private fun getReadyAds(){
        var adRequest = AdRequest.Builder().build()
        unloadedAd = false;
        InterstitialAd.load(this,"ca-app-pub-3940256099942544/1033173712", adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                //Log.d(TAG, adError?.toString())
                mInterstitialAd = null
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                //Log.d(TAG, 'Ad was loaded.')
                mInterstitialAd = interstitialAd
            }
        })
    }
    private fun initScreenGame(){
        //tamanio del la pantalla
        setSizeBoard();
        //escodenr el mensaje inicial
        hide_Message(false);//false ya que recien inica el juego
    }

    private fun setSizeBoard(){

        var iv: ImageView;

        //codigo para obtenr lso datos de la pantalla y ajustarloa a la pantalla
        var display = windowManager.defaultDisplay;
        var size = Point();
        display.getSize(size);
        val width = size.x  //le asignamos el ancho

        //ese valor de anchura lo volvemos dp de  densidad de pixeles
        var width_dp = (width / getResources().getDisplayMetrics().density);

        //valor por si se puso margenes
        var lateralMarginDP = 0;
        //restamos el margen de la pantalla y lo dividimos entre 8
        var widht_cell = (width_dp - lateralMarginDP)/8;
        var heigth_cell = widht_cell;

        //AGRANDAR EL BONUS en la pantalla
        width_bonus = 2 * widht_cell.toInt();

        for (i in 0..7){
            for (j in 0..7){
                //obtenmos el id de cada casilla del cuadro de ajedrez
                //1-la forma del id del imageview, lo segundo el el tipo que buscamos, el tercero es el contexto o de donde se debe buscar el id
                iv = findViewById(resources.getIdentifier("c$i$j", "id", packageName));

                //adminsitracion de pixeles de pantalla
                var heigth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heigth_cell, getResources().getDisplayMetrics()).toInt();
                var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widht_cell, getResources().getDisplayMetrics()).toInt();

                iv.setLayoutParams(TableRow.LayoutParams(width, heigth));
            }
        }
    }

    private fun hide_Message(start: Boolean){

        //OCULTAMOS EL MENSAJE
        var lyMessage:LinearLayout = findViewById(R.id.lyMessage);
        lyMessage.visibility = View.INVISIBLE;

        if (start) {//en caso se verda pasamos al siguiente nivel
            starGame();
        }
    }

    //Funcion para ir al siguiente nivel
    fun launchAction(vista: View){
        if (!premium && level > LASTLEVEL) callPayment();//llamar al pago premum si no es premium
        hide_Message(true);
    }

    //Funcion para compratir
    fun launchShareGame(vista: View){
        shareGame();
    }

    private fun shareGame(){
        //pedir los permisos al usuario
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), 1);
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), 1);

        //variable apra captrurar pantalla, capturara toda la pantalla con el this
        var ssc: ScreenCapture = capture(this);
        bitmap = ssc.getBitmap();

        if (bitmap != null){//en caso de que tome la captura de pantalla
            //guardamoa con la fecha
            var idGame = SimpleDateFormat("yyy/MM/dd").format(Date());
            idGame = idGame.replace(":", "");
            idGame =idGame.replace("/", "");
            val path = saveImage(bitmap, "${idGame}.jpg");
            //referenciar el recurso
            val bmpUri = Uri.parse(path);

            //creamos un intend para enviar la imagen y crear una nueva tarea
            var sharedIntent: Intent = Intent(Intent.ACTION_SEND);
            sharedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            //le pasamos el recurso
            sharedIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
            //mensaje que se envia un frase en el pie
            sharedIntent.putExtra(Intent.EXTRA_TEXT, string_share);
            sharedIntent.type = "image/png";

            //elegir el tipo de medio para compartir el puntaje
            var finalSharedIntent: Intent = Intent.createChooser(sharedIntent, "Select the appyou want to share the game");
            finalSharedIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(finalSharedIntent);//lanzammos la peticion a usario

        }
    }

    //funcion para guardar imagenes
    private fun saveImage(bitmap: Bitmap?, fileName: String): String?{
        if (bitmap == null) return null; //si esta vacio no retornar nada

        //comprobamos el sdk y si esque sepeude guardar las imagenes
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q){
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Screenshots");
            }

            val uri = this.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null){
                this.contentResolver.openOutputStream(uri).use {
                    if (it == null) return@use;

                    bitmap.compress(Bitmap.CompressFormat.PNG, 85, it); //TRANSFORMAMOS AL FORMATO Y SU CALIDAD RESPECTIVA
                    it.flush();
                    it.close();

                    //añadir un pic a al galeria
                    MediaScannerConnection.scanFile(this, arrayOf(uri.toString()), null, null);
                }
            }

            return  uri.toString();
        }
        //iondicamos la nueva ruta del directorio
        val filePath = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES + "/Screenshots"
        ).absolutePath;

        val dir = File(filePath);
        if (!dir.exists()) dir.mkdirs();
        val file = File(dir, fileName);
        val fOut = FileOutputStream(file);

        bitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);
        fOut.flush();
        fOut.close();

        //add pic a al galeria
        MediaScannerConnection.scanFile(this, arrayOf(file.toString()), null, null);
        return filePath;
        //SE REALIZO ESE PROCEDIMIENTO 2 VECES APRA ASEGURARSE QUE FUNCIONE EN TODOS LOS DIPODITIVOS
    }


    //funcion par ael clic en los imageview
    fun checkChellClicked(vista: View){
        //obtenmos las posiciones y la volvemos enteros para luego selccioanrlas
        var name:String = vista.tag.toString();
        var x = name.subSequence(1,2).toString().toInt();
        var y = name.subSequence(2,3).toString().toInt();

        //selectCell(x, y);
        checkCell(x, y);
    }

    //funcion para detectar si la celda donde se posicona el caballoe  correcta
    private fun checkCell(x: Int, y: Int){

        var checkTrue = true; //para verificar si la posicion es correcta

        if (checkMovement){//comprobar si aun se pude mover
            //obtenmos las distancia de la actual posicion con la antigua
            var dif_x:Int = x - cellSelected_x;
            var dif_y: Int = y - cellSelected_y;

            //ver si se mueve en L
            checkTrue = false; //para verificar si la posicion es correcta
            if (dif_x == 1 && dif_y == 2) checkTrue = true;
            if (dif_x == 1 && dif_y == -2) checkTrue = true;
            if (dif_x == 2 && dif_y == 1) checkTrue = true;
            if (dif_x == 2 && dif_y == -1) checkTrue = true;
            if (dif_x == -1 && dif_y == 2) checkTrue = true;
            if (dif_x == -1 && dif_y == -2) checkTrue = true;
            if (dif_x == -2 && dif_y == 1) checkTrue = true;
            if (dif_x == -2 && dif_y == -1) checkTrue = true;
        }
        else{//en caso se quede sin movimientos comprobar si tiene bonus y moverse
            if (board[x][y] != 1){
                bonus--;
                var tvBonusData: TextView = findViewById(R.id.tvBonusData);
                tvBonusData.text = " + $bonus"; //actuilizamos el bonus
                if (bonus == 0) tvBonusData.text = "";
            }
        }


        //evitar que use posiciones ya selccioandas
        if (board[x][y] == 1) checkTrue = false;

        //si el verifivador es verdadero entonces se puede selecionar
        if (checkTrue) selectCell(x, y);
    }
    private fun selectCell(x:Int, y:Int){

        //actualizamos los movimientos disponibles para el usuario
        moves--;
        var tvMovesData: TextView = findViewById(R.id.tvMovesData);
        tvMovesData.text = moves.toString();

        //funcion par ael progreso del bonus
        growProgressBonus();

        //Comprobamos si la posicion del tablero es un bonus
        Toast.makeText(this, "valor-${board[x][y]}", Toast.LENGTH_SHORT).show()
        if (board[x][y] == 2){
            Toast.makeText(this@MainActivity, "entro en la casilla $x-$y", Toast.LENGTH_SHORT).show()
            bonus++;
            //aumentamos visualizacion al bonus de la interfaz
            var tvBonusData: TextView = findViewById(R.id.tvBonusData);
            tvBonusData.text = " + $bonus";

            //En caso de ser un bonus reproducimos el bonus
            mpBonus.start();
        }
        else{//en caso de no ser bonus reproducimos el sonido de movimiento
            mpMovement.start();
        }

        //cambiamos valor de  la matriz del tablero en 1
        board[x][y] = 1;
        //ubicamos la posicion del tableero

        //pintar d=celda anterior
        paintHorseCell(cellSelected_x, cellSelected_y, "previus_cell");

        //actualizamos las celdas
        cellSelected_x = x;
        cellSelected_y = y

        //limpiar celdas para no ver las opciones antiguas
        clearOptions();
        //celda actual
        paintHorseCell(x, y, "selected_cell");
        //dejar como positivo el movememnt
        checkMovement = true;
        //ver opciones para moverse
        checkOptions(x, y);

        //dar opciones caso sigamso con moviemientos disponibles
        if (moves > 0){
            checkNewBonus();
            checkGameOver();
        }
        //en caso de que pase al siguiente nivel
        else showMessage("You Win!!", "Next level", false, false);
    }

    private fun resetBoard(){
        board = arrayOf(
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
            intArrayOf(0,0,0,0,0,0,0,0),
        )
    }

    private fun clearBoard(){
        var iv: ImageView

        var colorBlack = ContextCompat.getColor(this,
            resources.getIdentifier(nameColorBlack, "color", packageName));
        var colorWhite = ContextCompat.getColor(this,
        resources.getIdentifier(nameColorWhite, "color", packageName))

        //ahira recorremos cada posicon del tablero para ver si es de color blanco o negro
        //tambien se quitan los fondos de la imagen
        for (i in 0..7){
            for (j in 0..7){
                iv = findViewById(resources.getIdentifier("c$i$j", "id", packageName));//recorrer cada id
                //iv.setImageResource(R.drawable.horse);
                iv.setImageResource(0);

                if (checkColorCell(i, j) == "black") iv.setBackgroundColor(colorBlack);
                else iv.setBackgroundColor(colorWhite);
            }
        }
    }


    private fun setFirtsPosition(){
        //posicones aleatoria del caballo inicial
        var x:Int = 0;
        var y:Int = 0;

        //evitar que tome  las posicones de las columans marcadas
        var firstPosition: Boolean = false;
        //recoremos el tablero con posiciones aleatorias hasta encontrar un aubuicacion disponible
        while (!firstPosition){
            x = (0..7).random();
            y = (0..7).random();

            if (board[x][y] == 0) firstPosition = true;
            //comprobar si tiene opciones
            checkOptions(x, y);
            if (options == 0) firstPosition = false;
        }


        cellSelected_x = x;
        cellSelected_y = y;
        selectCell(x, y);
    }


    /*****FUNCIONES PARA LOS NIVELES*******/
    //Funcnion para poner el nivel del juego
    private fun setLevel(){
        if (nextLevel){//si pasas al siguiente nivel
            level++;
            if (premium) lives = 9999999; //en caso de ser premium darle esa vidas
            else lives++;
        }
        else{//en caso de perder ene se nivel pierdes vidas
            if (!premium) {
                lives--;
                if (lives < 1) {//si pierdes todas las vidas regresas al primer nivel
                    level = 1;
                    lives = 1;
                }
            }
        }
    }

    //Mostramos los parametros de cada nivel
    private fun setLevelParameters(){
        //Mostramos las vidas del usuaio
        var tvLiveData: TextView = findViewById(R.id.tvLiveData);
        tvLiveData.text = lives.toString();
        //score_lives = lives;
        if (premium) tvLiveData.text ="infinito"; ///solo en caso de ser premium

        //mostrar el nivel al jugador
        var tvLevelNumber: TextView = findViewById(R.id.tvLevelNumber);
        tvLevelNumber.text = level.toString();
        //scoreLevel = level;

        //mostrar bonus al usario
        var tvBonusData: TextView = findViewById(R.id.tvBonusData);
        tvBonusData.text = "";

        //establecer numero de movimientos que tiene el nivel
        setLevelMoves();
        moves = leveMoves;

        movesRequire = setMovesRequired();
    }

    //Poner la cantidad de moviemientos dependiendo del nivel
    private fun setLevelMoves(){
        when(level){
            1-> leveMoves = 64;
            2-> leveMoves = 56;
            3-> leveMoves = 32;
            4-> leveMoves = 16;
            5-> leveMoves = 48;
            6-> leveMoves = 36;
            7-> leveMoves = 48;
            8-> leveMoves = 49;
            9-> leveMoves = 59;
            10-> leveMoves = 48;
            11-> leveMoves = 64;
            12-> leveMoves = 48;
            13-> leveMoves = 48;
        }
    }

    //Funcion para saber cada cuanto debe salir un bonus por nivel
    private fun setMovesRequired(): Int{
        var movesRequired = 0;

        when(level){//nivel del usuario
            1->movesRequired = 8;
            2->movesRequired = 10;
            3->movesRequired = 12;
            4->movesRequired = 10;
            5->movesRequired = 10;
            6->movesRequired = 12;
            7->movesRequired = 5;
            8->movesRequired = 7;
            9->movesRequired = 9;
            10->movesRequired = 8;
            11->movesRequired = 1000;
            12->movesRequired = 5;
            13->movesRequired = 5;
        }
        return  movesRequired;
    }

    //poner aspecto de cada tablero por nivel
    private fun setBoardLevel(){
        when (level){
            2->paintLevel_2();
            3->paintLevel_3();
            4->paintLevel_4();
            5->paintLevel_5();
            6->paintLevel_6();
            7->paintLevel_7();
            8->paintLevel_8();
            9->paintLevel_9();
            10->paintLevel_10();
            11->paintLevel_11();
            12->paintLevel_12();
            13->paintLevel_13();
        }
    }

    /********BONUS FUNCIONES**********/
    //comprobar para lanzar nuevo bonus
    private fun checkNewBonus(){
        //ver si alcanxo lso movientos necesarios
        if (moves % movesRequire == 0){
            var bonusCell_x:Int = 0;
            var bonusCell_y: Int = 0;

            var bonusCell: Boolean = false; //sabes sie ncontro un cuadrado vacio para el bonus

            while (bonusCell == false){
                //numero aleatorios par alas casillas
                bonusCell_x = (0..7).random();
                bonusCell_y = (0..7).random();

                if (board[bonusCell_x][bonusCell_y] == 0) bonusCell = true;
            }

            //cambiamos el valor de la cailla a 2 y el simbolo en aparecer ahi
            board[bonusCell_x][bonusCell_y] = 2;
            Toast.makeText(this, "celda valor ${board[bonusCell_x][bonusCell_y]}", Toast.LENGTH_SHORT).show()
            paintBonusCell(bonusCell_x, bonusCell_y);
        }
    }

    //FUNCION para pintar las casillas de bonus
    private fun paintBonusCell(x: Int, y:Int){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName));
        iv.setImageResource(R.drawable.bonus);
    }

    //Funcion de progreso
    private fun growProgressBonus(){
        //levamso la cnatidad de bonus hechos  y de movimientos disponibles
        var moves_done = leveMoves - moves;
        var bonus_done = moves_done / movesRequire;
        var moves_rest = movesRequire * (bonus_done);
        var bonus_grow = moves_done - moves_rest;

        var v:View = findViewById(R.id.vNewBonus); //barra de bonus
        //de debe aumentar la anchura de la barra para el bonus
        var widthBonus:Float =  ((width_bonus / movesRequire) * bonus_grow).toFloat();

        var height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, getResources().getDisplayMetrics()).toInt();
        var width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthBonus, getResources().getDisplayMetrics()).toInt();

        v.setLayoutParams(TableRow.LayoutParams(width, height));
    }


    /*******OPCIONES FUNCIONES********/
    //limpiar los coleres del tablero en la aprte grafica
    private fun clearOption(x: Int, y: Int){
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName));
        if (checkColorCell(x, y) == "black")
            iv.setBackgroundColor(ContextCompat.getColor(this,
                resources.getIdentifier(nameColorBlack, "color", packageName)));
        else
            iv.setBackgroundColor(ContextCompat.getColor(this,
                resources.getIdentifier(nameColorWhite, "color", packageName)));

        //en caso que la casilla ya sea una usada
        if (board[x][y] == 1)
            iv.setBackgroundColor(ContextCompat.getColor(this,
                resources.getIdentifier("previus_cell", "color", packageName)));

    }

    private fun clearOptions() {
        for (i in 0..7){
            for (j in 0..7){
                if (board[i][j] == 9 || board[i][j] == 2 ){
                    //solo cambaimos el 9
                    if (board[i][j] == 9)
                        board[i][j] = 0;
                    clearOption(i, j);
                }
            }
        }
    }

    //Funcion para pintar opcion de casillas disponibles
    private fun paintOptions(x: Int, y: Int) {
        var iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName));
        //compromabmso si la celda el blanca o negra
        if (checkColorCell(x, y) == "black") iv.setBackgroundResource(optionBlack);
        else iv.setBackgroundResource(optionWhite);
    }
    //Pintar las opciones diponibles para moverse
    private fun paintAllOptions(){
        //recorremos to_do el tablero buscando opciones disponibles
        for (i in (0..7)){
            for (j in (0..7)){
                if (board[i][j] != 1) paintOptions(i, j);
                if (board[i][j] == 0) board[i][j] = 9
            }
        }
    }

    /***PINTAR LOS TABLERO DE ACUEDO A LOS NIVELES****/
    private fun paint_Column(column: Int){
        for (i in 0..7){
            board[column][i] = 1
            paintHorseCell(column, i, "previus_cell");
        }
    }
    private  fun paint_Row(row: Int){
        for (i in 0..7){
            board[i][row] = 1;
            paintHorseCell(i, row, "previus_cell");
        }
    }
    private fun paint_Diagonal(){
        for (i in 0..7){
            for (j in 0..7){
                board[i][j] = 1;
                paintHorseCell(i, j, "previus_cell");
            }
        }
    }
    private fun paint_InDiagonal(){
        for (i in 7 downTo 0){
            for (j in 7 downTo 0){
                board[i][j] = 1;
                paintHorseCell(i, j, "previus_cell");
            }
        }
    }
    //Pintamos los diferente niveles
    private fun paintLevel_2(){
        paint_Column(6);
    }
    private fun paintLevel_3(){
        for (i in 0..7){
            for (j in 4..7){
                board[j][i] = 1
                paintHorseCell(j ,i, "previus_cell")
            }
        }
    }
    private fun paintLevel_4(){
        paintLevel_3();
        paintLevel_5();
    }
    private fun paintLevel_5(){
        for (i in 0..3){
            for (j in 0..3){
                board[i][j] = 1;
                paintHorseCell(j, i, "previus_cell")
            }
        }

    }
    private  fun paintLevel_6(){
        for (i in 5..7){
            for (j in 1..3){
                board[i][j] = 1;
                paintHorseCell(j, i, "previus_cell")
            }
        }
    }
    private  fun paintLevel_7(){
        paintLevel_2();
        paintLevel_4();
    }
    private fun paintLevel_8(){
        paint_Row(5);
    }
    private fun paintLevel_9(){
        paint_Row(4)
        paint_Diagonal();
    }
    private fun paintLevel_10(){
        paint_Column(4)
        paint_InDiagonal();
    }
    private fun paintLevel_11(){
        paint_Diagonal()
        paint_InDiagonal();
    }
    private fun paintLevel_12(){
        paintLevel_6();
        paintLevel_9();
    }
    private fun paintLevel_13(){
        paintLevel_12();
        paintLevel_5();
    }
    //Funcion para ver si el juego acabo
    private fun checkGameOver() {

        if(options == 0){
            if (bonus > 0){
                checkMovement = false;
                paintAllOptions();
            }
            else {
                showMessage("Game Over", "Try Again", true, false);
            }
        }
    }


    //Funcion par amsotrar el mesaje
    private fun showMessage(title: String, action: String, gameOver: Boolean, endGame: Boolean) {
        gaming = false;
        //actualizar el netlevel para pasar de siguiente nivel
        nextLevel = !gameOver;
        //hacemos que el layput del mensaje este visible
        var lyMessage:LinearLayout = findViewById(R.id.lyMessage);
        lyMessage.visibility = View.VISIBLE;

        //mostar el titulo en la casilla
        var tvTitleMessage: TextView = findViewById(R.id.tvTitleMessage);
        tvTitleMessage.text = title;

        //mostar el score del mensaje, vemos si el game over
        var tvTimeData: TextView = findViewById(R.id.tvTimeData);
        var score: String = "";
        if (gameOver){

            mpGameOver.start(); //reproducir sonido se perdiste
            if (!premium) {//solo en caso de no ser premium
                showInerstitial();//mostrar el anuncio cunaod se pierde
            }
            score = "Score: " + (leveMoves - moves) + "/" + leveMoves;
            string_share = "This game makes me crazy !!!" + score + ") https://github.com/luisfe12";
        }
        else{//en caso de pasar de nivel

            mpYouWin.start(); //reproducir mensaje de ganaste
            score = tvTimeData.text.toString();
            string_share = "Let's go !! New Challenge Completed Level: $level (" + score + ") https://github.com/luisfe12";
        }

        if (endGame) score = ""

        var tvScoreMessage: TextView = findViewById(R.id.tvScoreMessage);
        tvScoreMessage.text = score;
        //mostrar el action en el mensaje
        var tvAction: TextView = findViewById(R.id.tvAction);
        tvAction.text = action;
    }






    /*****CHECK FUNCIONES****/
    private fun checkOptions(x: Int, y: Int) {
        options = 0;
        //opcion para poder ver cada uno de los movimintos disponibles
        checkMove(x, y, 1, 2);
        checkMove(x, y, 2, 1);
        checkMove(x, y, 1, -2);
        checkMove(x, y, 2, -1);
        checkMove(x, y, -1, 2);
        checkMove(x, y, -2, 1);
        checkMove(x, y, -1, -2);
        checkMove(x, y, -2, -1);

        var tvOptionsData: TextView = findViewById(R.id.tvOptionsData);
        //mandamos la cantidad a la opcion de text
        tvOptionsData.text = options.toString();
    }

    //los aprametros son la posicion actual y hacia donde podemos ir
    private fun checkMove(x: Int, y: Int, mov_x: Int, mov_y: Int) {
        var options_x = x + mov_x;
        var options_y = y + mov_y;

        //comprobamos que la posicion no salga del cuadro
        if (options_x < 8 && options_y < 8 && options_x >= 0 && options_y >= 0){
            //comprobemos que tales casillas esten libre s o sean bonus
            if (board[options_x][options_y] == 0 || board[options_x][options_y] == 2){
                options++;
                paintOptions(options_x, options_y);

                //amrcar solo como opcion si esta esta vacia
                if (board[options_x][options_y] == 0) {
                    board[options_x][options_y] = 9;
                }
            }
        }
    }

    //funcion para elegir si la casilla es negra o blanca
    private fun checkColorCell(x: Int, y: Int): String{
        //arreglos de posibels ubicacioens en el tablero
        var color:String =""
        var blackColum_x: Array<Int> = arrayOf(0,2,4,6);
        var blackRow_x: Array<Int> = arrayOf(1,3,5,7);
        //detectamos que este en la posiciones de los negros
        if ((blackColum_x.contains(x) && blackColum_x.contains(y))||(blackRow_x.contains(x) && blackRow_x.contains(y)))
            color = "black";
        else
            color = "white";
        return color;
    }

    private fun paintHorseCell(x: Int, y:Int, color:String){
        var  iv: ImageView = findViewById(resources.getIdentifier("c$x$y", "id", packageName));
        //ponemos el color
        iv.setBackgroundColor(ContextCompat.getColor(this, resources.getIdentifier(color, "color", packageName)));
        //ponemso el icono
        iv.setImageResource(R.drawable.horse);
    }

    private fun resetTime(){
        //invereos al star llmamos al cronometro y los ponemos en 0
        mHandler?.removeCallbacks(chronometer);
        timeInSeconds = 0;

        var tvTimeData: TextView  = findViewById(R.id.tvTimeData);
        tvTimeData.text = "00:00";
    }
    private fun startTime(){
        //usamos looper referenciamos el hadler para poder usarlo
        //le hacemos un run
        mHandler = Handler(Looper.getMainLooper());
        chronometer.run();

    }

    private var chronometer: Runnable = object : Runnable{
        override fun run(){
            try {
                //DETECTAR SI ESTAMOS JUGANDO
                if (gaming) {
                    //aumentamos los segundos
                    timeInSeconds++;
                    //mostrar esos segundos al usuario
                    updateStopWatchView(timeInSeconds);
                }
            }finally {
                //intervalo de tiempo entre ejecucion y otra, 1000 milisegundos
                mHandler!!.postDelayed(this, 1000L)
            }
        }
    }

    private fun updateStopWatchView(timeInSeconds: Long){
        //tranformammos esos segundo en un string
        //lo multiplica por 1000 para volverlos milisegundos
        var formattedTime: String = getFormattedStopWatch((timeInSeconds * 1000))
        //mostramos en el textview los segundos
        var tvTimeData: TextView = findViewById(R.id.tvTimeData);
        tvTimeData.text = formattedTime;

    }

    private fun getFormattedStopWatch(ms: Long): String{
        var milliseconds: Long = ms;
        var minutes = TimeUnit.MILLISECONDS.toMinutes(milliseconds);
        milliseconds -= TimeUnit.MINUTES.toMillis(minutes);
        val seconds = TimeUnit.MILLISECONDS.toSeconds(milliseconds);

        //retorna al acantidad de minutos y segundos que hay
        return "${if (minutes < 10) "0" else ""} $minutes:" +
                "${if (seconds < 10) "0" else ""} $seconds"
    }



    private fun starGame(){

        //descargar los anuncios solo si no es premium
        if (unloadedAd == true && premium == false) getReadyAds();
        //estabelcer el nivel del juego
        setLevel()
        //level = 4;
        if (level > LASTLEVEL){
            if (premium){
                showMessage("You have been beaten the game !!!", "Wait for more level", false, true);
            }
            else{
                showMessage("More level only with PREMIUM access", "Get premium accees", false, true);
            }
        }
        else {
            setLevelParameters();

            resetBoard();
            //limoiar tablero
            clearBoard();

            setBoardLevel();
            //posicion aleatoria del caballo
            setFirtsPosition();

            //lamos a las funciones del tiempo
            resetTime();
            startTime();
            gaming = true;
        }
    }

}