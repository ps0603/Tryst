package com.s_hitappfactory.tryst;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.support.v7.widget.RecyclerView;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.EditText;
import de.hdodenhof.circleimageview.CircleImageView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import android.net.Uri;
import com.google.android.gms.tasks.Task;
import android.support.annotation.NonNull;
import com.bumptech.glide.Glide;
import android.util.Log;
import com.google.firebase.database.FirebaseDatabase;
import com.google.android.gms.tasks.OnCompleteListener;
import android.support.v4.content.ContextCompat;


public class ChatFragment extends Fragment {

    public static final String MESSAGES_CHILD = "messages";
    //private static final int REQUEST_INVITE = 1;
    //private static final int REQUEST_IMAGE = 2;
    //private static final String LOADING_IMAGE_URL = "https://www.google.com/images/spin-32.gif";
    //public static final int DEFAULT_MSG_LENGTH_LIMIT = 10;
    //public static final String ANONYMOUS = "anonymous";
    //private static final String MESSAGE_SENT_EVENT = "message_sent";
    //private static final String MESSAGE_URL = "http://friendlychat.firebase.google.com/message/";

    private Button mSendButton;
    private RecyclerView mMessageRecyclerView;
    private LinearLayoutManager mLinearLayoutManager;
    private ProgressBar mProgressBar;
    private EditText mMessageEditText;
    private ImageView mAddMessageImageView;

    private DatabaseReference mFirebaseDatabaseReference;
    private FirebaseRecyclerAdapter<tMessage, MessageViewHolder> mFirebaseAdapter;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat, container, false);

        // Initialize ProgressBar and RecyclerView.
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);

        mMessageRecyclerView = (RecyclerView) view.findViewById(R.id.messageRecyclerView);
        mLinearLayoutManager = new LinearLayoutManager(getContext());
        mLinearLayoutManager.setStackFromEnd(true);
        //mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);







//TODO
//check library versions
//remove functionality




        // New child entries
        mFirebaseDatabaseReference = FirebaseDatabase.getInstance().getReference();
        mFirebaseAdapter = new FirebaseRecyclerAdapter<tMessage,MessageViewHolder>(
                tMessage.class,
                R.layout.item_message,
                MessageViewHolder.class,
                mFirebaseDatabaseReference.child(MESSAGES_CHILD)) {

            @Override
            protected void populateViewHolder(final MessageViewHolder viewHolder,tMessage tmessage, int position) {
                mProgressBar.setVisibility(ProgressBar.INVISIBLE);
                if (tmessage.getText() != null) {
                    viewHolder.messageTextView.setText(tmessage.getText());
                    viewHolder.messageTextView.setVisibility(TextView.VISIBLE);
                    viewHolder.messageImageView.setVisibility(ImageView.GONE);
                } else {
                    String imageUrl = tmessage.getImageUrl();
                    if (imageUrl.startsWith("gs://")) {
                        StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl);
                        storageReference.getDownloadUrl().addOnCompleteListener(
                                new OnCompleteListener<Uri>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Uri> task) {
                                        if (task.isSuccessful()) {
                                            String downloadUrl = task.getResult().toString();
                                            Glide.with(viewHolder.messageImageView.getContext())
                                                    .load(downloadUrl)
                                                    .into(viewHolder.messageImageView);
                                        } else {
                                            Log.w("Chat Fragment", "Getting download url was not successful.",
                                                    task.getException());
                                        }
                                    }
                                });
                    } else {
                        Glide.with(viewHolder.messageImageView.getContext())
                                .load(tmessage.getImageUrl())
                                .into(viewHolder.messageImageView);
                    }
                    viewHolder.messageImageView.setVisibility(ImageView.VISIBLE);
                    viewHolder.messageTextView.setVisibility(TextView.GONE);
                }


                viewHolder.messengerTextView.setText(tmessage.getName());
                if (tmessage.getPhotoUrl() == null) {
                    viewHolder.messengerImageView.setImageDrawable(ContextCompat.getDrawable(getContext(),
                            R.drawable.ic_account_circle_black_36dp));
                } else {
                    Glide.with(getContext()).load(tmessage.getPhotoUrl()).into(viewHolder.messengerImageView);
                }

            }
        };

        mFirebaseAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int tMessageCount = mFirebaseAdapter.getItemCount();
                int lastVisiblePosition = mLinearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the
                // user is at the bottom of the list, scroll to the bottom
                // of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (tMessageCount - 1) &&
                                lastVisiblePosition == (positionStart - 1))) {
                    mMessageRecyclerView.scrollToPosition(positionStart);
                }
            }
        });

        mMessageRecyclerView.setLayoutManager(mLinearLayoutManager);
        mMessageRecyclerView.setAdapter(mFirebaseAdapter);











        mMessageEditText = (EditText) view.findViewById(R.id.messageEditText);
        mMessageEditText.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(200)
                }
        );
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        mSendButton = (Button) view.findViewById(R.id.sendButton);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Send messages on click.
            }
        });

        mAddMessageImageView = (ImageView) view.findViewById(R.id.addMessageImageView);
        mAddMessageImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Select image for image message on click.
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in.
        // TODO: Add code to check if user is signed in.
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView messageTextView;
        ImageView messageImageView;
        TextView messengerTextView;
        CircleImageView messengerImageView;

        public MessageViewHolder(View v) {
            super(v);
            messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
            messageImageView = (ImageView) itemView.findViewById(R.id.messageImageView);
            messengerTextView = (TextView) itemView.findViewById(R.id.messengerTextView);
            messengerImageView = (CircleImageView) itemView.findViewById(R.id.messengerImageView);
        }
    }

}
