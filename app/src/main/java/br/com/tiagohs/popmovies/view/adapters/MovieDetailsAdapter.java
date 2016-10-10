package br.com.tiagohs.popmovies.view.adapters;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import br.com.tiagohs.popmovies.model.movie.MovieDetails;
import br.com.tiagohs.popmovies.view.fragment.MovieDetailsMidiaFragment;
import br.com.tiagohs.popmovies.view.fragment.MovieDetailsOverviewFragment;
import br.com.tiagohs.popmovies.view.fragment.MovieDetailsReviewsFragment;


public class MovieDetailsAdapter extends FragmentStatePagerAdapter {
    private static final int TAB_RESUMO = 0;
    private static final int TAB_MIDIA = 1;
    private static final int TAB_REVIEWS = 2;

    private MovieDetails mMovie;
    private String[] mTabsNames;

    public MovieDetailsAdapter(FragmentManager fm, MovieDetails movie, String[] tabNames) {
        super(fm);

        this.mMovie = movie;
        this.mTabsNames = tabNames;
    }

    @Override
    public Fragment getItem(int tabSelecionada) {

        switch (tabSelecionada) {
            case TAB_RESUMO:
                return MovieDetailsOverviewFragment.newInstance(mMovie);

            case TAB_MIDIA:
                return MovieDetailsMidiaFragment.newInstance(mMovie);

            case TAB_REVIEWS:
                return MovieDetailsReviewsFragment.newInstance(mMovie);

            default:
                return MovieDetailsOverviewFragment.newInstance(mMovie);
        }

    }

    @Override
    public int getCount() {
        return mTabsNames.length;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabsNames[position];
    }
}