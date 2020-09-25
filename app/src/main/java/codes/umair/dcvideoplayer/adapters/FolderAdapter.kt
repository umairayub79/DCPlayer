package codes.umair.dcvideoplayer.adapters


import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import codes.umair.dcvideoplayer.R
import codes.umair.dcvideoplayer.activities.VideoListActivity
import com.CodeBoy.MediaFacer.MediaFacer
import com.CodeBoy.MediaFacer.mediaHolders.videoFolderContent
import kotlinx.android.synthetic.main.item_folder.view.*


class FolderAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var items: ArrayList<videoFolderContent> = ArrayList()
    private lateinit var mActivity: Activity
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return CategoryViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_folder, parent, false)
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CategoryViewHolder -> {
                holder.bind(items[position])

            }

        }
    }

    override fun getItemCount(): Int {
        return items.size
    }

    fun submitList(folders: ArrayList<videoFolderContent>, activity: Activity) {
        items = folders
        mActivity = activity
    }

    class CategoryViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mFolderName: TextView = itemView.textView_folderName
        private val mVideoNum: TextView = itemView.textView_videoNum
        private val ctx = itemView.context

        fun bind(folder: videoFolderContent) {
            mFolderName.text = folder.folderName.capitalize()
            val allVideos = MediaFacer.withVideoContex(ctx)
                .getAllVideoContentByBucket_id(folder.bucket_id)
            mVideoNum.text = allVideos.size.toString() + " Local Videos"
            itemView.setOnClickListener {
                val intent = Intent(ctx, VideoListActivity::class.java)
                intent.putExtra("bucketID", folder.bucket_id)
                intent.putExtra("folderName", folder.folderName)
                ctx.startActivity(intent)
            }
        }


    }

}