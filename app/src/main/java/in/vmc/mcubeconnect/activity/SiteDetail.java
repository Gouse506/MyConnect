package in.vmc.mcubeconnect.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import in.vmc.mcubeconnect.R;
import in.vmc.mcubeconnect.adapter.Sitedetailadapter;
import in.vmc.mcubeconnect.model.SiteData;
import in.vmc.mcubeconnect.parser.Parser;
import in.vmc.mcubeconnect.parser.Requestor;
import in.vmc.mcubeconnect.utils.ConnectivityReceiver;
import in.vmc.mcubeconnect.utils.JSONParser;
import in.vmc.mcubeconnect.utils.SingleTon;
import in.vmc.mcubeconnect.utils.TAG;
import in.vmc.mcubeconnect.utils.Utils;

public class SiteDetail extends AppCompatActivity implements ConnectivityReceiver.ConnectivityReceiverListener,TAG, YouTubePlayer.OnInitializedListener {
    public static String siteId;
    public static String authkey;
    @InjectView(R.id.listView2)
    GridView listView;
    @InjectView(R.id.root)
    RelativeLayout mroot;
    @InjectView(R.id.desc)
    TextView tvdesc;
    ArrayList<SiteData> SiteData = new ArrayList<>();
    private Toolbar toolbar;
    private ProgressDialog pd;
    private Sitedetailadapter adapter;
    private YouTubePlayer.PlaybackEventListener playbackEventListener = new YouTubePlayer.PlaybackEventListener() {

        @Override
        public void onBuffering(boolean arg0) {
        }

        @Override
        public void onPaused() {
        }

        @Override
        public void onPlaying() {
        }

        @Override
        public void onSeekTo(int arg0) {
        }

        @Override
        public void onStopped() {
        }

    };
    private YouTubePlayer.PlayerStateChangeListener playerStateChangeListener = new YouTubePlayer.PlayerStateChangeListener() {

        @Override
        public void onAdStarted() {
        }

        @Override
        public void onError(YouTubePlayer.ErrorReason arg0) {
        }

        @Override
        public void onLoaded(String arg0) {
        }

        @Override
        public void onLoading() {
        }

        @Override
        public void onVideoEnded() {
        }

        @Override
        public void onVideoStarted() {
        }
    };
    private LinearLayout playerView;
    private YouTubePlayerSupportFragment youTubePlayerFragment;
    private YouTubePlayer videoPlayer;
    private String VIDEO_ID;
    private boolean fullScreen;
    private RequestQueue requestQueue;
    private SingleTon volleySingleton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_site_detail);
        ButterKnife.inject(this);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        playerView = (LinearLayout) findViewById(R.id.playerlayout);
        youTubePlayerFragment = (YouTubePlayerSupportFragment) getSupportFragmentManager().
                findFragmentById(R.id.youtube_fragment);
        volleySingleton = SingleTon.getInstance();
        requestQueue = volleySingleton.getRequestQueue();
//        siteId=getIntent().getExtras().getString("ID");
//        authkey=getIntent().getExtras().getString("authkey");
        Home.SCAN = false;
        GetSites();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(SiteDetail.this, LocationDetail.class);
                intent.putExtra(BID, SiteData.get(position).getBid());
                intent.putExtra(BEACONID, SiteData.get(position).getId());
                startActivity(intent);
            }
        });
        if (playerView.getVisibility() == View.VISIBLE) {
            playerView.setVisibility(View.GONE);
        }

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                //NavUtils.navigateUpFromSameTask(this);
                Home.SCAN = true;
                super.onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Home.SCAN = true;
        super.onBackPressed();
    }

    public void GetSites() {


        if (ConnectivityReceiver.isConnected()) {
            new Getsites().execute();
        } else {
            Snackbar snack = Snackbar.make(mroot, "No Internet Connection", Snackbar.LENGTH_SHORT)
                    .setAction(getString(R.string.text_tryAgain), new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            GetSites();

                        }
                    })
                    .setActionTextColor(ContextCompat.getColor(SiteDetail.this, R.color.accent));
            View view = snack.getView();
            TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
            tv.setTextColor(Color.WHITE);
            snack.show();
        }

    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player, boolean b) {
        player.setFullscreenControlFlags(0);
        player.setPlayerStateChangeListener(playerStateChangeListener);
        player.setPlaybackEventListener(playbackEventListener);
        player.setShowFullscreenButton(false);
        /** Start buffering **/
        videoPlayer = player;
        videoPlayer.setOnFullscreenListener(new YouTubePlayer.OnFullscreenListener() {

            @Override
            public void onFullscreen(boolean _isFullScreen) {
                fullScreen = _isFullScreen;
            }
        });
        if (!b) {
            player.cueVideo(VIDEO_ID);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
        Toast.makeText(SiteDetail.this, "Failured to Initialize!", Toast.LENGTH_LONG).show();
    }


    class Getsites extends AsyncTask<Void, Void, ArrayList<SiteData>> {
        String message = "n";
        String code = "n";
        JSONObject response = null;
        SiteData siteData = null;
        JSONArray data = null;
        ArrayList<SiteData> siteDatas;
        private String desc;
        private String media;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pd = new ProgressDialog(SiteDetail.this);
            pd.setMessage("Loading...");
            pd.show();
        }


        @Override
        protected ArrayList<SiteData> doInBackground(Void... params) {
            // TODO Auto-generated method stub

            try {
                response = Requestor.requestGetSiteDetail(requestQueue,GET_SITEDETAIL, authkey, siteId);
               // response = JSONParser.GetSiteDetail(GET_SITEDETAIL, authkey, siteId);
                Log.d("RESPONSE1", response.toString());
                siteDatas= Parser.ParseSiteData(response);

                if (response.has(CODE)) {
                    code = response.getString(CODE);

                }
                if (response.has(MESSAGE)) {
                    message = response.getString(MESSAGE);
                }


                if (response.has(SITEDESC)) {
                    desc = response.getString(SITEDESC);

                }
                if (response.has(SITEMEDIA)) {
                    media = response.getString(SITEMEDIA);

                }


            } catch (Exception e) {

            }
            return siteDatas;
        }

        @Override
        protected void onPostExecute(ArrayList<SiteData> data) {
            pd.dismiss();
            if (data != null) {
                SiteData = data;
                Log.d("SITE", SiteData.toString());
                listView.setAdapter(new Sitedetailadapter(SiteDetail.this, SiteData));
                tvdesc.setText(desc);

                if (media.length() > 3) {
                    VIDEO_ID = Utils.extractYTId(media);
                    youTubePlayerFragment.initialize(API_KEY, SiteDetail.this);
                    if (playerView.getVisibility() == View.GONE) {
                        playerView.setVisibility(View.VISIBLE);
                    }

                } else {
                    if (playerView.getVisibility() == View.VISIBLE) {
                        playerView.setVisibility(View.GONE);
                    }


                }


            }


        }


    }
    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.getInstance().setConnectivityListener(this);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        showSnack(isConnected);
    }



    private void showSnack(boolean isConnected) {
        String message;
        int color;
        if (!isConnected) {
            message = "Sorry! Not connected to internet";
            color = Color.RED;

            Snackbar snackbar = Snackbar
                    .make(mroot, message, Snackbar.LENGTH_LONG);

            View sbView = snackbar.getView();
            TextView textView = (TextView) sbView.findViewById(android.support.design.R.id.snackbar_text);
            textView.setTextColor(color);
            snackbar.show();
        }
    }

}
