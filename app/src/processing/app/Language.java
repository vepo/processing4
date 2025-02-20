/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2014 The Processing Foundation

  This program is free software; you can redistribute it and/or
  modify it under the terms of the GNU General Public License
  version 2, as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.io.*;
import java.util.*;

import processing.core.PApplet;


/**
 * Internationalization (I18N) and Localization (L10N)
 */
public class Language {
  // Store the language information in a file separate from the preferences,
  // because preferences need the language on load time.
  static protected final String PREF_FILE = "language.txt";
  static protected final File prefFile = Base.getSettingsFile(PREF_FILE);

  /** Single instance of this Language class */
  static private volatile Language instance;

  /** The system language */
  private String language;

  /** Available languages */
  private final Map<String, String> languages;

  private LanguageBundle bundle;


  private Language() {
    String systemLanguage = Locale.getDefault().getLanguage();
    language = loadLanguage();
    boolean writePrefs = false;

    if (language == null) {
      language = systemLanguage;
      writePrefs = true;
    }

    // Set available languages
    languages = new HashMap<>();
    for (String code : listSupported()) {
      Locale locale = Locale.forLanguageTag(code);
      languages.put(code, locale.getDisplayLanguage(locale));
    }

    // Set default language
    if (!languages.containsKey(language)) {
      language = "en";
      writePrefs = true;
    }

    if (writePrefs) {
      saveLanguage(language);
    }

    // Get bundle with translations (processing.app.language.PDE)
    //bundle = ResourceBundle.getBundle(Language.FILE, new Locale(this.language), new UTF8Control());
    try {
      bundle = new LanguageBundle(language);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


  static private String[] listSupported() {
    // List of languages in alphabetical order. (Add yours here.)
    // Also remember to add it to build/shared/lib/languages/languages.txt.
    return new String[] {
      "ar", // Arabic
      "ca", // Catalan
      "de", // German, Deutsch
      "en", // English
      "el", // Greek
      "es", // Spanish
      "fr", // French, Français
      "it", // Italiano, Italian
      "ja", // Japanese
      "ko", // Korean
      "nl", // Dutch, Nederlands
      "pt", // Portuguese
      "ru", // Russian
      "tr", // Turkish
      "uk", // Ukrainian
      "zh"  // Chinese
    };

    /*
    // come back to this when bundles are placed outside the JAR
    InputStream input = getClass().getResourceAsStream(LISTING);
    String[] lines = PApplet.loadStrings(input);
    ArrayList<String> list = new ArrayList<String>();
    for (String line : lines) {
      int index = line.indexOf('#');
      if (index != -1) {
        line = line.substring(0, index);
      }
      line = line.trim();
      list.add(line);
    }
    return list.toArray(new String[0]);
    */
  }


  /** Read the saved language */
  static private String loadLanguage() {
    try {
      if (prefFile.exists()) {
        String[] lines = PApplet.loadStrings(prefFile);
        if (lines != null && lines.length > 0) {
          String language = lines[0].trim().toLowerCase();
          if (language.length() != 0) {
            return language;
          }
        }
        System.err.println("Using default language because of a problem while reading " + prefFile);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }


  /**
   * Save the language directly to a settings file. This is 'save' and not
   * 'set' because a language change requires a restart of Processing.
   */
  static public void saveLanguage(String language) {
    try {
      Util.saveFile(language, prefFile);
      boolean ok = prefFile.setWritable(true, false);
      if (!ok) {
        System.err.println("Warning: could not set " + prefFile + " to writable");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    Platform.saveLanguage(language);
  }


  /** Singleton constructor */
  static public Language init() {
    if (instance == null) {
      synchronized (Language.class) {
        if (instance == null) {
          instance = new Language();
        }
      }
    }
    return instance;
  }


  static private String get(String key) {
    LanguageBundle bundle = init().bundle;

    try {
      String value = bundle.getString(key);
      if (value != null) {
        return value;
      }
    } catch (MissingResourceException ignored) { }

    return null;
  }


  /** Get translation from bundles. */
  static public String text(String key) {
    String value = get(key);
    if (value == null) {
      // MissingResourceException and null values
      return key;
    }
    return value;
  }


  static public String interpolate(String key, Object... arguments) {
    String value = get(key);
    if (value == null) {
      return key;
    }
    return String.format(value, arguments);
  }


  static public String pluralize(String key, int count) {
    // First check if the bundle contains an entry for this specific count
    String customKey = key + "." + count;
    String value = get(customKey);
    if (value != null) {
      return String.format(value, count);
    }
    // Use the general 'n' version for n items
    return interpolate(key + ".n", count);
  }


  /**
   * @param which either yes, no, cancel, ok, or browse
   */
  static public String getPrompt(String which) {
    return Language.text("prompt." + which);
  }


  /** Get all available languages */
  static public Map<String, String> getLanguages() {
    return init().languages;
  }


  /*
  static public String nameToCode(String languageName) {
    Map<String, String> languages = Language.getLanguages();
    for (Map.Entry<String, String> lang : languages.entrySet()) {
      if (lang.getValue().equals(languageName)) {
        return lang.getKey().trim().toLowerCase();
      }
    }
    return null;  // not found
  }
  */


  /**
   * Get the current language.
   * @return two-digit ISO code (lowercase)
   */
  static public String getLanguage() {
    return init().language;
  }


  /**
   * Is this a CJK language where Input Method support is suggested/required?
   * @return true if the user is running in Japanese, Korean, or Chinese
   */
  static public boolean useInputMethod() {
    final String language = getLanguage();
    return (language.equals("ja") ||
            language.equals("ko") ||
            language.equals("zh"));
  }


  @SuppressWarnings("unused")
  static public void addModeStrings(Mode mode) {
    String baseFilename = "languages/mode.properties";
    File modeBaseFile = new File(mode.getFolder(), baseFilename);
    if (modeBaseFile.exists()) {
      init().bundle.read(modeBaseFile, true);
    }

    String langFilename = "languages/mode_" + instance.language + ".properties";
    File modeLangFile = new File(mode.getFolder(), langFilename);
    if (modeLangFile.exists()) {
      init().bundle.read(modeLangFile, true);
    }
  }


//  /** Set new language (called by Preferences) */
//  static public void setLanguage(String language) {
//    this.language = language;
//
//    try {
//      File file = Base.getContentFile("lib/language.txt");
//      Base.saveFile(language, file);
//    } catch (Exception e) {
//      e.printStackTrace();
//    }
//  }


  /**
   * Custom 'Control' class for consistent encoding.
   * http://stackoverflow.com/questions/4659929/how-to-use-utf-8-in-resource-properties-with-resourcebundle
   */
  /*
  static class UTF8Control extends ResourceBundle.Control {
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IllegalAccessException, InstantiationException,IOException {
      // The below is a copy of the default implementation.
      String bundleName = toBundleName(baseName, locale);
      String resourceName = toResourceName(bundleName, "properties");
      ResourceBundle bundle = null;
      InputStream stream = null;
      if (reload) {
        URL url = loader.getResource(resourceName);
        if (url != null) {
          URLConnection connection = url.openConnection();
          if (connection != null) {
            connection.setUseCaches(false);
            stream = connection.getInputStream();
          }
        }
      } else {
        stream = loader.getResourceAsStream(resourceName);
      }
      if (stream != null) {
        try {
          // Only line changed from the original source:
          bundle = new PropertyResourceBundle(new InputStreamReader(stream, "UTF-8"));
        } finally {
          stream.close();
        }
      }
      return bundle;
    }
  }
  */


  static class LanguageBundle {
    Map<String, String> table;

    LanguageBundle(String language) throws IOException {
      table = new HashMap<>();

      // Check to see if the user is working on localization,
      // and has their own .properties files in their sketchbook.
      String baseFilename = "languages/PDE.properties";
      String langFilename = "languages/PDE_" + language + ".properties";

      File baseFile = Base.getLibFile(baseFilename);
      File userBaseFile = new File(Base.getSketchbookFolder(), baseFilename);
      if (userBaseFile.exists()) {
        baseFile = userBaseFile;
      }

      File langFile = Base.getLibFile(langFilename);
      File userLangFile = new File(Base.getSketchbookFolder(), langFilename);
      if (userLangFile.exists()) {
        langFile = userLangFile;
      }

      read(baseFile);
      read(langFile);
    }

    void read(File additions) {
      read(additions, false);
    }

    void read(File additions, boolean enforcePrefix) {
      String prefix = null;

      String[] lines = PApplet.loadStrings(additions);
      if (lines != null) {
        for (String line : lines) {
          if ((line.length() == 0) ||
            (line.charAt(0) == '#')) continue;

          // this won't properly handle = signs inside in the text
          int equals = line.indexOf('=');
          if (equals != -1) {
            String key = line.substring(0, equals).trim();

            boolean ignore = false;
            if (enforcePrefix) {
              if (prefix == null) {
                prefix = key.substring(0, key.indexOf('.') + 1);
                if (prefix.length() == 0) {
                  System.err.println("Language strings in Modes must include a prefix for all entries.");
                  System.err.println(additions + " will be ignored.");
                  return;  // exit read()
                }
              } else if (!key.startsWith(prefix)) {
                System.err.println("Ignoring " + key + " because all entries in " + additions + " must begin with " + prefix);
                ignore = true;
              }
            }

            if (!ignore) {
              String value = line.substring(equals + 1).trim();

              // Replace \n and \' with their actual values
              value = value.replaceAll("\\\\n", "\n");
              value = value.replaceAll("\\\\'", "'");

              table.put(key, value);
            }
          }
        }
      } else {
        System.err.println("Unable to read " + additions);
      }
    }

    String getString(String key) {
      return table.get(key);
    }


    /*
    // removing in 4.0 beta 5; not known to be in use [fry 220130]
    boolean containsKey(String key) {
      return table.containsKey(key);
    }
    */
  }
}
