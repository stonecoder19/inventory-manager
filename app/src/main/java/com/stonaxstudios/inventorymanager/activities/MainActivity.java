package com.stonaxstudios.inventorymanager.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.stonaxstudios.inventorymanager.R;
import com.stonaxstudios.inventorymanager.helper.Constants;
import com.stonaxstudios.inventorymanager.models.Item;
import com.stonaxstudios.inventorymanager.rest.RestTransaction;
import com.stonaxstudios.inventorymanager.sync.SyncItems;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    TextView tv;
    EditText editText;
    TableLayout tableLayout;
    String[] items = {"Medicine", "Medical", "Drugs", "Cocaine", "Cola","Pampers"};
    String itemsUrl = Constants.HOST_URL+ Constants.GET_ITEMS; //url to get items
    String transUrl  = Constants.HOST_URL+ Constants.POST_TRANSACTION; //url to post transactions
    List<Item> itemObjs;
    ProgressDialog progressDialog;
    TextView tvNoItems; //shown when no items have been loaded
    private SwipeRefreshLayout swipeRefreshLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvNoItems  = (TextView)findViewById(R.id.tvNoItems);
        tableLayout = (TableLayout)findViewById(R.id.table); //layout for table of cells and rows


        itemObjs = Item.listAll(Item.class); //list of items from the database

        if(itemObjs.size()>0) //if no items are present show a text view that says no items present
            tvNoItems.setVisibility(View.GONE);


        setUpViews(); //intitalize all the views/cells in the table layout




        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showFilenameDialog(); //show user prompt to name a file
            }
        });

        new LoadItemsTask(this).execute(""); //refresh the items from the backend


    }

    private void showFilenameDialog()
    {
        LayoutInflater li = LayoutInflater.from(this);
        View promptsView = li.inflate(R.layout.dialog_add_filename, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(
                this);

        // set prompts.xml to alertdialog builder
        alertDialogBuilder.setView(promptsView);

        final EditText filenameInput = (EditText) promptsView
                .findViewById(R.id.editTextFileName);

        // set dialog message
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("OK",null)
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // create alert dialog
        final AlertDialog alertDialog = alertDialogBuilder.create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {

            @Override
            public void onShow(DialogInterface dialog) {

                Button b = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                b.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        // TODO Do something
                        String filename = filenameInput.getText().toString(); //gets filename entered by user


                        if(!(filename.equals(""))){
                            postTransaction(filename); //process transaction and store it to backend database

                            alertDialog.dismiss();
                        }else{
                            Toast.makeText(view.getContext(),"Discussion must have a topic",Toast.LENGTH_SHORT).show();
                        }

                        //Dismiss once everything is OK.

                    }
                });
            }
        });

        // show it
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.show();
    }

    private void setUpViews() {
        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow row = (TableRow) tableLayout.getChildAt(i);

            setUpTableRow(row); //set up data for the row
        }
    }

    private void setUpTableRow(TableRow row)
    {
        //user enters name of item here
        AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView) row.findViewById(R.id.autocomplete_item);

        //user enters quantity here
        final EditText editText = (EditText) row.findViewById(R.id.editTextQty);

        //user sees price here
        final TextView tvRow = (TextView) row.findViewById(R.id.tvText);


        /*
            When user chooses item from the auto complete text view a new row is added
         */
        assert autoCompleteTextView != null;
        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                editText.setText("1"); //sets default quantity to 1
                tvRow.setText("$200"); //sets default price to 200
                AddRow(); //adds a new row

            }
        });

        /*
            when user clicks on send button on keyboard it updates the price and the quantity
         */
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                boolean handled = false;
                if (actionId == EditorInfo.IME_ACTION_SEND) { //when user submits a quantity
                    updatePrice(tvRow, Integer.parseInt(editText.getText().toString())); //update the price based on the quantitiy
                    handled = true;
                }
                return handled;
            }
        });

        ArrayList<String>strList = new ArrayList<>();

        for(int i=0;i<itemObjs.size();i++)
        {
            strList.add(itemObjs.get(i).getName());
        }

        String[]strArray = new String[strList.size()];

        strArray = strList.toArray(strArray);  //creates an array from the list of items

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, strArray);

        //ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, items);

        autoCompleteTextView.setAdapter(adapter); //inflates the adapter with the item names

    }

    private void AddRow() {
        TableRow row = (TableRow) LayoutInflater.from(this).inflate(R.layout.table_row, null);
        setUpTableRow(row); //sets up table row
        tableLayout.addView(row);

    }


    private void updatePrice(TextView tv, int qty) {
        tv.setText("$"+qty * 200 + "");
    }


    public void createandDisplayPdf(String filename) {

        Document doc = new Document();

        try {
            String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Inventory";

            File dir = new File(path);
            if (!dir.exists())
                dir.mkdirs();

            File file = new File(dir, filename+".pdf");
            FileOutputStream fOut = new FileOutputStream(file);

            PdfWriter.getInstance(doc, fOut); //creates a new pdf file

            //open the document
            doc.open();

           /* Paragraph p1 = new Paragraph(text);
            //  Font paraFont= new Font(Font.COURIER);
            p1.setAlignment(Paragraph.ALIGN_CENTER);
            // p1.setFont(paraFont);

            Anchor anchor = new Anchor("First Chapter");
            anchor.setName("First Chapter");

            // Second parameter is the number of the chapter
            Chapter catPart = new Chapter(new Paragraph(anchor), 1);

            Paragraph subPara = new Paragraph("Subcategory 1");
            Section subCatPart = catPart.addSection(subPara);
            subCatPart.add(new Paragraph("Hello"));



            //add paragraph to document
          //  doc.add(p1);*/

            createTable(doc); //creates a table and inserts it into the pdf

           // doc.add(subCatPart);

        } catch (DocumentException de) {
            // Log.e("PDFCreator", "DocumentException:" + de);
            Toast.makeText(this, "Not working", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(this, "Not file working", Toast.LENGTH_SHORT).show();
            //Log.e("PDFCreator", "ioException:" + e);
        } catch (Exception e) {
            Toast.makeText(this, "Not file working", Toast.LENGTH_SHORT).show();
            //Log.e("PDFCreator", "ioException:" + e);
        } finally {
            doc.close();
        }

        viewPdf(filename, "Inventory"); //open the pdf


    }

    // Method for opening a pdf file
    private void viewPdf(String file, String directory) {

        File pdfFile = new File(Environment.getExternalStorageDirectory() + "/" + directory + "/" + file);
        Uri path = Uri.fromFile(pdfFile);

        // Setting the intent for pdf reader
        Intent pdfIntent = new Intent(Intent.ACTION_VIEW);
        pdfIntent.setDataAndType(path, "application/pdf");
        pdfIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);

        try {
            startActivity(pdfIntent); //opens the pdf in a pdf viewer
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Can't read pdf file", Toast.LENGTH_SHORT).show();
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        if(id == R.id.action_refresh)
        {
            Refresh();
        }

        if(id==R.id.action_logout)
        {
            Logout();
        }

        return super.onOptionsItemSelected(item);
    }

    private void Refresh()
    {
        new LoadItemsTask(this).execute(""); //refresh items database
    }

    private void Logout()
    {
        SharedPreferences sharedPrefs = getApplicationContext().getSharedPreferences(Constants.PREFS_STRING, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = sharedPrefs.edit();
        prefsEditor.clear();
        prefsEditor.commit();
        Intent intent = new Intent(this,LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        this.startActivity(intent);
    }

    private  void createTable(Document subCatPart)
            throws Exception {
        PdfPTable table = new PdfPTable(3);

        // t.setBorderColor(BaseColor.GRAY);
        // t.setPadding(4);
        // t.setSpacing(4);
        // t.setBorderWidth(1);

        PdfPCell c1 = new PdfPCell(new Phrase("Item")); //name of item
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("Quantity"));  //quantity of item
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);

        c1 = new PdfPCell(new Phrase("Price")); //price if item
        c1.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c1);
        table.setHeaderRows(1);

        ArrayList<String> cellItems = getCellData();
        for(int i=0;i<cellItems.size();i++)
        {
           String item = cellItems.get(i);
            if(item!="")
                table.addCell(item); //adds data to the cells in the table of pdf
        }

        subCatPart.add(table);

    }


    //Method resposible for retrieving all the data from the cells in the UI
    private  ArrayList<String> getCellData()
    {
        ArrayList<String> cellItems = new ArrayList<>();

        TableLayout tableLayout = (TableLayout)findViewById(R.id.table);

        for(int i=0; i<tableLayout.getChildCount();i++)
        {
            TableRow row = (TableRow)tableLayout.getChildAt(i);

            AutoCompleteTextView itemEntry = (AutoCompleteTextView)row.findViewById(R.id.autocomplete_item);
            String itemname = itemEntry.getText().toString(); //name of item

            EditText qtyEntry = (EditText)row.findViewById(R.id.editTextQty);
            String qty = qtyEntry.getText().toString(); //quantity of item

            TextView costEntry = (TextView)row.findViewById(R.id.tvText);
            String cost = costEntry.getText().toString();
            if(!isStringEmptyOrNull(itemname)) //if there is no data in this row
            {
                cellItems.add(itemname);
                cellItems.add(qty);
                cellItems.add(cost);
            }


        }


        return cellItems;
    }

    private class LoadItemsTask extends AsyncTask<String,Integer,Boolean>
    {
        boolean sync = false;
        Context ctxt;

        public LoadItemsTask(Context ctxt)
        {
            this.ctxt = ctxt;

        }
        @Override
        protected void onPreExecute() {
            Toast.makeText(ctxt,"Syncing..",Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            try{
                sync = new SyncItems(itemsUrl).syncItems(); //syncs items to database
            }catch(Exception e){
                sync = false;
            }
            return sync;

        }

        @Override
        protected void onPostExecute(Boolean sync) {
            if(sync)
            {
                itemObjs = Item.listAll(Item.class); //gets items from database
                inflateAutoCompletetextView(); //updates all the autocomplete fields with item names
                Toast.makeText(ctxt,"Successful",Toast.LENGTH_SHORT).show();
            }else{
                itemObjs = Item.listAll(Item.class);
                if(itemObjs.size()<=0)
                    tvNoItems.setVisibility(View.VISIBLE);

                Toast.makeText(ctxt,"Failed",Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void inflateAutoCompletetextView()
    {
        ArrayList<String>strList = new ArrayList<>();

        for(int i=0;i<itemObjs.size();i++)
        {
            strList.add(itemObjs.get(i).getName());
        }

        String[]strArray = new String[strList.size()];

        strArray = strList.toArray(strArray); //stores list into an array

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, strArray);

        for (int i = 0; i < tableLayout.getChildCount(); i++) {
            TableRow row = (TableRow) tableLayout.getChildAt(i);

            AutoCompleteTextView autoCompleteTextView = (AutoCompleteTextView)row.findViewById(R.id.autocomplete_item);

            autoCompleteTextView.setAdapter(adapter); //sets up the adapter for each row

        }

    }

    /**
     * creates name value pairs for the transaction items and calls method to post it to the database
     * @param filename
     */
    private void postTransaction(String filename)
    {
        List<HashMap<String,Object>> nameValuePairList = new ArrayList<>(); //ist of post paramater eg.name=monkey
        int itemNo=0;
        TableLayout tableLayout = (TableLayout)findViewById(R.id.table);

        for(int i=0; i<tableLayout.getChildCount();i++) //iterates through the table rows
        {
            TableRow row = (TableRow)tableLayout.getChildAt(i); //gets a specific table row

            AutoCompleteTextView itemEntry = (AutoCompleteTextView)row.findViewById(R.id.autocomplete_item);
            String itemname = itemEntry.getText().toString(); //name of item

            EditText qtyEntry = (EditText)row.findViewById(R.id.editTextQty);
            String strQty = qtyEntry.getText().toString();
            int quantity=0;
            if(!isStringEmptyOrNull(strQty))
                 quantity = Integer.parseInt(strQty); //quantity

            TextView costEntry = (TextView)row.findViewById(R.id.tvText); //cost

            HashMap<String,Object> nameValuePair = new HashMap<>();

            if(!isStringEmptyOrNull((itemname)))
            {
                //gets the itemNo for an item name
                itemNo = Item.find(Item.class,"name = ?",itemname).get(0).getItemNo();
                nameValuePair.put("TransDate",now());
                nameValuePair.put("ItemNo",itemNo);
                nameValuePair.put("Quantity",quantity);

                nameValuePairList.add(nameValuePair); //adds name value pair to list
            }


        }

        if(nameValuePairList.size()>0)
        {

            new PostTransactionTask(this,nameValuePairList,filename).execute(""); //sends json data to database
        }


    }


    /**
     * gets the current date
     * @return current date
     */
    public static String now() {
         String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
        return sdf.format(cal.getTime());
    }

    /*
        checks if a string is empty or null
     */
    public boolean isStringEmptyOrNull(String string)
    {
        return string.isEmpty() || string == null;
    }

    /**
     * background task for posting a transaction to a database
     */

    private class PostTransactionTask extends AsyncTask<String,Integer,Boolean> {

        Context ctxt;
        List<HashMap<String,Object>> nameValuePairList;
        String filename;
        String output;

        public PostTransactionTask(Context ctxt, List<HashMap<String,Object>> nameValuePairList, String filename){
            this.ctxt = ctxt;
            this.nameValuePairList = nameValuePairList; //post parameters
            this.filename = filename; //name of pdf file
        }

        @Override
        protected void onPreExecute() {
            progressDialog = ProgressDialog.show(ctxt,"Adding Transactions..","Please wait.."); //shows a progress dialog
            progressDialog.setIndeterminate(true);
            progressDialog.setCanceledOnTouchOutside(true);
        }

        @Override
        protected Boolean doInBackground(String... params) {

            int result = 0;
            output = "";
            for(int i=0;i<nameValuePairList.size();i++)
            {
                RestTransaction restTransaction = new RestTransaction(transUrl);
                result = restTransaction.postTransaction((String)nameValuePairList.get(i).get("TransDate"),
                        (int)nameValuePairList.get(i).get("ItemNo"),(int)nameValuePairList.get(i).get("Quantity")); //post transaction to database


            }

            //0=faliure 1= success
            return result==1;



        }

        @Override
        protected void onPostExecute(Boolean result) {
            progressDialog.dismiss(); //remove progress dialog

            if(result){
                Toast.makeText(ctxt, "Transaction Successful..", Toast.LENGTH_SHORT).show();
                createandDisplayPdf(filename); //create and show the pdf document


            }else{
                Toast.makeText(ctxt,"Transaction Failed..",Toast.LENGTH_SHORT).show();
                //Toast.makeText(ctxt,output,Toast.LENGTH_SHORT).show();
            }

        }





    }



}
