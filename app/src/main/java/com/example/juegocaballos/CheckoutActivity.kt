package com.example.juegocaballos

//package com.example.demo

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.Log
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.Api.AnyClient
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

//librerias usadas par las coleccioens de datos
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CheckoutActivity : AppCompatActivity() {
    companion object {
        //private const val TAG = "CheckoutActivity"
        //private const val BACKEND_URL = "https://stripehorsegm.up.railway.app"
        private const val BACKEND_URL = "http://10.0.2.2:3000"
    }

    private lateinit var paymentIntentClientSecret: String
    private lateinit var paymentSheet: PaymentSheet

    private lateinit var payButton: Button

    //cariable apra el nivel
    private var level: Int? = 1;
//    private lateinit var addressLauncher: AddressLauncher

//    private var shippingDetails: AddressDetails? = null
//
//    private lateinit var addressButton: Button
//
//    private val addressConfiguration = AddressLauncher.Configuration(
//        additionalFields: AddressLauncher.AdditionalFieldsConfiguration(
//            phone: AdditionalFieldsConfiguration.FieldConfiguration.Required
//    ),
//    allowedCountries: setOf(“US”, “CA”, “GB”),
//    title: “Shipping Address”,
//    googlePlacesApiKey = “(optional) YOUR KEY HERE”
//    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_checkout)

        //valor para ellevel
        val bundle = intent.extras;
        level = bundle?.getInt("level");
        if (level == null) level = 1; //encaso no tenga nivel

        // Hook up the pay button
        payButton = findViewById(R.id.pay_button)
        payButton.setOnClickListener(::onPayClicked)
        payButton.isEnabled = false

        paymentSheet = PaymentSheet(this, ::onPaymentSheetResult)

        // Hook up the address button
//        addressButton = findViewById(R.id.address_button)
//        addressButton.setOnClickListener(::onAddressClicked)
//
//        addressLauncher = AddressLauncher(this, ::onAddressLauncherResult)

        fetchPaymentIntent(); //funcion que prepara ya el pago
    }

    private fun fetchPaymentIntent() {
        val url = "$BACKEND_URL/create-payment-intent" //ruta para hacer el pago

//        val shoppingCartContent = """
//            {
//                "items": [
//                    {"id":"xl-tshirt"}
//                ]
//            }
//        """
        val amount: Float = 100.0f; //cantidad de cobro 100 centimos
        val payMap: MutableMap<String, Any>  = HashMap(); //Prodcuto y la cantidad
        val itemMap: MutableMap<String, Any> = HashMap(); //
        val itemLit: MutableList<Map<String, Any>> = ArrayList(); //lista de articulos del carro de compra
        payMap["currency"] = "uds"; //moneda en dolares
        itemMap["id"] = "photo_subscription"; //foto del articulo
        itemMap["amount"] = amount; //cantidad
        itemLit.add(itemMap);//se añade este articulo al mapa
        payMap["items"] = itemLit; //map de articulos comprados

        val shoppingCartContent =Gson().toJson(payMap);

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val body = shoppingCartContent.toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()//hacemos la peticion

        OkHttpClient()
            .newCall(request)
            .enqueue(object: Callback {
                override fun onFailure(call: Call, e: IOException) {
                    showAlert("Failed to load data", "Error: $e")
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        showAlert("Failed to load page", "Error: $response")
                    } else {
                        val responseData = response.body?.string()
                        val responseJson = responseData?.let { JSONObject(it) } ?: JSONObject()
                        paymentIntentClientSecret = responseJson.getString("clientSecret")
                        runOnUiThread { payButton.isEnabled = true }
                        //Log.i(TAG, "Retrieved PaymentIntent")
                    }
                }
            })
    }

    private fun showAlert(title: String, message: String? = null) {
        runOnUiThread {
            val builder = AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
            builder.setPositiveButton("Ok", null)
            builder.create().show()
        }
    }

    private fun showSnackBar(message: String, duration: Int) {
//        runOnUiThread {
//            Toast.makeText(this,  message, Toast.LENGTH_LONG).show()
//        }
        //el snack tiene un diseño mas bonito en la barra
        val mySnackbar: Snackbar = Snackbar.make(findViewById(R.id.lyMain), message, duration);
        mySnackbar.show();
    }

    private fun onPayClicked(view: View) {
        Toast.makeText(this, "clickeo boton", Toast.LENGTH_SHORT).show()
        //despeus de precionar mostrara ventana de pagos
        val configuration = PaymentSheet.Configuration("Example, Inc.")

        // Present Payment Sheet
        paymentSheet.presentWithPaymentIntent(paymentIntentClientSecret, configuration)
    }

//    private fun onAddressClicked(view: View) {
//        addressLauncher.present(
//            publishableKey = publishableKey,
//            configuration = addressConfiguration
//        )
//    }

    private fun onPaymentSheetResult(paymentResult: PaymentSheetResult) {//diferentes mensajes conr especto al pago
        when (paymentResult) {
            is PaymentSheetResult.Completed -> {
                //showSnackBar("Payment complete!", Snackbar.LENGTH_SHORT);
                becamePremium(); //funcion cuando el usuario se vuelva premiun
            }
            is PaymentSheetResult.Canceled -> {
                //Log.i(TAG, "Payment canceled!")
                showSnackBar("Payment Canceled Try again", Snackbar.LENGTH_SHORT);
            }
            is PaymentSheetResult.Failed -> {
                //showAlert("Payment failed", paymentResult.error.localizedMessage)
                showSnackBar("Payment Failed Try again", Snackbar.LENGTH_SHORT);
            }
        }
    }

//    private fun onAddressLauncherResult(result: AddressLauncherResult) {
//        // TODO: Handle result and update your UI
//        when (result) {
//            AddressLauncherResult.Success -> {
//                shippingDetails = result.address
//            }
//            AddressLauncherResult.Canceled -> {
//                // TODO: Handle cancel
//            }
//        }
//    }
    private fun becamePremium(){
        //guardamso los datos del juego para despues enviarlos al main activity
        //COMPATIMOS LAS PREFERENCIAS
        var sharedPreferences: SharedPreferences;
        sharedPreferences =  getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        var editor = sharedPreferences.edit();
        editor.apply{
            putBoolean("PREMIUM", true);
            putInt("LEVEL", level!!);//para que no sea nulo el nivel
        }.apply()

        //volvemos a la pestaña del juego
        val intent: Intent = Intent(this, MainActivity::class.java);
        startActivity(intent);

    }
}