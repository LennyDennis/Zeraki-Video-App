package com.lennydennis.zerakiapp.ui.adapters;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lennydennis.zerakiapp.databinding.ParticipantViewBinding;
import com.twilio.video.RemoteParticipant;

public class ThumbnailParticipantsAdapter {

    public class ViewHolder extends RecyclerView.ViewHolder{

        private ParticipantViewBinding mParticipantViewBinding;

        public ViewHolder(@NonNull ParticipantViewBinding participantViewBinding) {
            super(participantViewBinding.getRoot());
            this.mParticipantViewBinding = participantViewBinding;
        }

//        public void bindView(LearningHoursLeaders learningHoursLeader){
//            mLearningItemBinding.setLearningLeaders(learningHoursLeader);
//            mLearningItemBinding.executePendingBindings();
//        }
    }
}
