package codes.umair.dcvideoplayer.Utils

import android.content.*
import android.content.Context.CLIPBOARD_SERVICE
import android.database.Cursor
import android.net.ConnectivityManager
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import codes.umair.dcvideoplayer.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class Utils {

    fun shareText(text: String, ctx: Context) {
        val shareIntent = Intent()
        shareIntent.action = Intent.ACTION_SEND
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, text)
        ctx.startActivity(Intent.createChooser(shareIntent, "Share"))
    }

    fun copyToClipboard(text: String, ctx: Context) {
        val myClipboard: ClipboardManager? =
            ctx.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?
        val myClip = ClipData.newPlainText("text", text)
        myClipboard?.setPrimaryClip(myClip)
    }


    fun toastLong(text: String, ctx: Context) {
        Toast.makeText(ctx, text, Toast.LENGTH_LONG).show()
    }

    fun toastShort(text: String, ctx: Context) {
        Toast.makeText(ctx, text, Toast.LENGTH_SHORT).show()
    }

//    fun showSnackBar(parent: View, text: String){
//        val snackbar = Snackbar.make(
//            parent,
//            text,
//            Snackbar.LENGTH_LONG
//        )
//        snackbar.show()
//    }


    fun getDurationString(durationMs: Long, negativePrefix: Boolean): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs)
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs)

        return if (hours > 0) {
            java.lang.String.format(
                Locale.getDefault(), "%s%02d:%02d:%02d",
                if (negativePrefix) "-" else "",
                hours,
                minutes - TimeUnit.HOURS.toMinutes(hours),
                seconds - TimeUnit.MINUTES.toSeconds(minutes)
            )
        } else java.lang.String.format(
            Locale.getDefault(), "%s%02d:%02d",
            if (negativePrefix) "-" else "",
            minutes,
            seconds - TimeUnit.MINUTES.toSeconds(minutes)
        )
    }

    fun formatDate(dateInLong: Long): String {
        val formatter: DateFormat = SimpleDateFormat("dd/MM/yyyy")
        val mCalender: Calendar = Calendar.getInstance()
        mCalender.timeInMillis = dateInLong
        return formatter.format(mCalender.time)
    }


    fun rateUs(ctx: Context) {
        try {
            ctx.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + ctx.packageName)
                )
            );
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace();
            toastShort(e.localizedMessage, ctx)
            ctx.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + ctx.packageName)
                )
            );
        }
    }

    fun sendFeedBack(email: String, ctx: Context) {
        val emailIntent = Intent(Intent.ACTION_SENDTO)
        emailIntent.data = Uri.parse("mailto:$email")
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.app_name) + " Feedback")
        ctx.startActivity(Intent.createChooser(emailIntent, "Send Feedback!"))
    }

    fun isNetworkAvailable(ctx: Context): Boolean {
        val connectivityManager =
            ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    fun getFileName(ctx: Context,uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            val cursor: Cursor? = ctx.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME))
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result!!.lastIndexOf('/')
            if (cut != -1) {
                result = result.substring(cut + 1)
            }
        }
        return result
    }

    enum class ControlsMode {
        LOCK,
        FULLCONTORLS
    }

    enum class GestureType {
        NoGesture,
        SwipeGesture
    }

    enum class PlaybackState {
        PLAYING,
        PAUSED,
        BUFFERING,
        IDLE
    }
}