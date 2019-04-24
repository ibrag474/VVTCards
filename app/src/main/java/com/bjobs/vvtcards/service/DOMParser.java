package com.bjobs.vvtcards.service;

import android.content.Context;
import android.util.Log;

import com.bjobs.vvtcards.R;
import com.bjobs.vvtcards.VVTCards;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class DOMParser {

    public Context context;
    VVTCards app;
    private Connection.Response res;
    private Document doc;
    private Element inputHidden;
    private Element body;
    private String text;

    public DOMParser(Context context) {
        this.context = context;
        this.app = ((VVTCards) this.context.getApplicationContext());
    }

    //Can check many cards
    public ArrayList<String> checkCards(int cycleCounter, ArrayList<String> cardsNids) {
        getCookies(0);
        for (int i = 0; i < cycleCounter; i++) {
            try {
                String fullData = cardsNids.get(i);
                String[] split = fullData.split("\n");
                String cardNu = split[1];
                res = Jsoup.connect("https://mano.vilniustransport.lt/card/card-check?number=" + cardNu + "&_csrf_token=" + app.securityTokenValue)
                        .header("Content-Type", "text/html; charset=UTF-8")
                        .userAgent("Mozilla/5.0 (Linux; Android 7.1.1; LG-D855 Build/NOF26W) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36")
                        .timeout(30000)
                        .cookies(app.welcomeCookies)
                        .execute();

                doc = res.parse();
                body = doc.body();
                text = body.text();
                cardsNids.set(i, fullData + "\n" + text);

            } catch (HttpStatusException e) {
                text = context.getResources().getString(R.string.cantCheckCard);
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return cardsNids;
    }

    public String checkCard(String number) {
        try {
            getCookies(0);
            res = Jsoup.connect("https://mano.vilniustransport.lt/card/card-check?number=" + number + "&_csrf_token=" + app.securityTokenValue)
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .userAgent("Mozilla/5.0 (Linux; Android 7.1.1; LG-D855 Build/NOF26W) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36")
                    .cookies(app.welcomeCookies)
                    .execute();

            doc = res.parse();
            body = doc.body();
            text = body.text();
        } catch (HttpStatusException e) {
            text = context.getResources().getString(R.string.cantCheckCard);
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    private void getCookies(int param) {
        try {
            res = Jsoup.connect("https://mano.vilniustransport.lt/login")
                    .userAgent("Mozilla/5.0 (Linux; Android 7.1.1; LG-D855 Build/NOF26W) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36")
                    .ignoreHttpErrors(true)
                    .timeout(30000)
                    .method(Connection.Method.GET)
                    .execute();
            doc = res.parse();
            app.welcomeCookies = res.cookies();
            if (param == 0)
                inputHidden = doc.select("input").last();
            else if (param == 1)
                inputHidden = doc.select("input").first();
            //String securityTokenKey = inputHidden.attr("name");
            app.securityTokenValue = inputHidden.attr("value");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
