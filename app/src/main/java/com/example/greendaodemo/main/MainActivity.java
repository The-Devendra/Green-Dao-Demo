package com.example.greendaodemo.main;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.greendaodemo.R;
import com.example.greendaodemo.database.base.BaseRepo;
import com.example.greendaodemo.database.user.entity.UserEntity;
import com.example.greendaodemo.database.user.operations.UserOperations;
import com.example.greendaodemo.databinding.ActivityMainBinding;
import com.example.greendaodemo.databinding.CustomMaterialDilogBinding;
import com.example.greendaodemo.main.adapter.RecyclerViewAdapter;
import com.example.greendaodemo.utils.Constants;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private ActivityMainBinding binding;
    private CustomMaterialDilogBinding materialDilogBinding;
    private Context context;
    private String selectedState;
    private RecyclerViewAdapter recyclerViewAdapter;
    private UserOperations userOperations;
    private ArrayAdapter<CharSequence> spinnerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        context = this;
        userOperations = UserOperations.getInstance(context);

        spinnerAdapter = ArrayAdapter.createFromResource(context,
                R.array.state_name, android.R.layout.simple_spinner_item);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        binding.spinnerState.setAdapter(spinnerAdapter);
        binding.spinnerState.setOnItemSelectedListener(this);


        binding.recyclerView.setNestedScrollingEnabled(true);
        binding.recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(context);
        binding.recyclerView.setLayoutManager(linearLayoutManager);
        recyclerViewAdapter = new RecyclerViewAdapter(context);
        binding.recyclerView.setAdapter(recyclerViewAdapter);
        setRecyclerView();

        binding.tvSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String userName = binding.etUserName.getText().toString().trim();
                if (userName.equals(Constants.EMPTY)) {
                    binding.etUserName.setError(getString(R.string.user_name_error));
                    Toast.makeText(context, R.string.user_name_error, Toast.LENGTH_SHORT).show();
                } else if (selectedState == null || selectedState.equals(Constants.ENTER_YOUR_STATE)) {
                    Toast.makeText(context, R.string.state_error, Toast.LENGTH_SHORT).show();
                } else {
                    insertDataToInternal(userName, selectedState);
                    selectedState = Constants.ENTER_YOUR_STATE;

                }
            }
        });

        recyclerViewAdapter.setListener(new RecyclerViewAdapter.ItemListener() {
            @Override
            public void onClick(final UserEntity userEntity, final int position) {
                materialDilogBinding = DataBindingUtil.inflate(LayoutInflater.from(context), R.layout.custom_material_dilog, null, false);

                final MaterialDialog materialDialog = new MaterialDialog.Builder(context)
                        .title(R.string.title)
                        .customView(materialDilogBinding.getRoot(), true)
                        .show();

                selectedState = userEntity.getUserState();
                materialDilogBinding.etUserName.setText(userEntity.getUserName());
                materialDilogBinding.spinnerState.setAdapter(spinnerAdapter);
                materialDilogBinding.spinnerState.setSelection(spinnerAdapter.getPosition(userEntity.getUserState()));
                materialDilogBinding.spinnerState.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        selectedState = adapterView.getItemAtPosition(i).toString();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {

                    }
                });

                materialDilogBinding.tvDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        userOperations.delete(userEntity.getUserId());
                        recyclerViewAdapter.delete(position);
                        materialDialog.dismiss();
                    }
                });
                materialDilogBinding.tvUpdate.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        String userName = materialDilogBinding.etUserName.getText().toString();

                        if (userName.equals(userEntity.getUserName()) && selectedState.equals(userEntity.getUserState())) {
                            Toast.makeText(context, "You have not edited any thing yet to update", Toast.LENGTH_SHORT).show();
                        } else if (selectedState.equals(Constants.ENTER_YOUR_STATE)) {
                            Toast.makeText(context, R.string.state_error, Toast.LENGTH_SHORT).show();
                        } else {
                            userEntity.setUserState(selectedState);
                            userEntity.setUserName(userName);
                            userOperations.upDate(userEntity);
                            recyclerViewAdapter.notifyItemChanged(position);
                            materialDialog.dismiss();
                        }

                    }
                });
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_delete_all) {

            if (userOperations.getUserEntityList().size() == 0) {
                Toast.makeText(context, "You have no enter to delete", Toast.LENGTH_SHORT).show();
            } else {
                new MaterialDialog.Builder(this)
                        .title(R.string.title)
                        .content(R.string.content)
                        .positiveText(R.string.agree)
                        .negativeText(R.string.disagree)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                new BaseRepo().truncate(UserEntity.class);
                                recyclerViewAdapter.truncate();
                                dialog.dismiss();
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                dialog.dismiss();
                            }
                        })
                        .show();

            }
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        selectedState = adapterView.getItemAtPosition(i).toString();
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {

    }

    private void insertDataToInternal(String userName, String state) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUserName(userName);
        userEntity.setUserState(state);
        userOperations.insertUser(userEntity);
        addItemToRecyclerView(userEntity);
    }

    private void addItemToRecyclerView(UserEntity userEntity) {
        recyclerViewAdapter.insertItem(userEntity);
        binding.etUserName.setText(Constants.EMPTY);
        binding.spinnerState.setSelection(0);
    }

    private void setRecyclerView() {

        if (userOperations != null) {
            recyclerViewAdapter.setData(userOperations.getUserEntityList());
        }
    }

}
