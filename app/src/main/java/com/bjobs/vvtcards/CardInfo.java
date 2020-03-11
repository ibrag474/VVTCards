package com.bjobs.vvtcards;

/**
 * Created by Ibragim on 2017-07-12.
 */

public class CardInfo {

    public String cardName;
    public String cardNumber;
    public String cardTextInfo;
    public String buyTicketsLink;
    public String addMoneyLink;

    public CardInfo(String cardName, String cardNumber) {
        this.cardName = cardName;
        this.cardNumber = cardNumber;
    }

    public CardInfo(String cardName, String cardNumber, String cardTextInfo, String addTickets, String addMoney) {
        this.cardName = cardName;
        this.cardNumber = cardNumber;
        this.cardTextInfo = cardTextInfo;
        this.buyTicketsLink = addTickets;
        this.addMoneyLink = addMoney;
    }
}
