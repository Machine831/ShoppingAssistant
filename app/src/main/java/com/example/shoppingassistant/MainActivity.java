package com.example.shoppingassistant;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    public static final String APP_PREFERENCES = "settings";
    SharedPreferences settings;
    ArrayList<Product> products = new ArrayList<>();
    HashSet<String> urls = new HashSet<>();
    private Handler mHandler;
    private int mInterval = 60000;
    MyRequest myRequest = new MyRequest();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidThreeTen.init(this);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.list);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        ProductAdapter productAdapter = new ProductAdapter(products);
        recyclerView.setAdapter(productAdapter);

        settings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        if (settings.contains("Products")) {
            settings.getStringSet("Products", urls);
            Log.println(Log.ASSERT, "List", urls.toString());

            if (!urls.isEmpty()) {
                for (String url : urls) {
                    Log.println(Log.ASSERT, "aga", url);
                    myRequest.execute(url);
                    try {
                        products.add(myRequest.get(10, TimeUnit.SECONDS));
                        recyclerView.getAdapter().notifyDataSetChanged();

                    } catch (InterruptedException | ExecutionException | TimeoutException e) {
                        e.printStackTrace();
                    }
                }
            }


        }


        mHandler = new Handler();


        Button button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ProductActivity.class);
                stopRepeatingTask();
                startActivityForResult(intent, 1);
                Log.println(Log.ASSERT, "execute", "дошло");

            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        String result = "";
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                result = data.getStringExtra("result");
                Log.println(Log.ASSERT, "result", "result");
            }
            urls.add(result);
            SharedPreferences.Editor editor = settings.edit();
            editor.putStringSet("Products", urls);
            editor.apply();
            Log.println(Log.ASSERT, "settings", urls.toString());
            myRequest.execute(result);
            try {
                products.add(myRequest.get(5, TimeUnit.SECONDS));
                recyclerView.getAdapter().notifyDataSetChanged();
                startRepeatingTask();

            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRepeatingTask();
    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            MyRequest myRequest = new MyRequest();
            try {

                for (Product product: products){
                    myRequest.execute(product.url);
                    products.set(0,myRequest.get(10, TimeUnit.SECONDS));
                }
                recyclerView.getAdapter().notifyDataSetChanged();


            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                e.printStackTrace();
            } finally {
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

}
