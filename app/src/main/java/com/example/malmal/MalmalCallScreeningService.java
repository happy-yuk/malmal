package com.example.malmal;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telecom.Call;
import android.telecom.CallScreeningService;
import android.telecom.Connection;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import java.util.Locale;

public class MalmalCallScreeningService extends CallScreeningService {
    @Override
    public void onScreenCall(Call.Details callDetails) {
        Log.d("MalmalCallScreeningService", "ENTERED");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            boolean isIncoming = callDetails.getCallDirection() == Call.Details.DIRECTION_INCOMING;
            boolean isOutgoing = callDetails.getCallDirection() == Call.Details.DIRECTION_OUTGOING;

            if (isIncoming) {
                switch (callDetails.getCallerNumberVerificationStatus()) {
                    case Connection.VERIFICATION_STATUS_NOT_VERIFIED:
//                        Timber.w("No caller verification was performed for " + details.getFormattedPhoneNumber() + "!");
                        Log.d("MalmalCallScreeningService", "VERIFICATION_STATUS_NOT_VERIFIED");
                        break;
                    case Connection.VERIFICATION_STATUS_FAILED:
//                        Timber.e("Caller " + details.getFormattedPhoneNumber() + " FAILED verification!");
                        Log.d("MalmalCallScreeningService", "VERIFICATION_STATUS_FAILED");
                        break;
                    case Connection.VERIFICATION_STATUS_PASSED:
//                        Timber.i("Caller " + details.getFormattedPhoneNumber() + " is verified...");
                        Log.d("MalmalCallScreeningService", "VERIFICATION_STATUS_PASSED");
                        break;
                }

                Uri handle = callDetails.getHandle();
                String phoneNumber = (handle != null) ? handle.getSchemeSpecificPart() : null;

                if (phoneNumber != null) {
                    Log.d("INCOMMING PHONE NUMBER", ""+phoneNumber);
                    if (phoneNumber.toString().equals("01028815298")) {
                        if (Settings.canDrawOverlays(this)) {
                            String contactName = getContactName(phoneNumber);
                            Intent intent = new Intent(this, OverlayActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                                intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                            }
                            intent.putExtra("CALLER_NAME", contactName); // 데이터 전달
                            this.startActivity(intent);
                        } else {
//                           권한 얻기
                        }
                    }
                }

                String caller = null;
                if (callDetails.getHandle() != null) {
                    caller = getContactName(callDetails.getHandle().getSchemeSpecificPart());
                }

//                boolean rejectDueToVerificationStatus = false;
//                switch (details.getCallerNumberVerificationStatus()) {
//                    case Connection.VERIFICATION_STATUS_FAILED:
////                        rejectDueToVerificationStatus = ScreeningPreferences.declineAuthenticationFailures();
//                        break;
//                    case Connection.VERIFICATION_STATUS_NOT_VERIFIED:
////                        rejectDueToVerificationStatus = ScreeningPreferences.declineUnauthenticatedCallers();
//                        break;
//                }

//                boolean rejectDueToUnknownCaller = caller == null && ScreeningPreferences.declineUnknownCallers();

//                CallResponse response;
//                if (rejectDueToVerificationStatus || rejectDueToUnknownCaller) {
//                    response = buildRejectionResponse();
//                } else {
//                    response = buildAcceptResponse();
//                }

//                respondToCall(details, response);
            } else if (isOutgoing) {
                Uri handle = callDetails.getHandle();
                String phoneNumber = (handle != null) ? handle.getSchemeSpecificPart() : null;

                if (phoneNumber != null) {
                    Log.d("OUTGOING PHONE NUMBER", ""+phoneNumber);
                }
            }
        }
    }

    private CallScreeningService.CallResponse buildAcceptResponse() {
        return new CallScreeningService.CallResponse.Builder().build();
    }

//    private CallScreeningService.CallResponse buildRejectionResponse() {
//        Timber.d("Reject? " + ScreeningPreferences.isServiceEnabled()
//                + ", Notify? " + !ScreeningPreferences.skipCallNotification()
//                + ", Log? " + !ScreeningPreferences.skipCallLog());
//        return new CallScreeningService.CallResponse.Builder()
//                .setDisallowCall(ScreeningPreferences.isServiceEnabled())
//                .setRejectCall(ScreeningPreferences.isServiceEnabled())
//                .setSkipNotification(ScreeningPreferences.skipCallNotification())
//                .setSkipCallLog(ScreeningPreferences.skipCallLog())
//                .build();
//    }

    private String getContactName(String phoneNumber) {
        Uri uri = Uri.withAppendedPath(
                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
                Uri.encode(phoneNumber)
        );

        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(0);
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    private static class CallDetailsExtensions {
        static String getFormattedPhoneNumber(Call.Details details) {
            String phoneNumber = details.getHandle() != null ? details.getHandle().getSchemeSpecificPart() : null;
            return phoneNumber != null ? PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().getCountry()) : "BLOCKED";
        }
    }
}
