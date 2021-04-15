package com.shishi.shishiface.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import androidx.recyclerview.widget.RecyclerView
import com.shishi.shishiface.R
import com.shishi.shishiface.faceserver.DataBase

import com.shishi.shishiface.fragment.RecyclerList.FaceItem
import jp.wasabeef.recyclerview.adapters.SlideInLeftAnimationAdapter

class FaceListFragment : androidx.fragment.app.Fragment() {

    private var columnCount = 1
    private var listener: OnListFragmentInteractionListener? = null
    private val db: DataBase
        get() = DataBase.localDataBase!!
    lateinit var myAdapter: MyFaceRecyclerViewAdapter
    lateinit var recyclerView: androidx.recyclerview.widget.RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            columnCount = it.getInt(ARG_COLUMN_COUNT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_face_list, container, false)

        if (view is RecyclerView) {
            with(view) {
                recyclerView = this
                layoutManager = when {
                    columnCount <= 1 -> androidx.recyclerview.widget.LinearLayoutManager(context)
                    else -> androidx.recyclerview.widget.GridLayoutManager(context, columnCount)
                }
                updateFromDB()
                myAdapter = MyFaceRecyclerViewAdapter(RecyclerList.faceItems, listener)
                val alphaAdapter = SlideInLeftAnimationAdapter(myAdapter)
                alphaAdapter.setDuration(750)
                alphaAdapter.setInterpolator(OvershootInterpolator())
                alphaAdapter.setFirstOnly(false)
                adapter = alphaAdapter
            }
        }
        return view
    }

    fun updateFromDB(): ArrayList<FaceItem> {
        RecyclerList.release()
        val data = db.getFace()
        for (i in data.indices) {
            val faceInfo = data[i]
            RecyclerList.addItem(faceInfo.id, faceInfo.sid, faceInfo.name, faceInfo.gender, faceInfo.permission)
        }
        RecyclerList.faceItems.sortWith(compareBy({ -it.permission }, { it.sid }))//排序
        return RecyclerList.faceItems
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: FaceItem?, act: Act)
        enum class Act {
            Click, LongClick
        }
    }

    companion object {

        const val ARG_COLUMN_COUNT = "column-count"

        @JvmStatic
        fun newInstance(columnCount: Int) =
            FaceListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_COLUMN_COUNT, columnCount)
                }
            }
    }
}
