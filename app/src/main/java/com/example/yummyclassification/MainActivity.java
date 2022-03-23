package com.example.yummyclassification;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.yummyclassification.app.AppController;
import com.example.yummyclassification.app.Server;
import com.example.yummyclassification.ml.Model;

import org.json.JSONException;
import org.json.JSONObject;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    ProgressDialog pDialog;
    Button camera, gallery, simpan;
    ImageView imageView;
    TextView result, tgll, resulttgl;
    EditText nomor;
    int imageSize = 384;
    Intent intent;
    int success;
    ConnectivityManager conMgr;
    private String url = Server.URL + "classification.php";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String TAG_SUCCESS = "success";
    private static final String TAG_MESSAGE = "message";
    String tag_json_obj = "json_obj_req";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        {
            if (conMgr.getActiveNetworkInfo() != null
                    && conMgr.getActiveNetworkInfo().isAvailable()
                    && conMgr.getActiveNetworkInfo().isConnected()) {
            } else {
                Toast.makeText(getApplicationContext(), "No Internet Connection",
                        Toast.LENGTH_LONG).show();
            }
        }

        setContentView(R.layout.activity_main);
        camera = findViewById(R.id.button);
        gallery = findViewById(R.id.button2);
        simpan = findViewById(R.id.button3);
        nomor = findViewById(R.id.nomor);
        result = findViewById(R.id.result);
        imageView = findViewById(R.id.imageView);
        tgll = findViewById(R.id.tgl);
        resulttgl = findViewById(R.id.resulttgl);

        //set textview for date
        Date today = Calendar.getInstance().getTime();//getting date
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");//formating according to my need
        String date = formatter.format(today);
        resulttgl.setText(date);

        //disable edittext nomorpesanan
        nomor.setEnabled(false);
        nomor.setVisibility(View.INVISIBLE);
        //disable button simpan
        simpan.setEnabled(false);
        simpan.setVisibility(View.INVISIBLE);
        //invisible textview tgll&result tgl
        tgll.setVisibility(View.INVISIBLE);
        resulttgl.setVisibility(View.INVISIBLE);

        nomor.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.toString().trim().length()==0){
                    simpan.setEnabled(false);
                    simpan.setVisibility(View.INVISIBLE);
                } else {
                    simpan.setEnabled(true);
                    simpan.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,int after) {
                // TODO Auto-generated method stub
            }
            @Override
            public void afterTextChanged(Editable s) {
                // TODO Auto-generated method stub
            }
        });

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    startActivityForResult(cameraIntent, 3);
                } else {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
                }
            }
        });

        gallery.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent cameraIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(cameraIntent, 1);
            }
        });

        simpan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Auto-generated method stub
                String noorder = nomor.getText().toString();
                String menu = result.getText().toString();
                String tgl = resulttgl.getText().toString();
                if (conMgr.getActiveNetworkInfo() != null && conMgr.getActiveNetworkInfo().isAvailable() && conMgr.getActiveNetworkInfo().isConnected()) {
                        checkSimpan(noorder, menu, tgl);
                } else {
                    Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void checkSimpan(final String noorder, final String menu, final String tgl) {
        pDialog = new ProgressDialog(this);
        pDialog.setCancelable(false);
        pDialog.setMessage("Simpan...");
        showDialog();

        StringRequest strReq = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                Log.e(TAG, "Save Response: " + response.toString());
                hideDialog();

                try {
                    JSONObject jObj = new JSONObject(response);
                    success = jObj.getInt(TAG_SUCCESS);

                    // Check for error node in json
                    if (success == 1) {

                        Log.e("Successfully Save!", jObj.toString());

                        Toast.makeText(getApplicationContext(),
                                jObj.getString(TAG_MESSAGE), Toast.LENGTH_LONG).show();
                        finish();
                        startActivity(getIntent());
                    } else {
                        Toast.makeText(getApplicationContext(),
                                jObj.getString(TAG_MESSAGE), Toast.LENGTH_LONG).show();

                    }
                } catch (JSONException e) {
                    // JSON error
                    e.printStackTrace();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Save Error: " + error.getMessage());
                Toast.makeText(getApplicationContext(),
                        error.getMessage(), Toast.LENGTH_LONG).show();

                hideDialog();

            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                // Posting parameters to login url
                Map<String, String> params = new HashMap<String, String>();
                params.put("noorder", noorder);
                params.put("menu", menu);
                params.put("tgl", tgl);
                return params;
            }

        };

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(strReq, tag_json_obj);
    }

    private void showDialog() {
        if (pDialog.isShowing())
            pDialog.show();
    }

    private void hideDialog() {
        if (pDialog.isShowing())
            pDialog.dismiss();
    }

    public void classifyImage(Bitmap image){
        try {
            Model model = Model.newInstance(getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 384, 384, 3}, DataType.FLOAT32);
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * imageSize * imageSize * 3);
            byteBuffer.order(ByteOrder.nativeOrder());

            int[] intValues = new int[imageSize * imageSize];
            image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());
            int pixel = 0;
            //iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
            for(int i = 0; i < imageSize; i ++){
                for(int j = 0; j < imageSize; j++){
                    int val = intValues[pixel++]; // RGB
                    byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255.f));
                    byteBuffer.putFloat((val & 0xFF) * (1.f / 255.f));
                }
            }

            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);
            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

            float[] confidences = outputFeature0.getFloatArray();
            // find the index of the class with the biggest confidence.
            int maxPos = 0;
            float maxConfidence = 0;
            for (int i = 0; i < confidences.length; i++) {
                if (confidences[i] > maxConfidence) {
                    maxConfidence = confidences[i];
                    maxPos = i;
                }
            }
            Log.d("ADebugTag", "maxpos: " + maxPos);
            Log.d("ADebugTag", "maxconfidence: " + maxConfidence);
            String[] classes = {"adobo", "ambrosia_food", "apple_pie", "apple_turnover",
                    "applesauce", "applesauce_cake", "baby_back_rib", "bacon_and_eggs",
                    "bacon_lettuce_tomato_sandwich", "baked_alaska", "baklava",
                    "barbecued_spareribs", "barbecued_wing", "beef_bourguignonne",
                    "beef_carpaccio", "beef_stroganoff", "beef_tartare",
                    "beef_wellington", "beet_salad", "beignet", "bibimbap", "biryani",
                    "blancmange", "boiled_egg", "boston_cream_pie", "bread_pudding",
                    "brisket", "bruschetta", "bubble_and_squeak", "buffalo_wing",
                    "burrito", "caesar_salad", "cannelloni", "cannoli",
                    "caprese_salad", "carbonnade_flamande", "carrot_cake", "casserole",
                    "ceviche", "cheesecake", "chicken_cordon_bleu", "chicken_curry",
                    "chicken_kiev", "chicken_marengo", "chicken_provencale",
                    "chicken_quesadilla", "chicken_wing", "chiffon_cake", "chili",
                    "chocolate_cake", "chocolate_mousse", "chop_suey", "chow_mein",
                    "churro", "clam_chowder", "clam_food", "club_sandwich",
                    "cockle_food", "coconut_cake", "coffee_cake", "compote", "confit",
                    "coq_au_vin", "coquilles_saint_jacques", "cottage_pie", "couscous",
                    "crab_cake", "crab_food", "crayfish_food", "creme_brulee",
                    "croque_madame", "croquette", "cruller", "crumb_cake", "crumpet",
                    "cupcake", "custard", "deviled_egg", "dolmas", "donut", "dumpling",
                    "eccles_cake", "edamame", "egg_roll", "eggs_benedict", "enchilada",
                    "entrecote", "escargot", "falafel", "farfalle", "fettuccine",
                    "filet_mignon", "fish_and_chips", "fish_stick", "flan",
                    "foie_gras", "fondue", "french_fries", "french_onion_soup",
                    "french_toast", "fried_calamari", "fried_egg", "fried_rice",
                    "frittata", "fritter", "frozen_yogurt", "fruitcake", "galantine",
                    "garlic_bread", "gingerbread", "gnocchi", "greek_salad",
                    "grilled_cheese_sandwich", "grilled_salmon", "guacamole", "gyoza",
                    "gyro", "haggis", "ham_and_eggs", "ham_sandwich", "hamburger",
                    "hot_and_sour_soup", "hot_dog", "huevos_rancheros", "huitre",
                    "hummus", "ice_cream", "jambalaya", "jerky", "kabob", "kedgeree",
                    "knish", "lasagna", "limpet_food", "linguine", "lobster_bisque",
                    "lobster_food", "lobster_roll_sandwich", "lobster_thermidor",
                    "lutefisk", "macaron", "macaroni_and_cheese", "manicotti",
                    "marble_cake", "matzo_ball", "meat_loaf_food", "meatball",
                    "miso_soup", "moo_goo_gai_pan", "mostaccioli", "moussaka",
                    "mussel", "nacho", "omelette", "onion_rings", "orzo", "osso_buco",
                    "oyster", "pad_thai", "paella", "pancake", "panna_cotta", "pate",
                    "pavlova", "peach_melba", "peking_duck", "penne", "pepper_steak",
                    "pho", "pilaf", "pirogi", "pizza", "poached_egg", "poi",
                    "pork_chop", "porridge", "potpie", "pound_cake", "poutine",
                    "prime_rib", "profiterole", "pulled_pork_sandwich", "ramen",
                    "ravioli", "red_velvet_cake", "reuben", "rigatoni", "risotto",
                    "rissole", "rock_cake", "roulade", "rugulah", "salisbury_steak",
                    "samosa", "sashimi", "sauerbraten", "sauerkraut", "sausage_roll",
                    "savarin", "scallop", "scampi", "schnitzel", "scotch_egg",
                    "scrambled_eggs", "scrapple", "seaweed_salad", "shirred_egg",
                    "shrimp_and_grits", "sloppy_joe", "souffle", "spaghetti_bolognese",
                    "spaghetti_carbonara", "sponge_cake", "spring_roll",
                    "steak_au_poivre", "steak_tartare", "strawberry_shortcake",
                    "streusel", "strudel", "stuffed_cabbage", "stuffed_peppers",
                    "stuffed_tomato", "succotash", "sukiyaki", "sushi", "syllabub",
                    "taco", "tagliatelle", "takoyaki", "tamale", "tamale_pie",
                    "tapenade", "tempura", "tenderloin", "terrine", "tetrazzini",
                    "tiramisu", "toad_in_the_hole", "torte", "tortellini", "tostada",
                    "tuna_tartare", "upside_down_cake", "veal_cordon_bleu",
                    "vermicelli", "victoria_sandwich", "vol_au_vent", "waffle",
                    "welsh_rarebit", "wonton", "ziti"};
            result.setText(classes[maxPos]);
            //enable edittext nomor pesanan
            nomor.setEnabled(true);
            nomor.setVisibility(View.VISIBLE);
            //display textview
            resulttgl.setVisibility(View.VISIBLE);
            tgll.setVisibility(View.VISIBLE);

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if(resultCode == RESULT_OK){
            if(requestCode == 3){
                Bitmap image = (Bitmap) data.getExtras().get("data");
                int dimension = Math.min(image.getWidth(), image.getHeight());
                image = ThumbnailUtils.extractThumbnail(image, dimension, dimension);
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }else{
                Uri dat = data.getData();
                Bitmap image = null;
                try {
                    image = MediaStore.Images.Media.getBitmap(this.getContentResolver(), dat);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                imageView.setImageBitmap(image);

                image = Bitmap.createScaledBitmap(image, imageSize, imageSize, false);
                classifyImage(image);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}