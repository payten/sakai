package org.sakaiproject.pasystem.impl.common;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.sakaiproject.pasystem.api.I18n;
import org.sakaiproject.pasystem.api.I18nException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class JSONI18n implements I18n {

    private JSONObject translations;

    public JSONI18n(ClassLoader loader, String resourceBase, Locale locale) {
        String language = "default";

        if (locale != null) {
            language = locale.getLanguage();
        }

        InputStream stream = loader.getResourceAsStream(resourceBase + "/" + language + ".json");

        if (stream == null) {
            stream = loader.getResourceAsStream(resourceBase + "/default.json");
        }

        if (stream == null) {
            throw new I18nException("Missing default I18n file: " + resourceBase + "/default.json");
        }


        try {
            JSONParser parser = new JSONParser();
            translations = (JSONObject) parser.parse(new InputStreamReader(stream));
        } catch (IOException | ParseException e) {
            throw new I18nException("Failure when reading I18n stream", e);
        }
    }


    public String t(String key) {
        String result = (String) translations.get(key);

        if (result == null) {
            throw new I18nException("Missing translation for key: " + key);
        }

        return result;
    }

}
