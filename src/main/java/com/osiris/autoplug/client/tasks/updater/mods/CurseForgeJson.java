/*
 * Copyright (c) 2022 Osiris-Team.
 * All rights reserved.
 *
 * This software is copyrighted work, licensed under the terms
 * of the MIT-License. Consult the "LICENSE" file for details.
 */

package com.osiris.autoplug.client.tasks.updater.mods;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.osiris.autoplug.client.utils.GD;
import com.osiris.autoplug.core.json.exceptions.HttpErrorException;
import com.osiris.autoplug.core.json.exceptions.WrongJsonTypeException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CurseForgeJson {
    public final String key = "$2a$10$7exv4HBW5Glx9ELBT3i3seB5jXto3m1baXQXwtA/kEMGv3TYfBUWK";


    /**
     * Returns the json-element. This can be a json-array or a json-object.
     *
     * @param input_url The url which leads to the json file.
     * @return JsonElement
     * @throws Exception When status code other than 200.
     */
    public JsonElement getJsonElement(String input_url) throws IOException, HttpErrorException {

        HttpURLConnection con = null;
        JsonElement element;
        try {
            con = (HttpURLConnection) new URL(input_url).openConnection();
            con.addRequestProperty("User-Agent", "AutoPlug-Client - Contact: " + GD.OFFICIAL_WEBSITE);
            con.addRequestProperty("x-api-key", key);
            con.setConnectTimeout(1000);
            con.connect();

            if (con.getResponseCode() == 200) {
                try (InputStreamReader inr = new InputStreamReader(con.getInputStream())) {
                    element = JsonParser.parseReader(inr);
                }
            } else {
                throw new HttpErrorException(con.getResponseCode(), con.getResponseMessage(), "Couldn't get the json file from: " + input_url);
            }
        } catch (IOException | HttpErrorException e) {
            if (con != null) con.disconnect();
            throw e;
        } finally {
            if (con != null) con.disconnect();
        }
        return element;
    }

    public JsonArray getJsonArray(String url) throws IOException, HttpErrorException, WrongJsonTypeException {
        JsonElement element = getJsonElement(url);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        } else {
            throw new WrongJsonTypeException("Its not a json array! Check it out -> " + url);
        }
    }

    /**
     * Turns a JsonArray with its objects into a list.
     *
     * @param url The url where to find the json file.
     * @return A list with JsonObjects or null if there was a error with the url.
     */
    public List<JsonObject> getJsonArrayAsList(String url) throws IOException, HttpErrorException, WrongJsonTypeException {
        List<JsonObject> objectList = new ArrayList<>();
        JsonElement element = getJsonElement(url);
        if (element != null && element.isJsonArray()) {
            final JsonArray ja = element.getAsJsonArray();
            for (int i = 0; i < ja.size(); i++) {
                JsonObject jo = ja.get(i).getAsJsonObject();
                objectList.add(jo);
            }
            return objectList;
        } else {
            throw new WrongJsonTypeException("Its not a json array! Check it out -> " + url);
        }
    }

    /**
     * Gets a single JsonObject.
     *
     * @param url The url where to find the json file.
     * @return A JsonObject or null if there was a error with the url.
     */
    public JsonObject getJsonObject(String url) throws IOException, HttpErrorException, WrongJsonTypeException {
        JsonElement element = getJsonElement(url);
        if (element != null && element.isJsonObject()) {
            return element.getAsJsonObject();
        } else {
            throw new WrongJsonTypeException("Its not a json object! Check it out -> " + url);
        }
    }

}

