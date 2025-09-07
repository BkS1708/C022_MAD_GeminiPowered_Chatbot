package com.bhavya.C022_BhavyaSanghrajka;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bhavya.C022_BhavyaSanghrajka.R;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private final List<Message> messages;

    public MessageAdapter(List<Message> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView messageText;
        private final LinearLayout container;

        public MessageViewHolder(View itemView) {
            super(itemView);
            messageText = itemView.findViewById(R.id.messageText);
            container = itemView.findViewById(R.id.messageContainer);
        }

        public void bind(Message message) {
            messageText.setText(TextFormatter.getBoldSpannableText(message.getText()));

            // Align left for AI, right for user
            if (message.isUser()) {
                messageText.setBackgroundResource(R.drawable.bg_user_message);
                messageText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
            } else {
                messageText.setBackgroundResource(R.drawable.bg_ai_message);
                messageText.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
        }



    }
}
