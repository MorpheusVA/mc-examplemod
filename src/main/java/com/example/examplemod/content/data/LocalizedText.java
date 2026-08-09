package com.example.examplemod.content.data;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

@JsonAdapter(LocalizedText.Adapter.class)
public class LocalizedText {
    private final Map<String, String> translations = new HashMap<>();

    public LocalizedText() {}

    public LocalizedText(String defaultText) {
        if (defaultText != null) {
            translations.put("en_us", defaultText);
        }
    }

    public void put(String lang, String text) {
        if (lang != null && text != null) {
            translations.put(lang.toLowerCase(), text);
        }
    }

    public boolean isBlank() {
        String val = get("en_us");
        return val == null || val.isBlank();
    }

    public String get() {
        return get("en_us");
    }

    public String get(String lang) {
        if (lang == null) lang = "en_us";
        lang = lang.toLowerCase();
        if (translations.containsKey(lang)) {
            return translations.get(lang);
        }
        if (translations.containsKey("en_us")) {
            return translations.get("en_us");
        }
        return translations.values().stream().findFirst().orElse("");
    }

    public Map<String, String> getTranslations() {
        return translations;
    }

    public static class Adapter extends TypeAdapter<LocalizedText> {
        @Override
        public void write(JsonWriter out, LocalizedText value) throws IOException {
            if (value == null || value.getTranslations().isEmpty()) {
                out.nullValue();
                return;
            }
            if (value.getTranslations().size() == 1 && value.getTranslations().containsKey("en_us")) {
                out.value(value.get("en_us"));
            } else {
                out.beginObject();
                for (Map.Entry<String, String> entry : value.getTranslations().entrySet()) {
                    out.name(entry.getKey());
                    out.value(entry.getValue());
                }
                out.endObject();
            }
        }

        @Override
        public LocalizedText read(JsonReader in) throws IOException {
            LocalizedText localized = new LocalizedText();
            JsonToken token = in.peek();
            if (token == JsonToken.STRING) {
                localized.put("en_us", in.nextString());
            } else if (token == JsonToken.BEGIN_OBJECT) {
                in.beginObject();
                while (in.hasNext()) {
                    String key = in.nextName();
                    String val = in.nextString();
                    localized.put(key, val);
                }
                in.endObject();
            } else {
                in.skipValue();
            }
            return localized;
        }
    }
}
