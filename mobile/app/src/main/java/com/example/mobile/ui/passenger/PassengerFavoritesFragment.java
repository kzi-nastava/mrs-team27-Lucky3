package com.example.mobile.ui.passenger;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.mobile.R;
import com.example.mobile.models.FavoriteRouteResponse;
import com.example.mobile.utils.ListViewHelper;
import com.example.mobile.viewmodels.PassengerFavoritesViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PassengerFavoritesFragment extends Fragment {

    private static final String TAG = "PassengerFavorites";

    private PassengerFavoritesViewModel viewModel;

    // UI elements
    private ListView favoritesListView;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private TextView tvFavoriteCount;

    // Data
    private List<FavoriteRouteResponse> favoriteRoutes = new ArrayList<>();
    private FavoritesAdapter adapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_passenger_favorites, container, false);

        // Initialize ViewModel
        viewModel = new ViewModelProvider(this).get(PassengerFavoritesViewModel.class);

        initViews(root);
        setupNavbar(root);
        setupListView();
        observeViewModel();

        // Load favorite routes
        viewModel.loadFavoriteRoutes();

        return root;
    }

    private void initViews(View root) {
        favoritesListView = root.findViewById(R.id.favorites_list_view);
        progressBar = root.findViewById(R.id.progress_bar);
        tvEmpty = root.findViewById(R.id.tv_empty);
        tvFavoriteCount = root.findViewById(R.id.tv_favorite_count);
    }

    private void setupNavbar(View root) {
        View navbar = root.findViewById(R.id.navbar);
        if (navbar != null) {
            navbar.findViewById(R.id.btn_menu).setOnClickListener(v ->
                    ((com.example.mobile.MainActivity) requireActivity()).openDrawer());
            ((TextView) navbar.findViewById(R.id.toolbar_title)).setText("Favorites");
        }
    }

    private void setupListView() {
        adapter = new FavoritesAdapter(favoriteRoutes);
        favoritesListView.setAdapter(adapter);
    }

    private void observeViewModel() {
        // Observe favorite routes
        viewModel.getFavoriteRoutes().observe(getViewLifecycleOwner(), routes -> {
            if (routes != null) {
                favoriteRoutes.clear();
                favoriteRoutes.addAll(routes);
                adapter.notifyDataSetChanged();
                ListViewHelper.setListViewHeightBasedOnChildren(favoritesListView);

                // Update count
                tvFavoriteCount.setText(routes.size() + " favorite" + (routes.size() != 1 ? "s" : ""));
                tvFavoriteCount.setVisibility(routes.isEmpty() ? View.GONE : View.VISIBLE);
                tvEmpty.setVisibility(routes.isEmpty() ? View.VISIBLE : View.GONE);
            }
        });

        // Observe loading state
        viewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (isLoading != null) {
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            }
        });

        // Observe errors
        viewModel.getError().observe(getViewLifecycleOwner(), errorMsg -> {
            if (errorMsg != null && !errorMsg.isEmpty()) {
                Toast.makeText(getContext(), errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        // Observe success messages
        viewModel.getSuccessMessage().observe(getViewLifecycleOwner(), message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ==================== Inner Adapter ====================
    private class FavoritesAdapter extends BaseAdapter {
        private final List<FavoriteRouteResponse> routes;

        FavoritesAdapter(List<FavoriteRouteResponse> routes) {
            this.routes = routes;
        }

        @Override
        public int getCount() {
            return routes.size();
        }

        @Override
        public FavoriteRouteResponse getItem(int position) {
            return routes.get(position);
        }

        @Override
        public long getItemId(int position) {
            FavoriteRouteResponse route = routes.get(position);
            return route.getId() != null ? route.getId() : position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_favorite_route, parent, false);
                holder = new ViewHolder();
                holder.tvRouteName = convertView.findViewById(R.id.tv_route_name);
                holder.tvDeparture = convertView.findViewById(R.id.tv_departure);
                holder.tvDestination = convertView.findViewById(R.id.tv_destination);
                holder.tvDistance = convertView.findViewById(R.id.tv_distance);
                holder.tvEstimatedTime = convertView.findViewById(R.id.tv_estimated_time);
                holder.btnOrder = convertView.findViewById(R.id.btn_order);
                holder.btnRemove = convertView.findViewById(R.id.btn_remove);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            FavoriteRouteResponse route = getItem(position);

            // Route name
            if (route.getRouteName() != null && !route.getRouteName().isEmpty()) {
                holder.tvRouteName.setText(route.getRouteName());
                holder.tvRouteName.setVisibility(View.VISIBLE);
            } else {
                holder.tvRouteName.setVisibility(View.GONE);
            }

            // Departure
            if (route.getStartLocation() != null && route.getStartLocation().getAddress() != null) {
                holder.tvDeparture.setText(route.getStartLocation().getAddress());
            } else {
                holder.tvDeparture.setText("—");
            }

            // Destination
            if (route.getEndLocation() != null && route.getEndLocation().getAddress() != null) {
                holder.tvDestination.setText(route.getEndLocation().getAddress());
            } else {
                holder.tvDestination.setText("—");
            }

            // Distance
            if (route.getDistance() != null) {
                holder.tvDistance.setText(String.format(Locale.US, "%.1f km", route.getDistance()));
            } else {
                holder.tvDistance.setText("—");
            }

            // Estimated time
            if (route.getEstimatedTime() != null) {
                holder.tvEstimatedTime.setText(String.format(Locale.US, "~%.0f min", route.getEstimatedTime()));
            } else {
                holder.tvEstimatedTime.setText("—");
            }

            // Order button
            holder.btnOrder.setOnClickListener(v -> {
                // Navigate to order ride with pre-filled data
                Bundle args = new Bundle();
                if (route.getStartLocation() != null) {
                    args.putString("startAddress", route.getStartLocation().getAddress());
                    args.putDouble("startLat", route.getStartLocation().getLatitude());
                    args.putDouble("startLng", route.getStartLocation().getLongitude());
                }
                if (route.getEndLocation() != null) {
                    args.putString("endAddress", route.getEndLocation().getAddress());
                    args.putDouble("endLat", route.getEndLocation().getLatitude());
                    args.putDouble("endLng", route.getEndLocation().getLongitude());
                }

                //TODO: fix this

                // Navigate to ride creation screen
                //Navigation.findNavController(requireView())
                //        .navigate(R.id.action_passenger_favorites_to_order_ride, args);
            });

            // Remove button
            holder.btnRemove.setOnClickListener(v -> {
                if (route.getId() != null) {
                    viewModel.removeFavoriteRoute(route.getId());
                }
            });

            return convertView;
        }

        class ViewHolder {
            TextView tvRouteName, tvDeparture, tvDestination, tvDistance, tvEstimatedTime;
            Button btnOrder, btnRemove;
        }
    }
}
