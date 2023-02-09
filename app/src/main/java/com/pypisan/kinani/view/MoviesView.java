package com.pypisan.kinani.view;

import android.content.res.Resources;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.facebook.shimmer.ShimmerFrameLayout;
import com.pypisan.kinani.R;
import com.pypisan.kinani.adapter.RecentAdapter;
import com.pypisan.kinani.api.RequestModule;
import com.pypisan.kinani.model.AnimeModel;
import com.pypisan.kinani.model.AnimeRecentModel;
import com.pypisan.kinani.storage.AnimeManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MoviesView extends Fragment implements RecentAdapter.SelectListener {

    private ArrayList<AnimeModel> animeList, animeListInc;
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapterMovies;
    private ShimmerFrameLayout containerMovies;
    private NestedScrollView nestedScrollViewMovie;
    private ProgressBar progressBarMovie;
    private AnimeManager animeManager;

    public MoviesView() {
        // Required empty public constructor
    }

    public static MoviesView newInstance() {
        return new MoviesView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.movies_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        animeListInc = new ArrayList<>();
        containerMovies = view.findViewById(R.id.shimmer_movies_layout);
        containerMovies.startShimmer();
        nestedScrollViewMovie = view.findViewById(R.id.nestedMovies);
        progressBarMovie = view.findViewById(R.id.movieProgress);

        insertDataToCard("3");

        //      initialization recycler

        recyclerView = view.findViewById(R.id.movie_recycler_view);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        recyclerView.setHasFixedSize(true);

        //        Item Declaration
//        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, dpToPx(2), true));
//        Log.d("hello", "anime list is " + animeList.size());
        adapterMovies = new RecentAdapter(getContext(), animeListInc, this);


//        infinite Scroller

    }

    private void insertDataToCard(String pageNum) {
        // Add the cards data and display them
//        fetching data
        if (pageNum.equals("")) {
            pageNum = "3";
        }
        animeList = new ArrayList<>();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://anime.pypisan.com/v1/anime/")
                .addConverterFactory(GsonConverterFactory
                        .create())
                .build();

        RequestModule moviesList = retrofit.create(RequestModule.class);
        Call<AnimeRecentModel> call = moviesList.getMovies(pageNum);

        call.enqueue(new Callback<AnimeRecentModel>() {
            @Override
            public void onResponse(Call<AnimeRecentModel> call, Response<AnimeRecentModel> response) {
//            Log.d("Hey1", "Response code is : " + response.code());
                AnimeRecentModel resource = response.body();
                boolean status = resource.getSuccess();
                if (status) {
                    List<AnimeRecentModel.datum> data = resource.getData();
                    AnimeModel model = new AnimeModel();
//                    int i = 0;
                    for (AnimeRecentModel.datum animes : data) {
//                        Log.d("Hey3", "Response code is : " + response.body() +  i);
                        model = new AnimeModel(animes.getImageLink(),
                                animes.getAnimeDetailLink(),
                                animes.getTitle(),
                                animes.getReleased());
                        animeList.add(model);
                    }
                    onMoviesDataLoad(0);
                    containerMovies.stopShimmer();
                    containerMovies.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    recyclerView.setItemAnimator(new DefaultItemAnimator());
                    recyclerView.setAdapter(adapterMovies);

                } else {
//                    Toast.makeText(this, "Response not found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AnimeRecentModel> call, Throwable t) {
//            Log.d("Hey3", "Response code is : 400" + t.getMessage());
            }
        });
    }

    private void onMoviesDataLoad(int offset) {
        if (animeListInc != null) {
            animeListInc.clear();
        }
        if (animeList.size() <= 42) {
            for (int i = offset; i < animeList.size(); i++) {
                animeListInc.add(animeList.get(i));
            }
            adapterMovies.notifyItemInserted(animeList.size());
        } else {
            for (int i = offset; i < 42; i++) {
                animeListInc.add(animeList.get(i));
            }
            adapterMovies.notifyItemInserted(39);
        }
    }

    @Override
    public void onItemClicked(String title, String detail, String image) {
        animeManager = new AnimeManager(getContext());
        animeManager.open();
        animeManager.insertRecent(detail, title, image);
        animeManager.close();
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        Fragment fragment = SummaryView.newInstance();
        fragment.setArguments(bundle);
        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentView, fragment, "summary_view");
        transaction.addToBackStack(null);
        transaction.commit();

    }

    // convert dp to pixels
    private int dpToPx(int dp) {
        Resources r = getResources();
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics()));
    }

    private class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

        private final int spanCount;
        private final int spacing;
        private final boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {

            int position = parent.getChildAdapterPosition(view);
            int column = position % spanCount;
            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount;
                outRect.right = (column + 1) * spacing / spanCount;
                if (position < spanCount) {
                    outRect.top = spacing;
                }
                outRect.bottom = spacing;
            } else {
                outRect.left = column * spacing / spanCount;
                outRect.right = spacing - (column + 1);
                if (position >= spanCount) {
                    outRect.top = spacing;
                }
            }
        }
    }
}