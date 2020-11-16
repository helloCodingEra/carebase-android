package org.getcarebase.carebase.activities.Main.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.CaptureActivity;

import org.getcarebase.carebase.R;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AddEquipmentFragment extends Fragment {
    private static final String TAG = AddEquipmentFragment.class.getSimpleName();
    private Activity parent;

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final CollectionReference usersRef = db.collection("users");
    private LinearLayout linearLayout;
    private LinearLayout procedureSummaryLinear;
    private LinearLayout buttonsLayout;
    private List<HashMap<String, Object>> procedureEquipment;
    private HashMap<String, String> procedureInfo;

    private String mNetworkId;
    private String mHospitalId;
    private String udiStr;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_addequipment, container, false);
        parent = getActivity();
        MaterialButton cancelButton = rootView.findViewById(R.id.equipment_cancel_button);
        MaterialButton saveButton = rootView.findViewById(R.id.equipment_save_button);
        FloatingActionButton addBarcode = rootView.findViewById(R.id.fragment_addScan);
        linearLayout = rootView.findViewById(R.id.equipment_linearlayout);
        buttonsLayout = rootView.findViewById(R.id.item_bottom);
        procedureSummaryLinear = rootView.findViewById(R.id.procedure_summary_linear);
        procedureEquipment = new ArrayList<>();
        MaterialToolbar topToolBar = rootView.findViewById(R.id.topAppBar);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        String userId = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                ProcedureInfoFragment fragment = new ProcedureInfoFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable("procedure_info", procedureInfo);
                fragment.setArguments(bundle);

                FragmentManager fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
                //clears other fragments
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(R.anim.enter_left, R.anim.fui_slide_out_left);
                fragmentTransaction.add(R.id.activity_main, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, callback);


        final DocumentReference currentUserRef = usersRef.document(userId);
        currentUserRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                String toastMessage;
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (Objects.requireNonNull(document).exists()) {
                        try {
                            mNetworkId = Objects.requireNonNull(document.get("network_id")).toString();
                            mHospitalId = Objects.requireNonNull(document.get("hospital_id")).toString();
                        } catch (NullPointerException e) {
                            toastMessage = "Error retrieving user information; Please contact support";
                            Toast.makeText(parent.getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // document for user doesn't exist
                        toastMessage = "User not found; Please contact support";
                        Toast.makeText(parent.getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    toastMessage = "User lookup failed; Please try again and contact support if issue persists";
                    Toast.makeText(parent.getApplicationContext(), toastMessage, Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        topToolBar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ProcedureInfoFragment fragment = new ProcedureInfoFragment();
                Bundle bundle = new Bundle();
                bundle.putSerializable("procedure_info", procedureInfo);
                fragment.setArguments(bundle);

                FragmentManager fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
                //clears other fragments
                fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(R.anim.enter_left, R.anim.fui_slide_out_left);
                fragmentTransaction.add(R.id.activity_main, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();

            }
        });

        if (getArguments() != null) {

            Bundle procedureInfoBundle = this.getArguments();

            // general procedure details
            if(procedureInfoBundle.getSerializable("procedure_info") != null) {
                procedureInfo = (HashMap<String, String>) procedureInfoBundle.getSerializable("procedure_info");
                checkProcedureInfo();
                setProcedureSummary(rootView);
            }

            if (procedureInfoBundle.getString("barcode") != null) {
                checkUdi(procedureInfoBundle.getString("barcode"));
            }

            if(procedureInfoBundle.getSerializable("procedure_udi") != null){
                List<HashMap<String, Object>> udiList =
                        (List<HashMap<String, Object>>) procedureInfoBundle.getSerializable("procedure_udi");
                for(int i = 0; i < udiList.size(); i++){
                    addReturnedUdi(udiList.get(i).get("udi").toString(),rootView,udiList.get(i).get("di").toString(),
                            udiList.get(i).get("amount_used").toString());
                }
            }
        }

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (parent != null)
                    parent.onBackPressed();
            }
        });


        final int[] i = {0};
        addBarcode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startScanner();
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {saveProcedureInfo();
            }
        });

        return rootView;
    }

    private void setProcedureSummary(View view){
        checkProcedureInfo();
        final View procedureSummaryView = View.inflate(view.getContext(), R.layout.procedure_summary, null);
        TextView newProcedureName = procedureSummaryView.findViewById(R.id.newprocedureinfoName_textview);
        TextView newProcedureDate = procedureSummaryView.findViewById(R.id.newprocedureinfoDate_textview);
        TextView newProcedureAccessionNumber = procedureSummaryView.findViewById(R.id.newprocedureinfoAccessionNumber_textview);
        newProcedureName.setText(Objects.requireNonNull(procedureInfo.get("procedure_used")));
        newProcedureDate.setText(Objects.requireNonNull(procedureInfo.get("procedure_date")));
        newProcedureAccessionNumber.setText(Objects.requireNonNull(procedureInfo.get("accession_number")));
        procedureSummaryLinear.addView(procedureSummaryView);
    }

    private void startScanner() {
        IntentIntegrator.forSupportFragment(AddEquipmentFragment.this)
                .setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
                .setBarcodeImageEnabled(true)
                .setCaptureActivity(CaptureActivity.class)
                .initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            String contents = result.getContents();
            if (contents != null) {
                checkUdi(contents);
            }
            if (result.getBarcodeImagePath() != null) {
                Log.d(TAG, "" + result.getBarcodeImagePath());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void checkUdi(final String contents){
        if (contents.equals("")) {
            return;
        }

        final View view = getView();
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(parent);
        String url = "https://accessgudid.nlm.nih.gov/api/v2/devices/lookup.json?udi=";
        url = url + contents;

        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        JSONObject responseJson;
                        try {
                            responseJson = new JSONObject(response);
                            Log.d(TAG, "RESPONSE: " + response);
                            JSONObject udi = responseJson.getJSONObject("udi");

                            final String di = udi.getString("di");
                            udiStr = contents;

                            DocumentReference docRef = db.collection("networks").document(mNetworkId)
                                    .collection("hospitals").document(mHospitalId).collection("departments")
                                    .document("default_department").collection("dis").document(di)
                                    .collection("udis").document(contents);
                            docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {
                                        DocumentSnapshot document = task.getResult();
                                        if (Objects.requireNonNull(document).exists()) {

                                            addUdi(contents, getView(), di);
                                        }else{
                                            offerEquipmentScan(Objects.requireNonNull(view), contents);
                                        }
                                    }else{
                                        Log.d(TAG, "Failed with: ", task.getException());
                                    }
                                }
                            });

                        } catch (JSONException e) {
                            FirebaseCrashlytics.getInstance().recordException(e);
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                FirebaseCrashlytics.getInstance().recordException(error);
                Log.d(TAG, "Error in parsing barcode");
                parseBarcodeError(Objects.requireNonNull(view));
            }
        });
        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void offerEquipmentScan(View view, final String udi){
        new MaterialAlertDialogBuilder(view.getContext())
                .setTitle("Scan equipment")
                .setMessage("The equipment could not be found in inventory.\nWould you like to scan it now?")

        .setPositiveButton("Scan", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                ItemDetailFragment fragment = new ItemDetailFragment();
                Bundle bundle = new Bundle();
                bundle.putString("barcode", udi);
                bundle.putSerializable("procedure_info", procedureInfo);
                bundle.putSerializable("udi_quantity", (Serializable) procedureEquipment);
                fragment.setArguments(bundle);
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

                //clears other fragments
               // fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(R.anim.fui_slide_in_right, R.anim.fui_slide_out_left);
                fragmentTransaction.add(R.id.activity_main, fragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();

            }
        })
        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        })
        .show();
    }

    public void parseBarcodeError(View view){
        new MaterialAlertDialogBuilder(view.getContext())
                .setTitle("Error reading barcode")
                .setMessage("The barcode could not be read/parsed.\n" +
                        "Would you like to add a new equipment to the procedure?")

                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startScanner();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (parent != null)
                            parent.onBackPressed();
                    }
                })
                .show();

    }

    private void addUdi(final String barcode, View view, final String di){
        final View textView = View.inflate(view.getContext(),R.layout.procedure_item,null);
        textView.setId(View.generateViewId());

        TextView udi = textView.findViewById(R.id.scanned_udi);
        final TextView quantity = textView.findViewById(R.id.udi_quantity);
        final ImageView plusIcon = textView.findViewById(R.id.increment_icon);
        ImageView deleteIcon = textView.findViewById(R.id.delete_icon);
        deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteUdiView(view,textView.getId(),barcode);
            }
        });
        plusIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumberPicker(view, quantity,barcode,plusIcon, di);
            }
        });

        addNumberPicker(view,quantity, barcode, plusIcon, di);
        udi.setText(barcode);
        buttonsLayout.setVisibility(View.VISIBLE);
        linearLayout.addView(textView,linearLayout.indexOfChild(buttonsLayout));


    }

    private void addReturnedUdi(final String barcode, View view, final String di, String quantityStr){
        final View textView = View.inflate(view.getContext(),R.layout.procedure_item,null);
        textView.setId(View.generateViewId());

        TextView udi = textView.findViewById(R.id.scanned_udi);
        final TextView quantity = textView.findViewById(R.id.udi_quantity);
        final ImageView plusIcon = textView.findViewById(R.id.increment_icon);
        ImageView deleteIcon = textView.findViewById(R.id.delete_icon);
        deleteIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteUdiView(view,textView.getId(),barcode);
            }
        });
        plusIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                addNumberPicker(view, quantity,barcode,plusIcon, di);
            }
        });

        udi.setText(barcode);
        String finalString;
        if(Integer.parseInt(quantityStr) > 1){
            finalString = quantityStr + " units have been used";
        }else{
            finalString = quantityStr + " unit has been used";
        }
        quantity.setText(finalString);
        buttonsLayout.setVisibility(View.VISIBLE);
        linearLayout.addView(textView,linearLayout.indexOfChild(buttonsLayout));
    }

    private void deleteUdiView(View view, int elementId, String barcode){
        linearLayout.removeView(linearLayout.findViewById(elementId));
        for(int i = 0; i < procedureEquipment.size(); i++){
            if(procedureEquipment.get(i).get("udi").toString().equals(barcode)){
                procedureEquipment.remove(i);
            }
        }
    }

    private void addNumberPicker(View view, final TextView quantity, final String barcode, final ImageView plusIcon, final String di){
        final Dialog d = new Dialog(view.getContext());
        //d.setTitle("Enter quantity");
        d.setContentView(R.layout.dialog);
        Button b1 =  d.findViewById(R.id.button1);
        Button b2 =  d.findViewById(R.id.button2);
        final NumberPicker np =  d.findViewById(R.id.numberPicker1);
        np.setMaxValue(1000);
        np.setMinValue(0);
        np.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
            }
        });
        b1.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                int quantityInt = np.getValue();
                if(quantityInt > 1){
                    quantity.setText(String.format(Locale.US,"%d units have been used", quantityInt));
                }else{
                    quantity.setText(String.format(Locale.US,"%d unit has been used", quantityInt));
                }

                quantity.setVisibility(View.VISIBLE);
                d.dismiss();
                HashMap<String, Object> eachUdi = new HashMap<>();
                eachUdi.put("udi",barcode);
                eachUdi.put("amount_used",String.valueOf(np.getValue()));
                eachUdi.put("di",di);
                procedureEquipment.add(eachUdi);
            }
        });
        b2.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v) {
                d.dismiss();
            }
        });
        d.show();
    }

    private void saveProcedureInfo(){
        checkProcedureInfo();
        for(int i = 0; i < procedureEquipment.size(); i++){
            for (Map.Entry<String,String> procedureInfoEntry: procedureInfo.entrySet()) {
                procedureEquipment.get(i).put(procedureInfoEntry.getKey(),procedureInfoEntry.getValue());
            }
        }

        for(int i = 0; i < procedureEquipment.size(); i++){
            DocumentReference procedureCounterRef = db.collection("networks").document(mNetworkId)
                    .collection("hospitals").document(mHospitalId).collection("departments")
                    .document("default_department").collection("dis")
                    .document(Objects.requireNonNull(procedureEquipment.get(i).get("di")).toString()).collection("udis")
                    .document(Objects.requireNonNull(procedureEquipment.get(i).get("udi")).toString());

            final int quantityUsed = Integer.parseInt(Objects.requireNonNull(procedureEquipment.get(i).get("amount_used")).toString());

            final int finalI = i;
            procedureCounterRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (Objects.requireNonNull(document).exists()) {
                            if(document.get("procedure_number") != null){
                                String procedureCounter = document.getString("procedure_number");
                                saveData(procedureEquipment.get(finalI),procedureCounter ,quantityUsed);
                            }else{
                                saveData(procedureEquipment.get(finalI),"0",quantityUsed);
                            }
                        } else {
                            Toast.makeText(parent, "Procedure information for " + procedureEquipment.get(finalI).get("udi") +
                                    " has not been saved", Toast.LENGTH_SHORT).show();
                            Log.d(TAG, "No such document");
                        }
                    } else {
                        Log.d(TAG, "get failed with ", task.getException());
                    }
                }
            });
        }
        if(parent != null){
            Toast.makeText(getActivity(), "Procedure information saved", Toast.LENGTH_SHORT).show();
            parent.onBackPressed();
        }


    }

    private void saveData(HashMap<String, Object> procedureEquipment, String procedureCounter, final int quantityUsed){
        final String accessionNumber = procedureInfo.get("accession_number");
        final String udi = Objects.requireNonNull(procedureEquipment.get("udi")).toString();
        final String di = Objects.requireNonNull(procedureEquipment.get("di")).toString();
        procedureEquipment.remove("udi");
        procedureEquipment.remove("di");
        final String[] udiQuantity = new String[1];
        final String[] diQuantity = new String[1];

        DocumentReference procedureDocRef = db.collection("networks").document(mNetworkId)
                .collection("hospitals").document(mHospitalId).collection("departments")
                .document("default_department").collection("dis").document(di)
                .collection("udis").document(udi);

        final DocumentReference quantityRef = db.collection("networks").document(mNetworkId)
                .collection("hospitals").document(mHospitalId).collection("departments")
                .document("default_department").collection("dis").document(di);

        quantityRef.collection("udis").document(udi).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task){

                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        if(document.get("quantity") != null){
                            udiQuantity[0] = document.getString("quantity");
                        }else{
                            udiQuantity[0] = "0";
                        }
                        int diff = updateUdiQuantity(Integer.parseInt(udiQuantity[0]),quantityUsed, quantityRef, udi);
                        if (diff < 0) {
                            Toast.makeText(parent, "There was " + Math.abs(diff) +
                                    " more of the equipment used during the procedure, than the known available quantity.", Toast.LENGTH_SHORT).show();
                        }
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        quantityRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        if(document.get("quantity") != null){
                            diQuantity[0] = document.getString("quantity");
                            int newQuantity = updateDiQuantity(Integer.parseInt(diQuantity[0]),quantityUsed,quantityRef);
                            if(Integer.parseInt(diQuantity[0]) <= 8){
                                notification(newQuantity,udiStr);
                            }
                        }else{
                            diQuantity[0] = "0";
                        }
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });


        // saves procedure information for each udi
        procedureDocRef.collection("procedures")
                .document("procedure_" + (Integer.parseInt(procedureCounter) + 1)).set(procedureEquipment)
                //in case of success
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "procedure info saved");
                    }
                })
                // in case of failure
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), "Error while saving data!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, e.toString());
                    }
                });

        // saves accession number in a collection of accession numbers
        HashMap<String, Object> accessionNumberMap = new HashMap<>();
        accessionNumberMap.put("accession_number", accessionNumber);
        db.collection("networks").document(mNetworkId).collection("hospitals")
                .document(mHospitalId).collection("accession_numbers").document(accessionNumber)
                .set(accessionNumberMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully written!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error writing document", e);
                    }
                });

        // updates number of procedures saved for each udi
        HashMap<String, Object> procedureCounterMap = new HashMap<>();
        int procedureCount = Integer.parseInt(procedureCounter);
        procedureCount++;
        procedureCounterMap.put("procedure_number",String.valueOf(procedureCount));
        procedureDocRef.set(procedureCounterMap, SetOptions.merge())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG,"Procedure counter has been saved");
                    }

                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(), "Error while saving data!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, e.toString());
                    }
                });
    }

    // updates UDI quantity
    // returns true difference
    private int updateUdiQuantity(int currentQuantity, int quantityUsed, DocumentReference quantityRef, String udi){
        int diff = currentQuantity - quantityUsed;

        quantityRef.collection("udis").document(udi)
                .update("quantity", String.valueOf(Math.max(diff, 0)))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });

        return diff;
    }
    // updates DI quantity
    // returns new quantity
    private int updateDiQuantity(int currentQuantity, int quantityUsed, DocumentReference quantityRef){
        int newQuantity = Math.max(currentQuantity - quantityUsed, 0);
        quantityRef
                .update("quantity", String.valueOf(newQuantity)) // either sets quantity to the difference or 0 if difference < 0
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "DocumentSnapshot successfully updated!");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error updating document", e);
                    }
                });
        return newQuantity;
    }

    private void notification(int diQuantityInt,String udiStr){
        String messageHeader = udiStr + " are running low.";
        String messageBody = "There are " + diQuantityInt + " " +  " remaining items.";

        HashMap<String, Object> notificationMessage = new HashMap<>();
        notificationMessage.put("header", messageHeader);
        notificationMessage.put("body", messageBody);
        notificationMessage.put("hospital_id", mHospitalId);

        CollectionReference notifRef = db.collection("networks").document(mNetworkId)
                .collection("hospitals").document(mHospitalId).collection("notifications");

        notifRef.add(notificationMessage).addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
            @Override
            public void onSuccess(DocumentReference documentReference) {
                Toast.makeText(parent, "Notification Added", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(parent, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

    }

    // check if procedureInfo has required fields
    private void checkProcedureInfo() {
        Objects.requireNonNull(procedureInfo);
        Objects.requireNonNull(procedureInfo.get("procedure_date"));
        Objects.requireNonNull(procedureInfo.get("procedure_used"));
        Objects.requireNonNull(procedureInfo.get("time_in"));
        Objects.requireNonNull(procedureInfo.get("time_out"));
        Objects.requireNonNull(procedureInfo.get("room_time"));
        Objects.requireNonNull(procedureInfo.get("fluoro_time"));
        Objects.requireNonNull(procedureInfo.get("accession_number"));
    }
}