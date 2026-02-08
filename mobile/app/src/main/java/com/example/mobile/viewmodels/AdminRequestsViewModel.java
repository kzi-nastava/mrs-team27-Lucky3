package com.example.mobile.viewmodels;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.mobile.models.DriverChangeRequest;
import com.example.mobile.models.ReviewDriverChangeRequest;
import com.example.mobile.utils.ClientUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminRequestsViewModel extends ViewModel {

    private final MutableLiveData<List<DriverChangeRequest>> requests = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final Set<Long> busyIds = new HashSet<>();
    private final MutableLiveData<Set<Long>> busyIdsLiveData = new MutableLiveData<>(new HashSet<>());

    public LiveData<List<DriverChangeRequest>> getRequests() {
        return requests;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Set<Long>> getBusyIds() {
        return busyIdsLiveData;
    }

    public void loadRequests() {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        ClientUtils.driverService.getDriverChangeRequests("PENDING")
                .enqueue(new Callback<List<DriverChangeRequest>>() {
                    @Override
                    public void onResponse(Call<List<DriverChangeRequest>> call,
                                           Response<List<DriverChangeRequest>> response) {
                        isLoading.setValue(false);
                        if (response.isSuccessful() && response.body() != null) {
                            requests.setValue(response.body());
                        } else {
                            errorMessage.setValue("Failed to load requests: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<List<DriverChangeRequest>> call, Throwable t) {
                        isLoading.setValue(false);
                        errorMessage.setValue("Network error: " + t.getMessage());
                    }
                });
    }

    public void approveRequest(Long requestId) {
        reviewRequest(requestId, true);
    }

    public void rejectRequest(Long requestId) {
        reviewRequest(requestId, false);
    }

    private void reviewRequest(Long requestId, boolean approve) {
        busyIds.add(requestId);
        busyIdsLiveData.setValue(new HashSet<>(busyIds));
        errorMessage.setValue(null);

        ReviewDriverChangeRequest review = new ReviewDriverChangeRequest(approve);

        ClientUtils.driverService.reviewDriverChangeRequest(requestId, review)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        busyIds.remove(requestId);
                        busyIdsLiveData.setValue(new HashSet<>(busyIds));

                        if (response.isSuccessful()) {
                            // Remove the request from the list
                            List<DriverChangeRequest> currentRequests = requests.getValue();
                            if (currentRequests != null) {
                                List<DriverChangeRequest> updatedRequests = new ArrayList<>();
                                for (DriverChangeRequest req : currentRequests) {
                                    if (!req.getId().equals(requestId)) {
                                        updatedRequests.add(req);
                                    }
                                }
                                requests.setValue(updatedRequests);
                            }
                        } else {
                            errorMessage.setValue("Failed to review request: " + response.code());
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        busyIds.remove(requestId);
                        busyIdsLiveData.setValue(new HashSet<>(busyIds));
                        errorMessage.setValue("Network error: " + t.getMessage());
                    }
                });
    }
}

