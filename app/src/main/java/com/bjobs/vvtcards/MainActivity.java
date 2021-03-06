package com.bjobs.vvtcards;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;

import java.util.List;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bjobs.vvtcards.Adapters.RecyclerAdapter;
import com.bjobs.vvtcards.fragments.CreateDelCard;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.bjobs.vvtcards.service.AutomatedCardChecker;
import com.bjobs.vvtcards.service.DOMParser;


public class MainActivity extends AppCompatActivity implements CreateDelCard.OnCompleteDelListener {

    /* Progress bars handler */
    Handler mHandler = new Handler();
    //ArrayList with cards names and IDs
    ArrayList<CardInfo> cards = new ArrayList<>();
    List<CardInfo> result = new ArrayList<CardInfo>();

    private int cycleCounter = 0;

    private static final String TAG = "MainActivity";
    private static final int PREFERENCE_MODE_PRIVATE = 0;
    private static int WIFI_STAT = 0;
    private int LOADED = 0;
    private boolean thisSessionCardAdded = false;

    protected DBHelper dbHelper;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private SwipeRefreshLayout swipeRefresh;

    private TextView emptysqlinfo;
    private ProgressBar progressBar;

    private PendingIntent pendingIntent;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    private Boolean goneToPlayMarket = false;

    //To retrieve settings
    SharedPreferences settingsPref;
    int syncFrequency;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myToolbar.setTitleTextColor(Color.parseColor("#ffffff"));

        recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swiperefresh);
        emptysqlinfo = (TextView) findViewById(R.id.emptySQLInfo);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        sharedPref = getPreferences(PREFERENCE_MODE_PRIVATE);
        editor = sharedPref.edit();
        swipeRefresh.setOnRefreshListener(
                new SwipeRefreshLayout.OnRefreshListener() {
                    @Override
                    public void onRefresh() {
                        swipeUpdate();
                    }
                }
        );
        swipeRefresh.setColorSchemeResources(R.color.colorPrimary, R.color.colorAccent, R.color.colorPrimary);
        /* Retrieve a PendingIntent that will perform a broadcast */
        Intent alarmIntent = new Intent(MainActivity.this, AutomatedCardChecker.class);
        pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0, alarmIntent, 0);
        dbHelper = new DBHelper(this);
        setup();
    }

    private void setup() {
        settingsPref = PreferenceManager.getDefaultSharedPreferences(this);
        syncFrequency = Integer.parseInt(settingsPref.getString("sync_frequency", "12"));
        toTheBackground();
        checkNetworkConnection(0);
        showRateAppDialog();
    }

    public void saveCard(String cardName, String cardNumber) {
        if (cardName.length() > 0 && cardNumber.length() > 0) {
            dbHelper.manageDataStorage(0, cardName, cardNumber, null);
            cards.add(new CardInfo(cardName, cardNumber));
            checkNetworkConnection(4);
            thisSessionCardAdded = true;
            checkBalanceOnce(cards.get(cards.size() - 1));
        } else {
            showAddCardDialog();
            Toast.makeText(this, getResources().getString(R.string.emptyCardDataToast),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCompleteDel(String name, String adapterId) {
        try {
            dbHelper.manageDataStorage(1, null, null, name);
            int id = Integer.parseInt(adapterId);
            result.remove(id);
            cards.remove(id);
            if (result.size() < 1)
                emptysqlinfo.setText(getResources().getString(R.string.emptySQLInfo));
            adapter.notifyItemRemoved(id);
            adapter.notifyItemRangeChanged(id, result.size());
            cycleCounter -= 1;
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
        }

    }

    public void toTheBackground() {
        AlarmManager manager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        int interval = syncFrequency * 60 * 1000;
        manager.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), interval, pendingIntent);
    }

    public void checkNetworkConnection(int wtd) {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            if (wtd == 0) {
                WIFI_STAT = 1;
                getCardsID();
            } else if (wtd == 1) {
                WIFI_STAT = 1;
                checkBalance(1);
            } else if (wtd == 2) {
                WIFI_STAT = 1;
                //checkBalanceOnce(cardNumber);
            }
        } else {
            WIFI_STAT = 0;
            showNetworkError();
        }
    }

    public void showNetworkError() {
        Snackbar snackbar = Snackbar
                .make(findViewById(android.R.id.content), R.string.nointernet, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.recheckinternet, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (LOADED == 0) {
                            checkNetworkConnection(0);
                            Log.d(TAG, "loaded 0");
                        } else if (LOADED == 1) checkNetworkConnection(1);
                    }
                });

        snackbar.show();
    }

    private void showRateAppDialog() {
        int openCounter = sharedPref.getInt("counter", 0);
        if (WIFI_STAT == 1) {
            String version = sharedPref.getString("version", "1.0");
            boolean liked = sharedPref.getBoolean("liked", false);
            if (getVersion(this).equals(version)) {
                final boolean rated = sharedPref.getBoolean("rated", false);
                if (openCounter > 14 && rated == false) {
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.rateApp))
                            .setMessage(getString(R.string.pleaseRate))
                            .setPositiveButton(getString(R.string.rate), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse("market://details?id=com.bjobs.vvtcards"));
                                    startActivity(intent);
                                    goneToPlayMarket = true;
                                    editor.putBoolean("rated", true);
                                    editor.commit();
                                }
                            }).setNegativeButton(getString(R.string.later), new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Snackbar snackbar = Snackbar
                                    .make(findViewById(android.R.id.content), R.string.notRated, Snackbar.LENGTH_LONG);
                            snackbar.show();
                            editor.putInt("counter", 11);
                            editor.putBoolean("rated", false);
                            editor.commit();
                        }
                    }).show();
                } else if (openCounter > 25 && liked == false) {
                    new AlertDialog.Builder(this)
                            .setTitle(getString(R.string.likePage))
                            .setMessage(getString(R.string.pleaseLike))
                            .setPositiveButton(getString(R.string.like), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    editor.putBoolean("liked", true);
                                    editor.commit();
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse("https://m.facebook.com/VilniusCards/"));
                                    startActivity(intent);
                                }
                            })
                            .setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    editor.putBoolean("liked", true);
                                    editor.commit();
                                }
                            })
                            .show();
                }
            } else {
                editor.putInt("counter", 0);
                editor.putBoolean("rated", false);
                editor.putString("version", getVersion(this));
                editor.commit();
            }
        }
        openCounter++;
        editor.putInt("counter", openCounter);
        editor.commit();
        Log.d(TAG, Integer.toString(openCounter));
    }

    private void showAddCardDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // Get the layout inflater
        LayoutInflater inflater = this.getLayoutInflater();
        final View v = inflater.inflate(R.layout.create_card_layout, null);
        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(v)
                .setTitle(R.string.addCard)
                // Add action buttons
                .setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        EditText cardname = (EditText) v.findViewById(R.id.cardName);
                        EditText cardnumber = (EditText) v.findViewById(R.id.cardNumberId);
                        String cardName = cardname.getText().toString();
                        String cardNumber = cardnumber.getText().toString();
                        saveCard(cardName, cardNumber);
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        builder.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_layout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent settings = new Intent(this, Settings.class);
                startActivity(settings);
                return true;
            case R.id.action_addcard:
                showAddCardDialog();
                return true;
            case R.id.action_about:
                Intent about = new Intent(this, About.class);
                startActivity(about);
                return true;
            case R.id.action_faq:
                Intent faq = new Intent(this, FaqActivity.class);
                startActivity(faq);
                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        adapter = new RecyclerAdapter(createList(0), this);
        recyclerView.setAdapter(adapter);
        if (goneToPlayMarket == true) {
            goneToPlayMarket = false;
            Snackbar snackbar = Snackbar
                    .make(findViewById(android.R.id.content), R.string.rated, Snackbar.LENGTH_LONG);
            snackbar.show();
        }
    }

    //Can check many cards
    private void checkBalance(int param) {
        if (param == 0)
            progressBar.setVisibility(View.VISIBLE);
        else {
            progressBar.setVisibility(View.GONE);
            int size = result.size();
            if (size > 0) {
                result.clear();
                adapter.notifyItemRangeRemoved(0, size);
            }
        }
        final DOMParser gcd = new DOMParser(this);
        gcd.context = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                cards = gcd.checkCards(cycleCounter, cards);

                // Update the progress bar
                mHandler.post(new Runnable() {
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        adapter = new RecyclerAdapter(createList(cards.size()), MainActivity.this);
                        recyclerView.setAdapter(adapter);
                    }
                });
            }
        }).start();
    }

    private void addToAdapter(CardInfo card) {
        CardInfo addci = new CardInfo(card.cardName, card.cardNumber, card.cardTextInfo, card.buyTicketsLink, card.addMoneyLink);
        result.add(addci);
        adapter.notifyDataSetChanged();
    }

    //Checks only one card balance
    private void checkBalanceOnce(final CardInfo card) {
        emptysqlinfo.setVisibility(View.GONE);
        final DOMParser gcd = new DOMParser(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                gcd.context = getApplicationContext();
                final ArrayList<CardInfo> oneCardList = new ArrayList<>();
                oneCardList.add(card);
                oneCardList.set(0, gcd.checkCards(1, oneCardList).get(0));
                cards.set(cards.size() -1, oneCardList.get(0));

                // Update the progress bar
                mHandler.post(new Runnable() {
                    public void run() {
                        if (emptysqlinfo.getText().toString().length() > 0)
                            emptysqlinfo.setText(null);
                        addToAdapter(oneCardList.get(0));
                    }
                });
            }
        }).start();
    }


    private void getCardsID() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Cursor cursor = db.query("cards", null, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            int idColIndex = cursor.getColumnIndex("id");
            int nameColIndex = cursor.getColumnIndex("name");
            int numberColIndex = cursor.getColumnIndex("number");

            do {
                // получаем значения по номерам столбцов и пишем все в лог
                Log.d(TAG,
                        "ID = " + cursor.getInt(idColIndex) +
                                ", name = " + cursor.getString(nameColIndex) +
                                ", number = " + cursor.getString(numberColIndex));
                String cardInfoNa = cursor.getString(nameColIndex);
                String cardInfoNu = cursor.getString(numberColIndex);
                cards.add(new CardInfo(cardInfoNa, cardInfoNu));
                cycleCounter++;
            } while (cursor.moveToNext());
            checkNetworkConnection(4);
            if (WIFI_STAT == 1) {
                checkBalance(0);
                LOADED = 1;
            } else showNetworkError();

        } else {
            emptysqlinfo.setText(getResources().getString(R.string.emptySQLInfo));
            Log.d(TAG, "0 rows");
            cursor.close();

        }
    }

    public String getVersion(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    private List<CardInfo> createList(int size) {
        String cardName = "", cardNumber = "";
        for (int i = 0; i < size; i++) {
            try {
                CardInfo fullData = cards.get(i);
                CardInfo ci = new CardInfo(fullData.cardName, fullData.cardNumber, fullData.cardTextInfo, fullData.buyTicketsLink, fullData.addMoneyLink);
                cardName = fullData.cardName;
                cardNumber = fullData.cardNumber;
                result.add(ci);
            } catch (NoSuchElementException e) {
                CardInfo lci = new CardInfo(cardName, cardNumber);
                result.add(lci);
            }
        }
        return result;
    }

    private void swipeUpdate() {
        if (thisSessionCardAdded == false) {
            checkNetworkConnection(1);
            swipeRefresh.setRefreshing(false);
        } else {
            swipeRefresh.setRefreshing(false);
            Toast.makeText(this, getResources().getString(R.string.newestData), Toast.LENGTH_SHORT).show();
        }
    }
}

