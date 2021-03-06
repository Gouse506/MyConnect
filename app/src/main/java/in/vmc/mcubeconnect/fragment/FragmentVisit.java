package in.vmc.mcubeconnect.fragment;


import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.volley.RequestQueue;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import in.vmc.mcubeconnect.R;
import in.vmc.mcubeconnect.activity.Home;
import in.vmc.mcubeconnect.activity.MyApplication;
import in.vmc.mcubeconnect.adapter.VisitAdapter;
import in.vmc.mcubeconnect.callbacks.Popupcallback;
import in.vmc.mcubeconnect.model.VisitData;
import in.vmc.mcubeconnect.parser.Parser;
import in.vmc.mcubeconnect.parser.Requestor;
import in.vmc.mcubeconnect.utils.ConnectivityReceiver;
import in.vmc.mcubeconnect.utils.JSONParser;
import in.vmc.mcubeconnect.utils.ReferDialogFragment;
import in.vmc.mcubeconnect.utils.SingleTon;
import in.vmc.mcubeconnect.utils.TAG;
import in.vmc.mcubeconnect.utils.Utils;


public class FragmentVisit extends Fragment implements TAG, SwipeRefreshLayout.OnRefreshListener {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private ArrayList<VisitData> VisitData = new ArrayList<>();
    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private Popupcallback popupcallback;
    private VisitAdapter adapter;
    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ReferDialogFragment referDialogFragment = new ReferDialogFragment();
    private RelativeLayout mroot;
    private String STATE_VISITDATA = "STATE_VISITDATA";
    private RequestQueue requestQueue;
    private SingleTon volleySingleton;

    public FragmentVisit() {
        // Required empty public constructor
    }

    public static FragmentVisit newInstance(String param1, String param2) {
        FragmentVisit fragment = new FragmentVisit();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        //save the movie list to a parcelable prior to rotation or configuration change
        outState.putParcelableArrayList(STATE_VISITDATA, VisitData);


    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        volleySingleton = SingleTon.getInstance();
        requestQueue = volleySingleton.getRequestQueue();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragmentall, container, false);
        swipeRefreshLayout = (SwipeRefreshLayout) view.findViewById(R.id.SwipefollowUp);
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
        mroot = (RelativeLayout) view.findViewById(R.id.root);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        swipeRefreshLayout.setOnRefreshListener(this);
        swipeRefreshLayout.setColorSchemeResources(
                R.color.refresh_progress_1,
                R.color.refresh_progress_2,
                R.color.refresh_progress_3);
        adapter = new VisitAdapter(getActivity(), VisitData, mroot, FragmentVisit.this);
        recyclerView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (savedInstanceState != null) {
            VisitData = savedInstanceState.getParcelableArrayList(STATE_VISITDATA);
            if (VisitData != null) {
                adapter.setData(VisitData);
                Log.d("RESPONSE", "VISIT LODED SCREEN ORIENTATION");
            }

        } else {
            VisitData = MyApplication.getWritableDatabase().getAllSites(3);
            if (VisitData != null && VisitData.size() > 0) {
                adapter.setData(VisitData);
            } else {
                GetVisits();
            }
        }
    }

    @Override
    public void onRefresh() {

        // swipeRefreshLayout.setRefreshing(false);
        GetVisits();

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            Activity activity = (Activity) context;
            popupcallback = (Home) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(e.toString()
                    + " must implement SellFragmentListener");
        }

    }


    public void GetVisits() {

        if (ConnectivityReceiver.isConnected()) {
            new GetVistHistory().execute();
            if (Home.snack.isShown()) {
                Home.snack.dismiss();
            }
        } else {
            if (getActivity() != null) {
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (!Home.snack.isShown()) {
                    Home.snack = Snackbar.make(getView(), "No Internet Connection", Snackbar.LENGTH_SHORT)
                            .setAction(getString(R.string.text_tryAgain), new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    GetVisits();

                                }
                            })
                            .setActionTextColor(ContextCompat.getColor(getActivity(), R.color.accent));
                    View view = Home.snack.getView();
                    TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    Home.snack.setDuration(Snackbar.LENGTH_INDEFINITE);
                    Home.snack.show();
                }
            }
        }

    }

    public void Resetdapter() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                GetVisits();

            }
        }, 800);

    }

    class GetVistHistory extends AsyncTask<Void, Void, ArrayList<VisitData>> {
        String message = "n";
        String code = "n";
        JSONObject response = null;
        VisitData visitData = null;
        JSONArray data = null;
        ArrayList<VisitData> visitDatas;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

        }


        @Override
        protected ArrayList<VisitData> doInBackground(Void... params) {
            // TODO Auto-generated method stub

            try {
               // response = JSONParser.GetVistDetail(VISTLIST, ((Home) getActivity()).authkey);
                response = Requestor.requestVistDetail(requestQueue,VISTLIST, ((Home) getActivity()).authkey);
                Log.d("RESPONSE", response.toString());
               // visitDatas = new ArrayList<VisitData>();
                visitDatas = Parser.ParseVisitData(response,true);


                if (response.has(CODE)) {
                    code = response.getString(CODE);

                }
                if (response.has(MESSAGE)) {
                    message = response.getString(MESSAGE);
                }
//
            } catch (Exception e) {

            }
            return visitDatas;
        }

        @Override
        protected void onPostExecute(ArrayList<VisitData> data) {
            if (swipeRefreshLayout.isRefreshing()) {
                swipeRefreshLayout.setRefreshing(false);
            }
            if (data != null) {
                VisitData = data;
                MyApplication.getWritableDatabase().insertAllSites(3, VisitData, true);
                //adapter.setData(data);
                adapter = new VisitAdapter(getActivity(), VisitData, mroot, FragmentVisit.this);
                recyclerView.setAdapter(adapter);
            }


        }


    }

}
