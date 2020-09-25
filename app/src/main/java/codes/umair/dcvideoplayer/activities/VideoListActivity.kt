package codes.umair.dcvideoplayer.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import codes.umair.dcvideoplayer.R
import codes.umair.dcvideoplayer.adapters.VideoAdapter
import codes.umair.dcvideoplayer.myInterface
import com.CodeBoy.MediaFacer.MediaFacer
import com.CodeBoy.MediaFacer.mediaHolders.videoContent
import kotlinx.android.synthetic.main.activity_video_list.*

class VideoListActivity : AppCompatActivity(), myInterface {

    private val mVideoAdapter = VideoAdapter()
    private val mAllVideos: ArrayList<videoContent> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_list)
        actionBar?.setDisplayHomeAsUpEnabled(true)
        title = intent.getStringExtra("folderName")


        mVideoAdapter.init(this,this)
        swipeViewVideos.setOnRefreshListener {
            getVideos()
        }
        getVideos()


    }

    private fun getVideos() {
        mAllVideos.clear()
        recyclerViewVideos.layoutManager = LinearLayoutManager(this)
        recyclerViewVideos.adapter = mVideoAdapter

        mAllVideos.addAll(
            MediaFacer.withVideoContex(this)
                .getAllVideoContentByBucket_id(intent.getIntExtra("bucketID", 0))
        )
        mVideoAdapter.submitList(mAllVideos)
        mVideoAdapter.notifyDataSetChanged()
        swipeViewVideos.isRefreshing = false
    }

    override fun onResume() {
        super.onResume()
        getVideos()
    }

    override fun updateVideos() {
//        getVideos()
    }

    override fun updateFolders() {
//        TODO("Not yet implemented")
    }
}