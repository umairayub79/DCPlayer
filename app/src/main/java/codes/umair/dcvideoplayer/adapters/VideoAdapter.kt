package codes.umair.dcvideoplayer.adapters


import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.media.MediaScannerConnection
import android.media.MediaScannerConnection.OnScanCompletedListener
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import codes.umair.dcvideoplayer.R
import codes.umair.dcvideoplayer.activities.PlayerActivity
import codes.umair.dcvideoplayer.activities.VideoListActivity
import codes.umair.dcvideoplayer.myInterface
import com.CodeBoy.MediaFacer.mediaDataCalculator
import com.CodeBoy.MediaFacer.mediaHolders.videoContent
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.item_video.view.*
import umairayub.madialog.MaDialog
import java.io.File
import java.io.IOException


class VideoAdapter : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    private var items: ArrayList<videoContent> = ArrayList()
    private lateinit var mActivity: Activity
    private lateinit var mInterface: myInterface

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_video, parent, false)
        )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = items[position]

        holder.mVideoName.text = video.videoName
        holder.mVideoDuration.text = mediaDataCalculator.milliSecondsToTimer(video.videoDuration)
        Glide.with(holder.ctx)
            .load(video.assetFileStringUri)
            .into(holder.mVideoThumb)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.ctx, PlayerActivity::class.java)
            intent.putExtra("videoPath", video.assetFileStringUri)
            intent.putExtra("videoName", video.videoName)
            holder.ctx.startActivity(intent)
        }
        holder.mVideoOptions.setOnClickListener{ view ->
            val popupMenu = PopupMenu(holder.ctx,view)
            popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_delete_item -> {
                        deleteFile(holder.ctx,video)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_share_item -> {
                        shareFile(holder.ctx,video)
                        return@setOnMenuItemClickListener true
                    }
                    R.id.action_properties_item -> {
                        showFileProperties(holder.ctx,video)
                        return@setOnMenuItemClickListener true
                    }
                    else -> {
                        return@setOnMenuItemClickListener false
                    }
                }

            }
            popupMenu.show()
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitList(videos: ArrayList<videoContent>) {
        items = videos
        if(items.isEmpty()){
            mActivity.finish()
        }
    }
    fun init(myInterface: myInterface, activity: Activity) {
        mInterface = myInterface
        mActivity = activity
    }

    private fun deleteFile(ctx: Context,videoFile : videoContent){
        val file = File(videoFile.path)
        if (file.exists()) {
            MaDialog.Builder(ctx)
                .setTitle("Delete?")
                .setTitleTextColor(Color.RED)
                .setMessage("Are you sure you want to delete ${videoFile.videoName} ?")
                .setPositiveButtonText("Yes")
                .setNegativeButtonText("Cancel")
                .setPositiveButtonListener {
                    items.remove(videoFile)
                    val deleted = file.delete()
                    notifyDataSetChanged()
                    Toast.makeText(ctx,"File deleted: $deleted",Toast.LENGTH_SHORT).show()
                    scanFile(ctx,file)
                    if(items.isEmpty()){
                        mActivity.finish()
                    }
                }
                .setNegativeButtonListener {}
                .build()

        }else{
            items.remove(videoFile)
            notifyDataSetChanged()
            scanFile(ctx,file)
        }
    }
    private fun shareFile(ctx: Context,videoFile : videoContent){
        val file = File(videoFile.path)
        try {
            val contentUri: Uri = FileProvider.getUriForFile(ctx,"codes.umair.dcvideoplayer.fileprovider",file)
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "video/*"
            intent.putExtra(Intent.EXTRA_STREAM, contentUri)
            ctx.startActivity(Intent.createChooser(intent, "Share via "))
        } catch (e: IOException) {
            throw RuntimeException("Error generating file", e)
        }

    }

    private fun showFileProperties(ctx: Context,videoFile : videoContent){
//        val file = File(videoFile.path)
        MaDialog.Builder(ctx)
            .setTitle("Details")
            .setMessage("Name: ${videoFile.videoName} \nLocation: ${videoFile.path} \nSize: ${mediaDataCalculator.convertBytes(videoFile.videoSize)} \nDuration: ${mediaDataCalculator.convertDuration(videoFile.videoDuration)} \n Date modified: ${mediaDataCalculator.milliSecondsToTimer(videoFile.date_modified)}")
            .setPositiveButtonText("OK")
            .setPositiveButtonListener {  }
            .build()

    }
    private fun scanFile(ctx: Context, file: File){
        MediaScannerConnection.scanFile(ctx,
            arrayOf(file.toString()),
            null,
            OnScanCompletedListener { path, uri ->
                Log.i("ExternalStorage", "Scanned $path:")
                Log.i("ExternalStorage", "-> uri=$uri")
            })
    }

//    private fun renameFile(ctx: Context,videoFile : videoContent){
//        val file = File(videoFile.path)
//        val fileName = file.nameWithoutExtension
//        val fileExtension = file.extension
//        val parentDir = videoFile.path.substringBefore(fileName)
//        val renameDialog = AlertDialog.Builder(ctx,R.style.Theme_AppCompat_Light_Dialog_Alert)
//        val edtName = EditText(ctx)
//        edtName.setText(fileName)
//        edtName.selectAll()
//        renameDialog.setTitle("Rename to")
//        renameDialog.setView(edtName)
//
//        renameDialog.setPositiveButton("Done", DialogInterface.OnClickListener { dialog, which ->
//            val newName = edtName.text.trim().toString()
//            val newFile = File("$parentDir$newName.$fileExtension")
//            if(file.exists()){
//                val renamed = file.renameTo(newFile)
//                items.remove(videoFile)
//                notifyDataSetChanged()
//                scanFile(ctx,newFile)
//                scanFile(ctx,file)
//                Toast.makeText(ctx, "File Renamed: $renamed", Toast.LENGTH_SHORT).show()
//                mInterface.updateVideos()
//
//            }else{
//                items.remove(videoFile)
//                scanFile(ctx,file)
//            }
//            mInterface.updateVideos()
//
//        })
//        renameDialog.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which ->
//            dialog.dismiss()
//        })
//        renameDialog.show()
//    }

    class VideoViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mVideoName: TextView = itemView.textView_videoName
        val mVideoDuration: TextView = itemView.textView_videoDuration
        val mVideoThumb: ImageView = itemView.imageView_videoThumb
        val mVideoOptions: Button = itemView.btn_options
        val ctx: Context = itemView.context

    }


}