package com.e.cardboard.ViewModel;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;



import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.e.cardboard.workers.CardWorker;
import com.e.cardboard.workers.CleanupWorker;
import com.e.cardboard.workers.SaveCardToFileWorker;

import static com.e.cardboard.Constants.CUSTOM_QUOTE;
import static com.e.cardboard.Constants.IMAGE_PROCESSING_WORK_NAME;
import static com.e.cardboard.Constants.KEY_IMAGE_URI;
import static com.e.cardboard.Constants.TAG_OUTPUT;


public class CustomCardViewModel extends AndroidViewModel {
    private Uri mImageUri;
    private WorkManager mWorkManager;
    private LiveData<List<WorkInfo>> mSavedWorkInfo;
    private Uri mOutputUri;

    public CustomCardViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);

        mSavedWorkInfo = mWorkManager.getWorkInfosByTagLiveData(TAG_OUTPUT);
    }

   public LiveData<List<WorkInfo>> getOutputWorkInfo() {
        return mSavedWorkInfo;
    }

  public  void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

  public  void setOutputUri(String outputImageUri) {
        mOutputUri = uriOrNull(outputImageUri);
    }
   public Uri getOutputUri(){ return mOutputUri;}

   public Uri getImageUri() {
        return mImageUri;
    }

   public void processImageToCard(String quote) {
        // the heavy lifting work happens!
        WorkContinuation continuation = mWorkManager
                .beginUniqueWork(IMAGE_PROCESSING_WORK_NAME,
                        ExistingWorkPolicy.REPLACE,
                        OneTimeWorkRequest.from(CleanupWorker.class));

        // Building our card
        OneTimeWorkRequest.Builder cardBuilder =
                new OneTimeWorkRequest.Builder(CardWorker.class);
        cardBuilder.setInputData(createInputDataForUri(quote));

        continuation = continuation.then(cardBuilder.build());

        // constraints
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true)
                .build();

        // WorkRequest to save the image to the filesystem
        OneTimeWorkRequest save = new OneTimeWorkRequest.Builder(SaveCardToFileWorker.class)
                .setConstraints(constraints)
                .addTag(TAG_OUTPUT)
                .build();
        continuation = continuation.then(save);

        // Seal the deal - start the work!
        continuation.enqueue();


    }

   public void cancelWork() {
        mWorkManager.cancelUniqueWork(IMAGE_PROCESSING_WORK_NAME);
    }

    private Data createInputDataForUri(String quote) {
        Data.Builder builder = new Data.Builder();
        if (mImageUri != null) {
            builder.putString(KEY_IMAGE_URI, mImageUri.toString());
            builder.putString(CUSTOM_QUOTE, quote);
        }
        return builder.build();

    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) {
            return Uri.parse(uriString);
        }
        return null;
    }
}
