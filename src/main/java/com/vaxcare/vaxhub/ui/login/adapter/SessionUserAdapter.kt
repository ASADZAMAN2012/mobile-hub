/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login.adapter

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.vaxcare.vaxhub.core.extension.getInflater
import com.vaxcare.vaxhub.databinding.RvSessionUserItemBinding
import com.vaxcare.vaxhub.model.user.SessionUser
import com.vaxcare.vaxhub.ui.login.viewholder.SessionUserViewHolder

class SessionUserAdapter(private val listener: SessionUserClickListener) :
    ListAdapter<SessionUser, SessionUserViewHolder>(ItemCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionUserViewHolder {
        return SessionUserViewHolder(
            binding = RvSessionUserItemBinding.inflate(parent.getInflater(), parent, false),
            listener = listener
        )
    }

    override fun onBindViewHolder(holder: SessionUserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

private object ItemCallback : DiffUtil.ItemCallback<SessionUser>() {
    override fun areItemsTheSame(oldItem: SessionUser, newItem: SessionUser): Boolean {
        return oldItem.userId == newItem.userId
    }

    override fun areContentsTheSame(oldItem: SessionUser, newItem: SessionUser): Boolean {
        return oldItem.userId == newItem.userId &&
            oldItem.isLocked == newItem.isLocked
    }
}
