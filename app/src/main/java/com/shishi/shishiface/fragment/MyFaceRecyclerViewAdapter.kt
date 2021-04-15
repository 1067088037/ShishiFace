package com.shishi.shishiface.fragment

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.shishi.shishiface.Config
import com.shishi.shishiface.R


import com.shishi.shishiface.fragment.FaceListFragment.OnListFragmentInteractionListener
import com.shishi.shishiface.fragment.RecyclerList.FaceItem

import kotlinx.android.synthetic.main.fragment_face.view.*

/**
 * [RecyclerView.Adapter] that can display a [FaceItem] and makes a call to the
 * specified [OnListFragmentInteractionListener].
 */
class MyFaceRecyclerViewAdapter(
    var mValues: List<FaceItem>,
    private val mListener: OnListFragmentInteractionListener?
) : RecyclerView.Adapter<MyFaceRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener
    private val mOnLongClickListener: View.OnLongClickListener

    init {
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as FaceItem
            mListener?.onListFragmentInteraction(item, OnListFragmentInteractionListener.Act.Click)
        }
        mOnLongClickListener = View.OnLongClickListener { v ->
            val item = v.tag as FaceItem
            mListener?.onListFragmentInteraction(item, OnListFragmentInteractionListener.Act.LongClick)
            true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.fragment_face, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = mValues[position]
        holder.mCheckBox.isChecked = item.checked
        holder.mIdView.text = item.sid.toString()
        val name = item.name
        holder.mNameView.text = name
        val textPAndG =
            "${if (item.isAdmin) "[${if (item.permission == Config.SYSTEM_ADMINISTRATOR) "S" else "A"}]" else ""} ${Config.genderToText(
                item.gender
            )}"
        holder.mPermissionGenderView.text = textPAndG

        holder.mCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (position >= RecyclerList.faceItems.size) return@setOnCheckedChangeListener
            RecyclerList.faceItems[position].checked = isChecked
        }

        with(holder.mView) {
            tag = item
            setOnClickListener(mOnClickListener)
            setOnLongClickListener(mOnLongClickListener)
        }
    }

    fun insert(faceItem: FaceItem) {
        RecyclerList.faceItems.add(faceItem)
        mValues = RecyclerList.faceItems
        notifyItemInserted(RecyclerList.faceItems.size - 1)
    }

    fun remove(position: Int) {
        RecyclerList.faceItems.removeAt(position)
        mValues = RecyclerList.faceItems
        notifyItemRemoved(position)
    }

    fun change(position: Int) {
        mValues = RecyclerList.faceItems
        notifyItemChanged(position)
    }

    fun changeWhereToEnd(start: Int) {
        mValues = RecyclerList.faceItems
        notifyItemRangeChanged(start, RecyclerList.faceItems.size - start, "ShishiFace")
    }

    fun changeAll() {
        mValues = RecyclerList.faceItems
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = mValues.size

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mCheckBox: CheckBox = mView.ml_checkBox
        val mIdView: TextView = mView.ml_sid
        val mNameView: TextView = mView.ml_name
        val mPermissionGenderView: TextView = mView.ml_permission_gender

        override fun toString(): String {
            return super.toString() + mNameView.text
        }
    }

}