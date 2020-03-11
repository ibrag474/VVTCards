package com.bjobs.vvtcards.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.preference.PreferenceManager;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import android.util.Log;

import com.bjobs.vvtcards.DBHelper;
import com.bjobs.vvtcards.MainActivity;
import com.bjobs.vvtcards.NotificationReceiver;
import com.bjobs.vvtcards.R;


import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ibragim on 2017-08-18.
 */

public class AutomatedCardChecker extends BroadcastReceiver {

    private static int mNotificationId = 20010419;
    private int WIFI_STAT = 0;

    protected DBHelper dbHelper;
    ArrayList<String> cardsNids = new ArrayList<>();
    private int cycleCounter = 0;

    private Connection.Response res;
    private Connection.Response res1;
    private Document doc;
    private Document doc1;
    private Element answer;
    private String title;
    private String cardNa;
    private String cardNu;
    private String cardCa;
    private String cardTicketDate;
    private String retrivedData;
    private float money;

    Map welcomeCookies;
    String securityTokenValue;

    private Calendar calendar;
    Date expirationDate;

    Handler mHandler = new Handler();
    NotificationCompat.Builder mBuilder;

    //To retrieve settings
    SharedPreferences settingsPref;
    int syncFrequency;

    boolean dataType;
    boolean syncTickets;
    boolean syncMoney;
    int ticketsNotifierRange;
    float minMoney;

    Context context;

    SharedPreferences sharedPref;
    SharedPreferences.Editor editor;
    int remindMoney;


    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        sharedPref = context.getSharedPreferences("notif", Context.MODE_PRIVATE);
        editor = sharedPref.edit();
        dbHelper = new DBHelper(context);
        Log.d("service", "starting");
        setup();
    }

    private void setup() {
        remindMoney = sharedPref.getInt("remindMoney", 0);
        settingsPref = PreferenceManager.getDefaultSharedPreferences(context);
        dataType = settingsPref.getBoolean("data_type", true);
        syncTickets = settingsPref.getBoolean("ticket_sync", true);
        syncMoney = settingsPref.getBoolean("money_sync", true);
        ticketsNotifierRange = Integer.parseInt(settingsPref.getString("ticket_notifier_range", "3"));
        minMoney = Float.parseFloat(settingsPref.getString("money_notifier_range", "0"));
        syncFrequency = Integer.parseInt(settingsPref.getString("sync_frequency", "12"));

        if (syncMoney == true || syncTickets == true) {
            checkTiming();
        }
    }

    private void checkTiming() {
        calendar = Calendar.getInstance();
        long lastChecked = sharedPref.getLong("lastTime", 0);
        long time = calendar.getTimeInMillis();
        long syncFreqMillis = syncFrequency * 60 * 60 * 1000;
        if ((time - lastChecked) >= syncFreqMillis) {
            checkNetworkConnection();
            editor.putLong("lastTime", time);
            editor.commit();
        }
    }

    private void checkNetworkConnection() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null) {
            //Works when sync only over Wi-Fi enabled
            if (dataType == true) {
                if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    WIFI_STAT = 1;
                    getCardsID();
                    Log.d("service", "checking network");
                }
            } else {
                WIFI_STAT = 1;
                getCardsID();
                Log.d("service", "checking mobile network");
            }
        } else {
            WIFI_STAT = 0;
        }
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
                String cardInfoNa = cursor.getString(nameColIndex);
                String cardInfoNu = cursor.getString(numberColIndex);
                cardsNids.add(cardInfoNa + "\n" + cardInfoNu);
                cycleCounter++;
            } while (cursor.moveToNext());
            if (WIFI_STAT == 1) {
                checkBalance();
                Log.d("service", "loaded cards");
            }
        } else {
            cursor.close();

        }
        db.close();
    }

    //Can check many card
    private void checkBalance() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    res = Jsoup.connect("https://mano.vilniustransport.lt/login")
                            .userAgent("Mozilla/5.0 (Linux; Android 7.1.1; LG-D855 Build/NOF26W) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36")
                            .ignoreHttpErrors(true)
                            .method(Connection.Method.GET)
                            .execute();
                    doc = res.parse();
                    welcomeCookies = res.cookies();
                    Element inputHidden = doc.select("input").last();
                    //String securityTokenKey = inputHidden.attr("name");
                    securityTokenValue = inputHidden.attr("value");
                } catch (IOException e) {

                }
                for (int i = 0; i < cycleCounter; i++) {
                    try {
                        String fullData = cardsNids.get(i);
                        String[] split = fullData.split("\n");
                        String cardNu = split[1];
                        res1 = Jsoup.connect("https://mano.vilniustransport.lt/card/card-check?number=" + cardNu + "&_csrf_token=" + securityTokenValue)
                                .header("Content-Type", "text/html; charset=UTF-8")
                                .userAgent("Mozilla/5.0 (Linux; Android 7.1.1; LG-D855 Build/NOF26W) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36")
                                .cookies(welcomeCookies)
                                .execute();

                        doc1 = res1.parse();
                        answer = doc1.body();
                        title = answer.text();
                        String presentData = cardsNids.get(i);
                        cardsNids.set(i, presentData + "\n" + title);

                    } catch (HttpStatusException e) {

                    } catch (IllegalArgumentException e) {

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // Update the progress bar
                mHandler.post(new Runnable() {
                    public void run() {
                        Log.d("service", "find balance start");
                        findBalance();
                    }
                });
            }
        }).start();
    }

    private void findBalance() {
        Log.d("service", "starting findBalance");
        for (int i = 0; i < cardsNids.size(); i++) {
           try {
                String fullData = cardsNids.get(i);
                StringTokenizer tokens = new StringTokenizer(fullData, "\n");
                cardNa = tokens.nextToken();
                cardNu = tokens.nextToken();
                retrivedData = tokens.nextToken();
                //String info = "E. bilietas 30 d. -80% m/s 1 galioja iki 2017.11.05 23:59 E. pinigai 1,99 € Informuojame, kad kai kurie duomenys apie pažymėtus bilietus yra atnaujinami vieną kartą per parą, todėl tikslūs likučiai gali būti rodomi praėjus iki 24 val. po Jūsų paskutinės kelionės";
                //String info = "Jūsų kortelė tuščia. Prisijunkite arba prisiregistruokite papildyti.";
                if (retrivedData.length() > 215) {
                    Pattern cashDatas = Pattern.compile(".*bilietas\\s(.*).*iki\\s(.*).*E. pinigai\\s(.*).*Informuojame\\S(.*)");
                    Matcher mMatcher = cashDatas.matcher(retrivedData);
                    while (mMatcher.find()) {
                        if (mMatcher.group(1).length() > 0) {
                            cardTicketDate = mMatcher.group(2);
                            cardCa = mMatcher.group(3);
                            String[] delEuro = cardCa.split("\\,|\\s");
                            try {
                                money = Float.parseFloat(delEuro[0] + "." + delEuro[1]);
                            } catch (NumberFormatException e) {

                            }
                            Log.d("service", "got card tickets");
                            calculateDate(cardTicketDate, cardNa, money);
                        }
                    }
                }
           } catch (NoSuchElementException e) {

           }
        }
    }

    private void calculateDate(String thatDay, String cardName, float cash) {
        String[] split = thatDay.split("\\.|\\s");
        Calendar expirationDate = Calendar.getInstance();
        expirationDate.set(Calendar.YEAR, Integer.parseInt(split[0]));
        expirationDate.set(Calendar.MONTH, Integer.parseInt(split[1]));
        expirationDate.set(Calendar.DAY_OF_MONTH, Integer.parseInt(split[2]));
        // Get the number of days in that month
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        long diff = ((expirationDate.getTimeInMillis() - calendar.getTimeInMillis()) / (24 * 60 * 60 * 1000)) - daysInMonth;
        if (syncTickets == true && diff <= ticketsNotifierRange) {
            if (diff == 1) {
                showNotification(context.getResources().getString(R.string.inCard) + " \"" + cardName + "\" " +
                                context.getResources().getString(R.string.notifyExpiringTicket),
                        context.getResources().getString(R.string.notifyToActivate) + " " + Long.toString(diff) + " " +
                                context.getResources().getString(R.string.dayLeft));
            } else {
                showNotification(context.getResources().getString(R.string.inCard) + " \"" + cardName + "\" " +
                                context.getResources().getString(R.string.notifyExpiringTicket),
                        context.getResources().getString(R.string.notifyToActivate) + " " + Long.toString(diff) + " " +
                                context.getResources().getString(R.string.daysLeft));
            }
        }
        if (syncMoney == true && cash <= minMoney) {
            showNotification(context.getResources().getString(R.string.card) + " \"" + cardName + "\" " + context.getResources().getString(R.string.running_out_of_money),
                        context.getResources().getString(R.string.eurosleft1) + " " + cash + context.getResources().getString(R.string.eurosleft2));
        }
    }

    private void showNotification(String title, String text) {
        Intent intentGotIt = new Intent(context, NotificationReceiver.class);
        intentGotIt.putExtra("remindMoney", 12);


        PendingIntent pIntent = PendingIntent.getActivity(context, mNotificationId, intentGotIt, PendingIntent.FLAG_ONE_SHOT);
    if (remindMoney > 0) {
        remindMoney--;
        editor.putInt("remindMoney", remindMoney);
        editor.commit();
        mBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setColor(51283);


    } else if (remindMoney == 0) {
         mBuilder = new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.notification_icon)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setLights(65280, 800, 700)
                        .setColor(51283)
                        .addAction(R.drawable.notification_icon, context.getResources().getString(R.string.muteBtn), pIntent)
                        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
    }


// Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(context, MainActivity.class);


// The stack builder object will contain an artificial back stack for the
// started Activity.
// This ensures that navigating backward from the Activity leads out of
// your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
// Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
// Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

// mNotificationId is a unique integer your app uses to identify the
// notification. For example, to cancel the notification, you can pass its ID
// number to NotificationManager.cancel().

        mNotificationManager.notify(mNotificationId, mBuilder.build());
        mNotificationId++;
    }
}
