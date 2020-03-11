package com.bjobs.vvtcards.service;

import com.google.gson.annotations.SerializedName;

public class CardBalanceResponse {
    private String status;
    @SerializedName("replace_with")
    private String replaceWith;
    private String target;
    /*@SerializedName("transformed_data")
    private String transformedData;*/

    public String getStatus() {
        return status;
    }

    public String getReplaceWith() {
        return replaceWith;
    }

}
