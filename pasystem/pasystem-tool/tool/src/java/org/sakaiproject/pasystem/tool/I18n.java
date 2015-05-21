package org.sakaiproject.pasystem.tool;

import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Properties;
import org.sakaiproject.user.cover.PreferencesService;
import org.sakaiproject.tool.cover.SessionManager;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.json.simple.JSONObject;

class I18n {

    private JSONObject translations;

    public I18n(Locale locale) {
        String language = "default";

        if (locale != null) {
            language = locale.getLanguage();
        }

        InputStream stream = getClass().getClassLoader().getResourceAsStream("org/sakaiproject/pasystem/tool/i18n/" + language + ".json");

        if (stream == null) {
            stream = getClass().getClassLoader().getResourceAsStream("org/sakaiproject/pasystem/tool/i18n/default.json");
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
