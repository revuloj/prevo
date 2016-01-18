/*
 * PReVo - A portable version of ReVo for Android
 * Copyright (C) 2012, 2013, 2016  Neil Roberts
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package uk.co.busydoingnothing.prevo;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.inputmethod.InputMethodManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SearchActivity extends ListActivity
  implements TextWatcher
{
  public static final String EXTRA_LANGUAGE =
    "uk.co.busydoingnothing.prevo.Language";
  public static final String EXTRA_SEARCH_TERM =
    "uk.co.busydoingnothing.prevo.SearchTerm";

  public static final String TAG = "prevosearch";

  private SearchAdapter searchAdapter;
  private String searchLanguage;

  private static boolean actionInitialised;
  private static boolean actionSupported;
  private static Method setShowAsActionMethod;

  @Override
  public void onCreate (Bundle savedInstanceState)
  {
    super.onCreate (savedInstanceState);
    setContentView (R.layout.search);

    ensureActionInitialised ();

    Intent intent = getIntent ();

    ListView lv = getListView ();

    if (intent != null)
      {
        searchLanguage = intent.getStringExtra (EXTRA_LANGUAGE);

        if (searchLanguage != null)
          {
            searchAdapter = new SearchAdapter (this, searchLanguage);

            lv.setAdapter (searchAdapter);

            TextView tv = (TextView) findViewById (R.id.search_edit);
            tv.addTextChangedListener (this);

            setTitle (getTitle () + " [" + searchLanguage + "]");
          }

        String searchTerm = intent.getStringExtra (EXTRA_SEARCH_TERM);

        if (searchTerm != null)
          {
            TextView tv = (TextView) findViewById (R.id.search_edit);
            tv.setText (searchTerm);
          }
      }

    lv.setOnItemClickListener (new AdapterView.OnItemClickListener ()
      {
        public void onItemClick (AdapterView<?> parent,
                                 View view,
                                 int position,
                                 long id)
        {
          SearchAdapter adapter =
            (SearchAdapter) parent.getAdapter ();
          SearchResult result = adapter.getItem (position);
          Intent intent = new Intent (view.getContext (),
                                      ArticleActivity.class);
          intent.putExtra (ArticleActivity.EXTRA_ARTICLE_NUMBER,
                           result.getArticle ());
          intent.putExtra (ArticleActivity.EXTRA_MARK_NUMBER,
                           result.getMark ());
          startActivity (intent);
        }
      });
  }

  @Override
  public void onStart ()
  {
    super.onStart ();

    View tv = findViewById (R.id.search_edit);

    tv.requestFocus ();

    InputMethodManager imm =
      (InputMethodManager) getSystemService (INPUT_METHOD_SERVICE);

    if (imm != null)
      imm.showSoftInput (tv,
                         0, /* flags */
                         null /* resultReceiver */);
  }

  private void addLanguageMenuItem (Menu menu,
                                    String language)
  {
    Resources resources = getResources ();
    String label;

    if (language.equals ("eo"))
      {
        label = resources.getString (R.string.menu_search_language_esperanto);
      }
    else
      {
        LanguageList languageList = LanguageList.getDefault (this);
        String languageName = languageList.getLanguageName (language);
        label = resources.getString (R.string.menu_search_language,
                                     languageName);
      }

    MenuItem item = menu.add (label);
    item.setIntent (MenuHelper.createSearchIntent (this, language));

    if (actionSupported)
      {
        try
          {
            setShowAsActionMethod.invoke (item,
                                          MenuItem.SHOW_AS_ACTION_IF_ROOM |
                                          MenuItem.SHOW_AS_ACTION_WITH_TEXT);
            item.setTitleCondensed (language);
          }
        catch (IllegalAccessException e)
          {
          }
        catch (InvocationTargetException e)
          {
          }
      }
  }

  @Override
  public boolean onCreateOptionsMenu (Menu menu)
  {
    LanguageDatabaseHelper dbHelper = new LanguageDatabaseHelper (this);
    String[] languages = dbHelper.getLanguages ();
    int nLanguages;

    if (searchLanguage.equals ("eo"))
      {
        nLanguages = 2;
      }
    else
      {
        addLanguageMenuItem (menu, "eo");
        nLanguages = 1;
      }

    for (String language : languages)
      {
        if (searchLanguage == null || !language.equals (searchLanguage))
          {
            addLanguageMenuItem (menu, language);
            if (--nLanguages <= 0)
              break;
          }
      }

    MenuInflater inflater = getMenuInflater ();
    inflater.inflate (R.menu.search_menu, menu);

    return true;
  }

  @Override
  public boolean onOptionsItemSelected (MenuItem item)
  {
    Intent intent = item.getIntent ();

    if (intent != null &&
        intent.getComponent () != null &&
        intent.getComponent ().equals (getComponentName ()))
      {
            TextView tv = (TextView) findViewById (R.id.search_edit);
            intent.putExtra (EXTRA_SEARCH_TERM, tv.getText ().toString ());
      }

    if (MenuHelper.onOptionsItemSelected (this, item))
      return true;

    return super.onOptionsItemSelected (item);
  }

  @Override
  protected Dialog onCreateDialog (int id)
  {
    return MenuHelper.onCreateDialog (this, id);
  }

  @Override
  public void afterTextChanged (Editable s)
  {
    searchAdapter.getFilter ().filter (s);
  }

  @Override
  public void beforeTextChanged (CharSequence s,
                                 int start,
                                 int count,
                                 int after)
  {
  }

  @Override
  public void onTextChanged (CharSequence s,
                             int start,
                             int before,
                             int count)
  {
  }

  private static void ensureActionInitialised ()
  {
    if (actionInitialised)
      return;

    try
      {
        setShowAsActionMethod =
          MenuItem.class.getMethod ("setShowAsAction", int.class);

        actionSupported = true;
      }
    catch (NoSuchMethodException e)
      {
        Log.i (TAG, "Action not supported: " + e.getMessage ());
      }

    actionInitialised = true;
  }
}
