//package com.example.malmal;
//
//import android.database.Cursor;
//import android.net.Uri;
//import android.os.Build;
//import android.provider.ContactsContract;
//import android.telecom.Call;
//import android.telecom.CallScreeningService;
//import android.telecom.Connection;
//import android.telephony.PhoneNumberUtils;
//import android.util.Log;
//
//import java.util.Locale;
//
//public class MalmalCallScreeningService extends CallScreeningService {
//    @Override
//    public void onScreenCall(Call.Details details) {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
//            if (details.getCallDirection() == Call.Details.DIRECTION_INCOMING) {
//                switch (details.getCallerNumberVerificationStatus()) {
//                    case Connection.VERIFICATION_STATUS_NOT_VERIFIED:
////                        Timber.w("No caller verification was performed for " + details.getFormattedPhoneNumber() + "!");
//                        Log.d("");
//                        break;
//                    case Connection.VERIFICATION_STATUS_FAILED:
////                        Timber.e("Caller " + details.getFormattedPhoneNumber() + " FAILED verification!");
//                        Log.d("");
//                        break;
//                    case Connection.VERIFICATION_STATUS_PASSED:
////                        Timber.i("Caller " + details.getFormattedPhoneNumber() + " is verified...");
//                        Log.d("");
//                        break;
//                }
//
//                String caller = null;
//                if (details.getHandle() != null) {
//                    caller = getContactName(details.getHandle().getSchemeSpecificPart());
//                }
//
//                boolean rejectDueToVerificationStatus = false;
//                switch (details.getCallerNumberVerificationStatus()) {
//                    case Connection.VERIFICATION_STATUS_FAILED:
////                        rejectDueToVerificationStatus = ScreeningPreferences.declineAuthenticationFailures();
//                        break;
//                    case Connection.VERIFICATION_STATUS_NOT_VERIFIED:
////                        rejectDueToVerificationStatus = ScreeningPreferences.declineUnauthenticatedCallers();
//                        break;
//                }
//
////                boolean rejectDueToUnknownCaller = caller == null && ScreeningPreferences.declineUnknownCallers();
//
//                CallResponse response;
//                if (rejectDueToVerificationStatus || rejectDueToUnknownCaller) {
//                    response = buildRejectionResponse();
//                } else {
//                    response = buildAcceptResponse();
//                }
//
//                respondToCall(details, response);
//            }
//        }
//    }
//
//    private CallScreeningService.CallResponse buildAcceptResponse() {
//        return new CallScreeningService.CallResponse.Builder().build();
//    }
//
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
//
//    private String getContactName(String phoneNumber) {
//        Uri uri = Uri.withAppendedPath(
//                ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
//                Uri.encode(phoneNumber)
//        );
//
//        String[] projection = new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME};
//        Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
//        if (cursor != null) {
//            try {
//                if (cursor.moveToFirst()) {
//                    return cursor.getString(0);
//                }
//            } finally {
//                cursor.close();
//            }
//        }
//        return null;
//    }
//
//    private static class CallDetailsExtensions {
//        static String getFormattedPhoneNumber(Call.Details details) {
//            String phoneNumber = details.getHandle() != null ? details.getHandle().getSchemeSpecificPart() : null;
//            return phoneNumber != null ? PhoneNumberUtils.formatNumber(phoneNumber, Locale.getDefault().getCountry()) : "BLOCKED";
//        }
//    }
//}
