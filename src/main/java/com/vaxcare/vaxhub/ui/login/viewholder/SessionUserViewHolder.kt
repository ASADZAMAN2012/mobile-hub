/**************************************************************************************************
 * Copyright VaxCare (c) 2023.                                                                    *
 **************************************************************************************************/

package com.vaxcare.vaxhub.ui.login.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.vaxcare.vaxhub.core.extension.setOnSingleClickListener
import com.vaxcare.vaxhub.databinding.RvSessionUserItemBinding
import com.vaxcare.vaxhub.model.user.SessionUser
import com.vaxcare.vaxhub.ui.login.adapter.SessionUserClickListener

class SessionUserViewHolder(
    val binding: RvSessionUserItemBinding,
    private val listener: SessionUserClickListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(user: SessionUser) {
        val displayName = "${user.firstName} ${user.lastName}"
        with(binding) {
            userNameLabel.text = displayName
            root.setOnSingleClickListener { listener.onSessionUserClicked(user.username) }
        }
    }
}
