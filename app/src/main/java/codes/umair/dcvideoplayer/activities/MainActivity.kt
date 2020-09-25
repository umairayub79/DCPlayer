package codes.umair.dcvideoplayer.activities

import android.Manifest
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import codes.umair.dcvideoplayer.R
import codes.umair.dcvideoplayer.adapters.FolderAdapter
import com.CodeBoy.MediaFacer.MediaFacer
import com.CodeBoy.MediaFacer.VideoGet
import com.CodeBoy.MediaFacer.mediaHolders.videoFolderContent
import kotlinx.android.synthetic.main.activity_main.*
import pub.devrel.easypermissions.EasyPermissions


class MainActivity : AppCompatActivity() {
    private val mVideoFolders: ArrayList<videoFolderContent> = ArrayList()
    private val mFolderAdapter = FolderAdapter()
    private val mPermissions = Manifest.permission.WRITE_EXTERNAL_STORAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        title = "Folders"

        swipeViewFolders.setOnRefreshListener {
            getFolders()
        }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        mPermissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, mPermissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, mPermissions, grantResults, this)

    }

    private fun getFolders() {
        recyclerViewFolders.adapter = mFolderAdapter
        recyclerViewFolders.layoutManager = LinearLayoutManager(this)

        mVideoFolders.clear()
        mVideoFolders.addAll(
            MediaFacer.withVideoContex(this).getVideoFolders(VideoGet.externalContentUri)
        )
        mFolderAdapter.submitList(mVideoFolders, this)
        mFolderAdapter.notifyDataSetChanged()
        swipeViewFolders.isRefreshing = false
    }


    override fun onResume() {
        super.onResume()
        if (EasyPermissions.hasPermissions(this@MainActivity, mPermissions)) {
            getFolders()
        } else {
            EasyPermissions.requestPermissions(
                this@MainActivity,
                "We need permission to Access Video files",
                123,
                mPermissions
            )
        }
    }
}