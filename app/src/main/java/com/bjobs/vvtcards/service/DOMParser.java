package com.bjobs.vvtcards.service;

import android.content.Context;
import android.text.Html;

import com.bjobs.vvtcards.CardInfo;
import com.bjobs.vvtcards.R;
import com.bjobs.vvtcards.VVTCards;
import com.google.gson.Gson;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class DOMParser {

    public Context context;
    VVTCards app;
    private Connection.Response res;
    private Document doc;
    private Element inputHidden;
    private String text;
    private String URL;

    public DOMParser(Context context) {
        this.context = context;
        this.app = ((VVTCards) this.context.getApplicationContext());
        this.URL = "https://mano.vilniustransport.lt/lt/vilniecio-korteles-pildymas/card_balance";
    }

    //Can check many cards
    public ArrayList<CardInfo> checkCards(int cycleCounter, ArrayList<CardInfo> cardsNids) {
        getCookies(0);
        for (int i = 0; i < cycleCounter; i++) {
            try {
                CardInfo fullData = cardsNids.get(i);
                String cardNu = fullData.cardNumber;
                URL url = new URL(URL);

                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");

                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
                con.setRequestProperty("Cookie", app.welcomeCookies.toString().substring(1, app.welcomeCookies.toString().length() - 1));
                con.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:71.0) Gecko/20100101 Firefox/71.0");
                con.setRequestProperty("Accept", "*/*");
                con.setRequestProperty("Cache-Control", "no-cache");
                con.setRequestProperty("Host", "mano.vilniustransport.lt");
                con.setRequestProperty("Accept-Encoding", "gzip, deflate, br");
                con.setRequestProperty("Content-Length", "65");

                con.setDoOutput(true);

                String inputString = "token=" + app.securityTokenValue + "&card=" + cardNu;

                try(OutputStream os = con.getOutputStream()) {
                    byte[] input = inputString.getBytes("utf-8");
                    os.write(input, 0, input.length);
                }

                try(BufferedReader br = new BufferedReader(
                        new InputStreamReader(con.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine = null;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    CardBalanceResponse cardBalanceResponse = new Gson().fromJson(response.toString(), CardBalanceResponse.class);
                    doc = Jsoup.parse(cardBalanceResponse.getReplaceWith());
                    fullData.buyTicketsLink = doc.select("a").first().attr("href");
                    fullData.addMoneyLink = doc.select("a").last().attr("href");
                    doc.select("a").remove();
                    text = String.valueOf(Html.fromHtml(doc.body().toString()));
                }
                fullData.cardTextInfo = text;
                cardsNids.set(i, fullData);

            } catch (HttpStatusException e) {
                text = context.getResources().getString(R.string.cantCheckCard);
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return cardsNids;
    }

    private void getCookies(int param) {
        try {
            res = Jsoup.connect("https://mano.vilniustransport.lt")
                    .userAgent("Mozilla/5.0 (Linux; Android 7.1.1; LG-D855 Build/NOF26W) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.83 Mobile Safari/537.36")
                    .ignoreHttpErrors(true)
                    .timeout(30000)
                    .method(Connection.Method.GET)
                    .execute();
            doc = res.parse();
            app.welcomeCookies = res.cookies();
            if (param == 0)
                inputHidden = doc.select("input[name=token]").first();
            else if (param == 1)
                inputHidden = doc.select("input").first();
            //String securityTokenKey = inputHidden.attr("name");
            app.securityTokenValue = inputHidden.attr("value");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
