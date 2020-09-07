package com.example.Moody.Chat;

import android.app.Activity;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.recyclerview.widget.RecyclerView;

import com.example.Moody.Activity.IntroActivity;
import com.example.Moody.Activity.MainActivity;
import com.example.Moody.Model.ChatModel;

import com.example.Moody.Model.ChatRoomModel;
import com.example.Moody.Model.UserModel;
import com.example.Moody.R;
import com.example.Moody.Sign.SignAddInfoActivity;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.Reference;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends Activity {
    private static final String TAG = "ChatActivity";

    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database = FirebaseDatabase.getInstance();

    public static RecyclerView chatRecyclerView;//채팅 내용 리사이클러뷰
    private PersonalAdapter pAdapter; //1:1 채팅 어뎁터
    private GroupAdapter gAdapter;//단체 채팅 어뎁터

    private ArrayList<ChatModel> chatModels = new ArrayList<ChatModel>();
    private ArrayList<ChatRoomModel> chatRoomModels = new ArrayList<ChatRoomModel>();

    private String receiver; //상대방 id
    private String uid; //사용자 id
    public static String uName; //사용자 이름
    public static String roomid; //채팅방 id
    private String chatRoomName; //상단에 채팅방 이름
    private String check; //단체인지 개인인지 체크

    private String imageUrl; //보낸 사진 url
    private int GET_GALLERY_IMAGE=200;

    private String auto_text;
    public static String emotion;
    public static String sText;

    ChildEventListener childEventListener;
    DatabaseReference reference;

    //작성 시간
    SimpleDateFormat dateFormat1 = new SimpleDateFormat("yyyy-MM-dd");
    String date = dateFormat1.format(Calendar.getInstance().getTime());
    //사진 파일 이름
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddhhmmss");
    String datetime = dateFormat.format(Calendar.getInstance().getTime());


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            getWindow().getDecorView().setSystemUiVisibility( View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    |View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }

        setContentView(R.layout.activity_chattingroom);

        mAuth = FirebaseAuth.getInstance();//현재 로그인 정보
        currentUser = mAuth.getCurrentUser();
        uid = currentUser.getUid();//현재 사용자 id
        UserInfo();//현재 사용자 정보 가져오는 함수

        roomid = getIntent().getStringExtra("roomid");//채팅방 id
        chatRoomName = getIntent().getStringExtra("name"); //채팅방 상단 이름 받아옴

        final TextView recUser = (TextView) findViewById(R.id.chatRoom_users);//채팅방 상단
        final EditText sendText = (EditText) findViewById(R.id.chatRoom_text); //메세지 입력창
        recUser.setText(chatRoomName);//채팅방 상단 이름 설정

        chatRecyclerView = (RecyclerView) findViewById(R.id.chatRoom_recyclerView); //리사이클러뷰
        chatRecyclerView.setHasFixedSize(true); //리사이클러뷰 크기 고정

        check = getIntent().getStringExtra("check");

        ChatDisplay(check);
        if(check.equals("1")){//1:1 채팅
            receiver = getIntent().getStringExtra("receiver"); //상대방 id
            pAdapter = new PersonalAdapter(receiver,roomid,chatModels,chatRoomModels); //개인 채팅방 어뎁터
            chatRecyclerView.setAdapter(pAdapter);

        }else{//단체 채팅
            gAdapter = new GroupAdapter(roomid,chatModels,chatRoomModels); //단체 채팅방 어뎁터
            chatRecyclerView.setAdapter(gAdapter);

        }

        //버튼 선언
        ImageView backBtn = (ImageView) findViewById(R.id.chatRoom_backBtn);
        Button sendBtn = (Button) findViewById(R.id.chatRoom_sendBtn);
        Button galleryBtn = (Button) findViewById(R.id.chatRoom_galleryBtn);
        Button autoBtn = (Button) findViewById(R.id.chatRoom_autoBtn);


        //뒤로가기
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ChatActivity.this,MainActivity.class);
                intent.putExtra("fragment","chat");
                startActivity(intent);
                finish();
            }
        });

        //send버튼 클릭시
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sText = sendText.getText().toString();
                SendMsg(sText);
                sendText.setText(null);

                if(check.equals("1")){
                    chatRecyclerView.scrollToPosition(pAdapter.getItemCount() - 1);
                }else{
                    chatRecyclerView.scrollToPosition(gAdapter.getItemCount() - 1);
                }
            }
        });

        //갤러리 버튼
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                intent.setDataAndType(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                startActivityForResult(intent, GET_GALLERY_IMAGE);

            }
        });


        //이미지추천 버튼
        autoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sText = sendText.getText().toString();
                AutoImage(sendText,sText);

            }


        });


        //예약 전송 버튼
        sendBtn.setOnLongClickListener(new View.OnLongClickListener() {
            final Chronometer chrono = (Chronometer)findViewById(R.id.chrono);
            int hour=0;
            int min=0;
            String resText = null;
            Thread th=null;
            LinearLayout layout = (LinearLayout)findViewById(R.id.late_msg_layout);
            TextView lateMsg = (TextView)findViewById(R.id.late_msg_send);

            //길게 클릭시
            @Override
            public boolean onLongClick(View v) {
                //시간선택 창
                TimePickerDialog.OnTimeSetListener mTimeSetListener = new TimePickerDialog.OnTimeSetListener() {
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Toast toast = Toast.makeText(ChatActivity.this,hourOfDay + "시 " + minute+"분에 메시지가 예약되었습니다.",Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER_HORIZONTAL, Gravity.CENTER, 0);
                        toast.show();

                        hour =hourOfDay;
                        min =minute;
                        resText = sendText.getText().toString();
                        sendText.setText(null);

                        if(th==null) {
                            newThread();
                            th.start();
                        }
                        else{
                            th.interrupt();
                            th=null;
                            newThread();
                            th.start();
                        }

                    }
                };
                TimePickerDialog oDialog = new TimePickerDialog(ChatActivity.this,
                        android.R.style.Theme_DeviceDefault_Light_Dialog,
                        mTimeSetListener, 0, 0, false);
                oDialog.show();
                return true;
            }

            //스레드
            private void newThread() {
                chrono.setBase(SystemClock.elapsedRealtime());
                th = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        long mNow = System.currentTimeMillis();
                        Date mReDate = new Date(mNow);
                        SimpleDateFormat mFormat = new SimpleDateFormat("HHmm");
                        String formatDate = mFormat.format(mReDate);

                        int settime = (hour * 100 + min) - Integer.parseInt(formatDate);

                        chrono.start();
                        System.out.println("send:" + resText);
                        //예약 문자 레이아웃 띄우기
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                layout.setVisibility(View.VISIBLE);
                                lateMsg.setText(resText);
                            }
                        });

                        while (true) {
                            long elapsedMillis = SystemClock.elapsedRealtime() - chrono.getBase();
                            if (elapsedMillis == settime * 60000) {
                                if(!resText.equals("")){
                                    SendMsg(resText);
                                    sendText.setText(null);
                                    chrono.stop();

                                    break;
                                }

                            }
                        }

                        //레이아웃 지우기기
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                layout.setVisibility(View.GONE);
                            }
                        });
                    }


                });
            }


        });


    }

    //채팅 보내기
    public void SendMsg(String sendMsg){
        if (!(sendMsg.equals(""))) {
            DatabaseReference ref = database.getReference("Message").child(roomid);

            Map<String,Object> read = new HashMap<>();
            read.put(currentUser.getUid(),true); //메시지 보낸사람은 바로 읽음

            HashMap<String, Object> member = new HashMap<String, Object>();
            member.put("uID", currentUser.getUid()); //보낸사람 id
            member.put("userName", uName); //보낸 사람 이름
            member.put("msg", sendMsg); //보낸 메시지
            member.put("timestamp", ServerValue.TIMESTAMP); //보낸 시간
            member.put("msgType", "0"); //메시지 타입
            member.put("readUsers",read); //읽음 여부
            ref.push().setValue(member);

            auto_text = sendMsg;

            HashMap<String, Object> chatroom = new HashMap<String, Object>();
            chatroom.put("lastMsg",sendMsg);//마지막 메시지
            chatroom.put("lastTime",ServerValue.TIMESTAMP); //마지막 시간
            database.getReference("ChatRoom").child(roomid).updateChildren(chatroom);
        }
    }

    //채팅내용 불러오기
    public void ChatDisplay(final String check){

        database.getReference("ChatRoom").child(roomid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                ChatRoomModel chatRoomModel = dataSnapshot.getValue(ChatRoomModel.class);
                chatRoomModels.add(chatRoomModel);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });

        database.getReference("Message").child(roomid).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                chatModels.clear();
                for(DataSnapshot dataSnapshot: snapshot.getChildren()){
                    ChatModel chat = dataSnapshot.getValue(ChatModel.class);
                    String commentKey = dataSnapshot.getKey();

                    //읽었는지
                    Map<String,Object> read = new HashMap<>();
                    read.put(currentUser.getUid(),true);
                    database.getReference("Message").child(roomid).child(commentKey).child("readUsers").updateChildren(read);

                    chatModels.add(chat);

                    if (check.equals("1")) {
                        pAdapter.notifyDataSetChanged();
                    } else {
                        gAdapter.notifyDataSetChanged();
                    }
                    chatRecyclerView.scrollToPosition(chatModels.size()-1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

    }


    //갤러리 함수
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GET_GALLERY_IMAGE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri selectedImageUri = data.getData(); //이미지 경로 원본
            System.out.println("이미지:"+selectedImageUri);
            String imagePath = "Chat/"+roomid+"/"+datetime; //사진파일 경로 및 이름
            UploadFiles(selectedImageUri,imagePath); //사진 업로드

        }

    }

    //파이어베이스에 사진 업로드
    public void UploadFiles(Uri uri, final String path) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        final StorageReference storageRef = storage.getReference();

        final StorageReference riversRef = storageRef.child(path);
        UploadTask uploadTask = riversRef.putFile(uri);
        Task<Uri> urlTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return riversRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    imageUrl = downloadUri.toString();
                    //DB에 저장
                    DatabaseReference ref = database.getReference("Message").child(roomid);
                    Map<String,Object> read = new HashMap<>();
                    read.put(currentUser.getUid(),true);
                    HashMap<String, Object> member = new HashMap<String, Object>();
                    member.put("uID", currentUser.getUid()); //보낸사람 id
                    member.put("userName", uName); //보낸 사람 이름
                    member.put("msg", imageUrl); //url
                    member.put("timestamp",ServerValue.TIMESTAMP); //작성 시간
                    member.put("msgType","1"); //메세지 타입
                    member.put("readUsers",read);
                    ref.push().setValue(member); //DB에 저장

                    HashMap<String, Object> chatroom = new HashMap<String, Object>();
                    chatroom.put("lastMsg","사진"); //사진일때
                    chatroom.put("lastTime",ServerValue.TIMESTAMP); //마지막 시간
                    database.getReference("ChatRoom").child(roomid).updateChildren(chatroom);
                }
            }
        });


    }

    //사용자 정보 불러오기
    public void UserInfo(){
        database.getReference("userInfo").child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                UserModel um = dataSnapshot.getValue(UserModel.class);
                uName = um.getName();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }



    //===================================================================================================================

    //뒤로 가기 버튼 클릭시
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //database.getReference("Message").child(roomid).removeEventListener(childEventListener);
        Intent intent = new Intent(ChatActivity.this,MainActivity.class);
        intent.putExtra("fragment","chat");
        startActivity(intent);
        finish();
    }

    //키보드 내리기
    public boolean onTouchEvent(MotionEvent event) {
        EditText sendText = (EditText)findViewById(R.id.chatRoom_text);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(sendText.getWindowToken(), 0);
        return true;
    }

    //자동이미지 추천
    public void AutoImage(TextView sendText,String sText){

        if(IntroActivity.word_set==null){
            IntroActivity.word_set=Word();
        }

        String input_text;
        System.out.println(auto_text);

        input_text = auto_text;

        if (!(sendText.getText().toString().equals(""))) {
            input_text = sendText.getText().toString();
        }
        System.out.println("input_text: "+ input_text);
        if (input_text != null) {

            int[] num = new int[50];
            int cnt = 0;

            String[] word = input_text.split(" ");

            for (int i = 0; i < word.length; i++) {
                if (IntroActivity.word_set.containsValue(word[i])) {
                    Integer key = IntroActivity.getKey(IntroActivity.word_set, word[i]);
                    System.out.println(key);
                    num[cnt] = key.intValue();
                    cnt++;
                    System.out.println(word[i] + " " + key);
                } else {
                    int temp = word[i].length();
                    int temp2 = temp - 1;
                    int check = 0;

                    for (int j = 0; j < word[i].length(); j++) {
                        String str = word[i].substring(0, temp);
                        System.out.println("str " + str);
                        for (Map.Entry<Integer, String> entry : IntroActivity.word_set.entrySet()) {
                            if (entry.getValue().contains(str)) {
                                String tstr = entry.getValue();
                                String[] arr_word = tstr.split("");
                                String[] arr_str = str.split("");
                                if (arr_word[0].equals(arr_str[0])) {
                                    System.out.println("1> " + entry.getValue());
                                    Integer key = entry.getKey();
                                    System.out.println("2> " + key);
                                    num[cnt] = key.intValue();
                                    check = cnt;
                                    cnt++;
                                    break;
                                }
                            }

                        }
                        temp--;
                        if (check != 0) break;
                    }
                    if (check == 0) {
                        for (int j = 0; j < word[i].length(); j++) {
                            String str2 = word[i].substring(1, temp2);
                            for (Map.Entry<Integer, String> entry : IntroActivity.word_set.entrySet()) {
                                if (entry.getValue().contains(str2)) {
                                    System.out.println("1> " + entry.getValue());
                                    Integer key = entry.getKey();
                                    System.out.println("2> " + key);
                                    num[cnt] = key.intValue();
                                    check = cnt;
                                    cnt++;
                                    break;
                                }
                            }

                            if (check != 0) break;
                        }
                        temp2--;
                        if (check != 0) break;
                    }
                }
            }

            System.out.println(cnt);

            float[][] input = new float[1][8];

            int[] index = new int[cnt];
            for (int i = 0; i < cnt; i++) {
                index[i] = num[i];
                System.out.print("index" + index[i] + " ");
            }

            if (cnt > 7) {
                Arrays.sort(index);

                for (int j = 0; j < 8; j++) {
                    input[0][j] = (float) index[j];
                    System.out.print(index[j] + " ");
                }
            } else {
                int[] arr = new int[8];

                int j, k = 0;
                for (j = 0; j < 8; j++) {
                    if (j < (8 - cnt))
                        arr[j] = 0;
                    else arr[j] = index[k++];
                    System.out.print(arr[j] + " ");
                }

                System.out.println();
                System.out.println("here");

                for (int i = 0; i < 8; i++) {
                    System.out.print(arr[i] + " ");
                    input[0][i] = (float) arr[i];
                }
            }

            float[][] output = new float[1][6];

            Interpreter tflite = getTfliteInterpreter("new_lstm_model.tflite");
            tflite.run(input, output);

            String result = String.valueOf(output[0]);

            int maxIdx = 0;
            float maxProb = output[0][0];
            for (int i = 1; i < 6; i++) {
                if (output[0][i] > maxProb) {
                    maxProb = output[0][i];
                    maxIdx = i;
                    System.out.println("확률: " + maxIdx + maxProb);

                }
            }
            System.out.println(maxIdx);
            emotion = null;
            if (maxIdx == 0)
                emotion = "happy";
            else if (maxIdx == 1)
                emotion = "sad";
            else if (maxIdx == 2)
                emotion = "angry";
            else if (maxIdx == 3)
                emotion = "surprise";
            else if (maxIdx == 4)
                emotion = "fear";
            else if (maxIdx == 5)
                emotion = "disgust";

            System.out.println("감정: " + emotion);
            sendText.setText(null);


            Intent intent = new Intent(ChatActivity.this, AutoChatActivity.class);
            intent.putExtra("name", chatRoomName);
            intent.putExtra("receiver", receiver);
            intent.putExtra("roomid", roomid);
            intent.putExtra("check",check);

            startActivity(intent);
        }

    }

    private Interpreter getTfliteInterpreter(String modelPath) {
        try {
            return new Interpreter(loadModelFile(ChatActivity.this, modelPath));
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    // 모델을 읽어오는 함수
    // MappedByteBuffer 바이트 버퍼를 Interpreter 객체에 전달하면 모델 해석을 할 수 있다.
    private MappedByteBuffer loadModelFile(Activity activity, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private HashMap<Integer, String> Word() {

        HashMap<Integer, String> word_set = new HashMap<Integer, String>();

        try {
            InputStream inputStream = getResources().openRawResource(R.raw.wordset);
            InputStreamReader reader = new InputStreamReader(inputStream);
            // 입력 버퍼 생성
            BufferedReader bufReader = new BufferedReader(reader);
            String line = "";

            int i=0;
            while ((line = bufReader.readLine()) != null) {
                System.out.println(line);

                String[] word = line.split(":");
                word_set.put(Integer.parseInt(word[1]), word[0]);
                i++;
            }
            bufReader.close();
        } catch (IOException e) {
            System.out.println(e);
        }

        return word_set;
    }

}