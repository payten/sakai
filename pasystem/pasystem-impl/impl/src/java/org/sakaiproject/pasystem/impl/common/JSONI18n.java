package org.sakaiproject.pasystem.impl.common;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import org.sakaiproject.user.cover.PreferencesService;
import org.sakaiproject.tool.cover.SessionManager;

import org.sakaiproject.pasystem.api.I18n;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;

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
            throw new RuntimeException("Missing default I18n file: " + resourceBase + "/default.json");
        }


        try {
            JSONParser parser = new JSONParser();
            translations = (JSONObject)parser.parse(new InputStreamReader(stream));
        } catch (IOException|ParseException e) {
            throw new RuntimeException(e);
        }
    }


    public String t(String key) {
        String result = (String)translations.get(key);

        if (result == null) {
            throw new RuntimeException("Missing translation for key: " + key);
        }

        return result;
    }

}
