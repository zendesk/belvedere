package zendesk.belvedere;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class HeadlessFragment<E> extends Fragment {

    private static final String TAG = "HeadlessFragment";

    /**
     * Retrieve previously stored data.
     *
     * @param fm the {@link FragmentManager}
     * @param <E> type of the stored object
     * @return the stored data or {@code null} if fragment wasn't installed or no data is available
     */
    static <E> E getData(FragmentManager fm) {
        final Fragment fragment = fm.findFragmentByTag(TAG);

        if(fragment instanceof HeadlessFragment<?>) {
            //noinspection unchecked
            return ((HeadlessFragment<E>) fragment).getData();
        } else {
            return null;
        }
    }

    /**
     * Put an object into a fragment.
     *
     * @param fm the {@link FragmentManager}
     * @param data object to store
     * @param <E> type object
     */
    static <E> void install(FragmentManager fm, E data) {
        final HeadlessFragment<E> fragment = new HeadlessFragment<>();
        fragment.setData(data);

        fm.beginTransaction()
                .add(fragment, TAG)
                .commit();
    }

    private E data;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        setRetainInstance(true);
        return null;
    }

    private void setData(E data) {
        this.data = data;
    }

    private E getData() {
        return data;
    }

}