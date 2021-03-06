package com.hooloovoo.securenotes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;


import com.espian.showcaseview.OnShowcaseEventListener;
import com.espian.showcaseview.ShowcaseView;
import com.espian.showcaseview.ShowcaseViews;
import com.espian.showcaseview.targets.ActionItemTarget;
import com.espian.showcaseview.targets.ActionViewTarget;
import com.espian.showcaseview.targets.ViewTarget;
import com.hooloovoo.securenotes.object.DAO;
import com.hooloovoo.securenotes.object.Encryptor;
import com.hooloovoo.securenotes.object.Note;
import com.hooloovoo.securenotes.object.PBKDF2Encryptor;
import com.hooloovoo.securenotes.object.PasswordPreference;
import com.hooloovoo.securenotes.object.SingletonParametersBridge;
import com.hooloovoo.securenotes.widget.NoteAdapter;
import com.hooloovoo.securenotes.widget.SwipeDismissListViewTouchListener;
import com.hooloovoo.securenotes.widget.UndoBarController;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ListView;

import org.xmlpull.v1.XmlPullParserException;

public class NotesActivity extends ListActivity implements ListView.OnItemClickListener,
															UndoBarController.UndoListener{
	private final static int NOTE_CREATE_REQUEST = 1;
	private final static int NOTE_UPDATE_REQUEST = 2;
	ArrayList<Note> mData;
	NoteAdapter mAdapter;

	
	UndoBarController mUndoBarController;

	DAO dao;
	
	SharedPreferences mSharedPreferences;
	
	int positionRemovedNote;
	int positionUpdatedNote;
	
	boolean sameApp;


    Encryptor encryptor;
    ProgressDialog progressDialog;

    Context mContext;
    static final ShowcaseView.ConfigOptions co = new ShowcaseView.ConfigOptions();
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_notes);
		dao = DAO.getInstance(getApplicationContext());
        mContext = this;

//        setEncryptor();
//		dao.openDB();
//		setDataArrayList();
		//setWidgetListener();
        co.shotType = ShowcaseView.TYPE_ONE_SHOT;

       		Log.d("NOTEACTIVITY", "Start noteactivity");


	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent){



		sameApp = false;
		if(resultCode==RESULT_OK){
			Bundle bun = intent.getExtras();
	    	Note newNote = (Note) bun.getParcelable("note");
//	    	Bitmap image = (Bitmap) bun.getParcelable("bitmapNote");
//	    	newNote.setBitmapImage(image);

			if (requestCode == NOTE_CREATE_REQUEST){
				storeNote(newNote,false);
			}else if (requestCode == NOTE_UPDATE_REQUEST){
				updateNote(newNote);
			}
			try{
			    mAdapter.notifyDataSetChanged();
            }catch (NullPointerException ex){
                ex.printStackTrace();
            }
		}
	}

	@Override 
	public void onResume(){
		super.onResume();
		//boolean orientation = (getResources().getConfiguration().orientation==Configuration.ORIENTATION_LANDSCAPE)?
			//	true:false;
        //setActionBar();
        Log.d("FFFFFFFFFFF ON RESUME","PRIMA DI SETTARE ENCRYPTOR");
        setEncryptor();
        Log.d("NOTESACTIVITY", "close timer");
        PasswordPreference preference = new PasswordPreference(getApplicationContext());
        if(preference.isAppLocked()){
            setContentView(R.layout.locked_app_layout);
            setLockedLayout();
            setListAdapter(null);
        }else{
            setContentView(R.layout.activity_notes);
            setWidgetListener();
            restoreSituation();
        }

	}
	
	@Override
	public void onPause(){
		super.onPause();
		Log.d("NOTEACTIVITY", "sameApp: "+sameApp);
        SingletonParametersBridge.getInstance().addParameter("adapter",mAdapter);
        SingletonParametersBridge.getInstance().addParameter("cachenotes",mData);
        if(progressDialog != null && progressDialog.isShowing()) progressDialog.dismiss();
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean toLock = mSharedPreferences.getBoolean("lockapponpause",false);
		if(!sameApp && toLock){
			//lockAPP
            Log.d("NOTEACTIVITY", "lockApp");
			PasswordPreference preference = new PasswordPreference(getApplicationContext());
            preference.setLockedPassword(true);
		}
	}
	
	@Override
	public void onDestroy(){
		super.onDestroy();
        /*
		TimerUnlock timerUnlock = TimerUnlock.getInstance();
        timerUnlock.resetTimer();
        Log.d("NOTES ACTIVITY","Timer closed on Destroy");
        SingletonParametersBridge.getInstance().addParameter("cachenotes",null);*/

        Log.d("ON DESTROY","this activity is destroied by another one");

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.notes, menu);
//        View showcasedView = findViewById(R.id.action_add);
//        ViewTarget target = new ViewTarget(showcasedView);
//        ShowcaseView.insertShowcaseView(target, this, R.string.accedi, R.string.accedi);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        boolean hints_value = sharedPref.getBoolean("hints_value",true);
        if(hints_value){
            final Activity activity = this;
//        ActionItemTarget ac = new ActionItemTarget(this,R.id.action_add);
//        ShowcaseView showcaseView = ShowcaseView.insertShowcaseView(ac,this,R.string.tips_title_add_note, R.string.tips_add_note);



            ShowcaseViews showcaseViews = new ShowcaseViews(this);
            showcaseViews.addView(new ShowcaseViews.ItemViewProperties(R.id.action_add,R.string.tips_title_add_note, R.string.tips_add_note,ShowcaseView.ITEM_ACTION_ITEM));
            //showcaseViews.addView(new ShowcaseViews.ItemViewProperties(R.id.action_add,R.string.tips_unlock_time_tile, R.string.tips_unlock,ShowcaseView.ITEM_ACTION_ITEM));

            showcaseViews.show();



        }

		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
        switch (item.getItemId()) {
		case R.id.action_settings:
			sameApp = true;
			openSettings();
			break;
		case R.id.action_add:
			Log.d("NOTEACTIVITY","Menu add_note_action");
			sameApp = true;
			addNote();
			break;
            case R.id.action_export:
                if(dao.isExternalStorageReadable()){
                    //export notes
                    //new ExportNotesTask().execute();
                    ExportDialogFragment frgm = new ExportDialogFragment();
                    frgm.show(getFragmentManager(),"export notes");
                }else{
                    Toast.makeText(getApplicationContext(),R.string.error_export_db,Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.action_import:
                if(dao.isExternalStorageReadable()){
                    //export notes
                   ImportDialogFragment fragment = new ImportDialogFragment();
                   fragment.show(getFragmentManager(),"import notes");
                }else{
                    Toast.makeText(getApplicationContext(),R.string.error_export_db,Toast.LENGTH_LONG).show();
                }
                break;
		default:
            return super.onOptionsItemSelected(item);
		}
		
		return true;
	}
	
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		sameApp = true;
		Note toUpdate = mData.get(position);
//		mData.remove(position);
		positionUpdatedNote = position;
		Intent intent = new Intent();
		Bundle bundle = new Bundle();
		bundle.putParcelable("noteToUpdate", toUpdate);
		//bundle.putParcelable("BitmapToUpdate", toUpdate.getBitmapImage());
		intent.putExtras(bundle);
		intent.setClass(NotesActivity.this, AddNoteActivity.class);
		startActivityForResult(intent, NOTE_UPDATE_REQUEST);
		
	}
	
	@Override
	public void onUndo(Parcelable token) {
		Note toReactive = (Note) token;
		storeNote(toReactive, true);
		mAdapter.notifyDataSetChanged();

	}

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("foo", true); 
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if(savedInstanceState != null){
            PasswordPreference preference = new PasswordPreference(getApplicationContext());
            preference.setLockedPassword(false);
         }

    }




    public void exportNotes(String filename){
        ExportNotesTask task = new ExportNotesTask(filename);
        task.execute();
    }

    public void importNotes(String filename){
        ImportNotesTask task = new ImportNotesTask(filename);
        task.execute();
    }

    /**
     * this method restore old situation on resume activity
     */
    private void restoreSituation(){
        try{
            mData = (ArrayList<Note>) SingletonParametersBridge.getInstance().getParameter("cachenotes");
            if(mData == null) Log.d("FFFFFFFFFFF","DATA NULL");
            //mAdapter = new NoteAdapter(this,mData);
            mAdapter = (NoteAdapter) SingletonParametersBridge.getInstance().getParameter("adapter");
            mAdapter.data = mData;
            setListAdapter(mAdapter);
        }catch (NullPointerException ex){
            ex.printStackTrace();
            mAdapter = new NoteAdapter(this);
            setListAdapter(mAdapter);
            new NoteLoaderTask().execute();

        }

        try{
            if((Boolean) SingletonParametersBridge.getInstance().getParameter("settedpassword") || setEncryptor()){
                SingletonParametersBridge.getInstance().addParameter("settedpassword",false);
                Toast.makeText(getApplicationContext(),R.string.wait_for_update_note,Toast.LENGTH_LONG).show();
                RefreshNoteIntoDBTask rf = new RefreshNoteIntoDBTask(getApplicationContext());
                rf.execute();
                Log.d("NOTEACTIVITY","We need refresh note into db");
            }
        }catch(NullPointerException ex){
            ex.printStackTrace();
        }
    }

    /**
     * this method set layout if app locked
     */
    private void setLockedLayout(){
        Button button = (Button) findViewById(R.id.button_unlock_app);
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/EarlyGameBoy.ttf");
        ((TextView)findViewById(R.id.app_locked_title_dialog)).setTypeface(font);
        ((Button)findViewById(R.id.button_unlock_app)).setTypeface(font);
        ((TextView)findViewById(R.id.editText_password_locked_app)).setTypeface(font);
        final EditText editText = (EditText) findViewById(R.id.editText_password_locked_app);
        final PasswordPreference preference = new PasswordPreference(getApplicationContext());
        final String pass = preference.getPassword();
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(pass.equals(editText.getText().toString().trim())){
                    preference.setLockedPassword(false);
                    setContentView(R.layout.activity_notes);
                    setWidgetListener();
                    restoreSituation();
                }else{
                    Toast.makeText(getApplicationContext(),R.string.esito_no,Toast.LENGTH_LONG).show();
                }
            }
        });

    }
	
	private void addNote(){
		Intent intent = new Intent(NotesActivity.this,AddNoteActivity.class);
		startActivityForResult(intent, NOTE_CREATE_REQUEST);
		
	}

    /*private void setActionBar(){
        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        ActionBar actionBar = getActionBar();
        actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#330000ff")));
        actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#550000ff")));
    }*/
	
    private boolean setEncryptor(){
        try{
            encryptor = (Encryptor) SingletonParametersBridge.getInstance().getParameter("encrypt");
            Log.d("Need to throws exception", encryptor.getRawKey());
        }catch (NullPointerException ex){
            ex.printStackTrace();
            encryptor = new PBKDF2Encryptor();
            SingletonParametersBridge.getInstance().addParameter("encrypt",encryptor);
        }
        return false;
    }
	
	private void openSettings(){
		Intent intent = new Intent(NotesActivity.this, SettingsActivity.class);
		
		startActivity(intent);
	}
	
	private boolean loadNoteFromDB(){
        dao.openDB();
        Log.d("LOAD NOTE", "Stiamo caricando le note");
		ArrayList<Note> fromDB = (ArrayList<Note>) dao.getAllNotes();
        mData = new ArrayList<Note>();
        if(fromDB.size()!=0){
            for(Note note:dao.getAllNotes()){
                mData.add(decryptNote(note));
            }
        }
        dao.closeDB();
		return (mData.size() == 0)? false:true;
	}
	
	/**
	 * this method store note to DB and to list of note. 
	 * @param newNote note to store
	 * @param reInsert true if note is to re-insert, false if it's new one
	 */
	private void storeNote(Note newNote,boolean reInsert){
        if(reInsert){
            dao.openDB();
            long i = dao.addNoteToDB(encryptNote(newNote), reInsert);

            dao.closeDB();
            if(i == -1 ){
                Toast.makeText(getApplicationContext(), R.string.error_write_db, Toast.LENGTH_LONG).show();
            }else {
                mData.add(positionRemovedNote,newNote);
                mAdapter.notifyDataSetChanged();
            }
        }else {
            StoreNoteToDBTask task = new StoreNoteToDBTask(newNote);
            task.execute();
        }

	}
	
	private void updateNote(Note newNote){

        //mAdapter.update(newNote, positionUpdatedNote);

        UpdateNoteToDBTask task = new UpdateNoteToDBTask(newNote);
        task.execute();
		
	}

    /**
     * this method creates an encrypted Note to store into DB
     * @param toEncrypt
     * @return Encrypted note
     */
    private Note encryptNote(Note toEncrypt){
        if(toEncrypt == null) Log.d("FFFFFFFFFFFFFFFFF", "TODECRYPT NULL");
        if(encryptor == null){
            setEncryptor();
            Log.d("FFFFFFFFFFFFFFFFF", "ENCRYPTOR NULL");
        }
        if(Encryptor.password == null){
            Log.d("FFFFFFFFFFFFF","PASSWORD NULL");
            PasswordPreference preference = new PasswordPreference(getApplicationContext());
            Encryptor.password = preference.getPassword();
            Log.d("PASSWORD IN PREFERENCE", Encryptor.password);
        }
        String nameEn = encryptor.encrypt(toEncrypt.getmName(),Encryptor.password);
        String descEn = encryptor.encrypt(toEncrypt.getmDesc(),Encryptor.password);

        Note encrypted;
        if(toEncrypt.getImageLen()>1){
            //criptiamo immagine
            String imgString = Base64.encodeToString(toEncrypt.getmImage(),Base64.DEFAULT);
            String enimgString = encryptor.encrypt(imgString,Encryptor.password);
            byte[] imgEn = enimgString.getBytes();

        //Note encrypted = new Note(toEncrypt.getmId(),nameEn,descEn,toEncrypt.getmDate(),
         //      toEncrypt.getImageLen(),toEncrypt.getmImage());
        encrypted = new Note(toEncrypt.getmId(),nameEn,descEn,toEncrypt.getmDate(),
                imgEn.length,imgEn);
        }else{
        encrypted = new Note(toEncrypt.getmId(),nameEn,descEn,toEncrypt.getmDate(),
                  toEncrypt.getImageLen(),toEncrypt.getmImage());
        }
        return encrypted;
    }

    /**
     * this method creates a decrypted note loaded from db
     * @param toDecrypt
     * @return decrypted note
     */
    private Note decryptNote(Note toDecrypt){
        if(toDecrypt == null) Log.d("FFFFFFFFFFFFFFFFF", "TODECRYPT NULL");
        if(encryptor == null) Log.d("FFFFFFFFFFFFFFFFF", "ENCRYPTOR NULL");
        if(Encryptor.password == null){
            Log.d("FFFFFFFFFFFFF","PASSWORD NULL");
            PasswordPreference preference = new PasswordPreference(getApplicationContext());
            Encryptor.password = preference.getPassword();
            Log.d("PASSWORD IN PREFERENCE", Encryptor.password);
        }

        String nameEn = encryptor.decrypt(toDecrypt.getmName(), Encryptor.password);
        String descEn = encryptor.decrypt(toDecrypt.getmDesc(), Encryptor.password);

        Note decrypted;
        /*if (toDecrypt.getImageLen()>1){
            byte[] imgEn = toDecrypt.getmImage();
            String enimgString = new String(imgEn);
            String imgString = encryptor.decrypt(enimgString,Encryptor.password);
            byte[] img = Base64.decode(imgString,Base64.DEFAULT);
            decrypted = new Note(toDecrypt.getmId(),nameEn,descEn,toDecrypt.getmDate(),
                    img.length,img);*/
        //}else{
        decrypted = new Note(toDecrypt.getmId(),nameEn,descEn,toDecrypt.getmDate(),
                toDecrypt.getImageLen(),toDecrypt.getmImage());
        //}
        return decrypted;
    }
	
	private void removeNote(int position){
        dao.openDB();
		Note toRemove = (Note) mAdapter.getItem(position);
		mAdapter.remove(toRemove);
		dao.removeFromDB(toRemove);
		mUndoBarController.showUndoBar(false, getString(R.string.undobar_message), toRemove);
        dao.closeDB();
		this.positionRemovedNote = position;
	}
	
	private void setWidgetListener(){
		getListView().setOnItemClickListener(this);
		// Create a ListView-specific touch listener. ListViews are given special treatment because
        // by default they handle touches for their list items... i.e. they're in charge of drawing
        // the pressed state (the list selector), handling list item clicks, etc.
        SwipeDismissListViewTouchListener touchListener =
                new SwipeDismissListViewTouchListener(
                        getListView(),
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            @Override
                            public boolean canDismiss(int position) {
                                return true;
                            }

                            @Override
                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    removeNote(position);
                                }
                                mAdapter.notifyDataSetChanged();
                            }
                        });
        
        getListView().setOnTouchListener(touchListener);
        getListView().setOnScrollListener(touchListener.makeScrollListener());
        mUndoBarController = new UndoBarController(findViewById(R.id.undobar), this);
	}
	


    /**
     * this method set prograss dialog
     */
    private void setProgressDialog(String message){
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(message);
        progressDialog.setCancelable(false);
    }


    private class NoteLoaderTask extends AsyncTask<Void,Void,Boolean>{


        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            setProgressDialog(mContext.getResources().getString(R.string.loading_notes));
            progressDialog.show();
        }
        @Override
        protected Boolean doInBackground(Void... voids) {
            return loadNoteFromDB();
        }

        @Override
        protected void onPostExecute(Boolean esito){
            progressDialog.dismiss();
            if(esito){
                //aggiorno listview
                Collections.reverse(mData);
                mAdapter.data = mData;
                mAdapter.notifyDataSetChanged();
            }else{
                //mostro toast di info
            }


        }
    }

    /**
     * this AsyncTask is used to update notes into db after updating password or algorithm to encrypt
     */
    private class RefreshNoteIntoDBTask extends AsyncTask<Void,Void,Void>{

        Context mContext;

        public RefreshNoteIntoDBTask(Context context){
            mContext = context;
        }
        @Override
        protected Void doInBackground(Void... voids) {
            dao.openDB();
            for(Note note:mData){
                dao.updateNoteToDB(encryptNote(note));
            }
            dao.closeDB();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(mContext,R.string.updating_completed,Toast.LENGTH_LONG).show();
        }
    }

    /**
     * AsyncTask to export Notes to xml file. Notice that notes are encrypted
     */
    private class ExportNotesTask extends AsyncTask<Void,Void,Boolean>{

        String filename;
        String path = "";


        public ExportNotesTask(String filename){
            this.filename = filename;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setProgressDialog(mContext.getResources().getString(R.string.exporting_notes));
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            boolean esito;
            dao.openDB();
            try{
                path = dao.exportDBCSV(getApplicationContext(),filename);
                esito = true;
            }catch (IOException ex){
                esito = false;
            }finally {
                dao.closeDB();
            }
            return esito;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            if(aVoid){
                //Toast ok
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.generated_file)+path+"/"+filename+".csv",Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(getApplicationContext(),R.string.error_write_db,Toast.LENGTH_LONG).show();
            }
        }
    }


    private class ImportNotesTask extends AsyncTask<Void,Void,Boolean>{

        boolean esito;
        String filename;

        public ImportNotesTask(String filename){
            this.filename = filename;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            setProgressDialog(mContext.getResources().getString(R.string.import_notes));
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try{
                ArrayList<Note> data = dao.importNotesFromFileCSV(getApplicationContext(),filename);
                for(Note note:data){
                    int jint ;
                    dao.openDB();
                    Note newNote = decryptNote(note);
                    if((jint = (int) dao.addNoteToDB(note, false))!=-1){

                        newNote.setmId(jint);
                        mData.add(newNote);
                        esito = true;
                    }else{Log.d("ERRORE IMPORT", "-1 id");
                        esito = false;
                    }
                    dao.closeDB();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
                esito = false;
            }  catch (ParseException e) {
                e.printStackTrace();
                esito = false;
            } catch (IOException e) {
                e.printStackTrace();
                esito = false;
            } catch (RuntimeException e){
                e.printStackTrace();
                esito = false;
            }
            return esito;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            Collections.reverse(mData);
            if(aVoid){
                if(mAdapter.getCount()==0) mAdapter.data = mData;
                mAdapter.notifyDataSetChanged();

                Toast.makeText(getApplicationContext(),getString(R.string.import_ok_esito)+filename,Toast.LENGTH_LONG).show();
            }else{
                Toast.makeText(getApplicationContext(), getString(R.string.error_import_note), Toast.LENGTH_LONG).show();
            }
        }
    }


    private class UpdateNoteToDBTask  extends AsyncTask<Void,Void,Boolean>{
        Note note;

        public UpdateNoteToDBTask(Note n){
            note = n;
        }
        @Override
        protected Boolean doInBackground(Void... voids) {
            dao.openDB();
            Note en = encryptNote(note);
            int i = 0;
            while(i<3){
                Log.d("ToUpdate", "start Update");
                int d = dao.updateNoteToDB(en);
                Log.d("COMPRESSION IMAGE-NOTEACTIVITy", ""+note.getImageLen());
                Log.d("ToUpdate", "# Updates: " + d);
                if(d==0){
                 i++;

                }else{
                    return true;
                 }
            }

            dao.closeDB();
                return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(!aBoolean){
                Toast.makeText(getApplicationContext(), R.string.error_update_db, Toast.LENGTH_LONG).show();
            }else{
                try{
                    mAdapter.update(note, positionUpdatedNote);
                    mAdapter.notifyDataSetChanged();
                }catch (NullPointerException ex){}
            }
        }


    }


    private class StoreNoteToDBTask  extends AsyncTask<Void,Void,Boolean>{
        Note mNote;

        public StoreNoteToDBTask(Note n){
            mNote = n;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            int jint ;
            dao.openDB();
            int i = 0;
            while(i<3){
                if((jint = (int) dao.addNoteToDB(encryptNote(mNote), false))!=-1){
                    mNote.setmId(jint);
                    dao.closeDB();
                    return true;
                }else{
                    i++;
                }
            }
            dao.closeDB();
            return false;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            super.onPostExecute(aBoolean);
            if(!aBoolean){
                Toast.makeText(getApplicationContext(), R.string.error_write_db, Toast.LENGTH_LONG).show();
            }else{
                mData.add(0,mNote);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

	

}
