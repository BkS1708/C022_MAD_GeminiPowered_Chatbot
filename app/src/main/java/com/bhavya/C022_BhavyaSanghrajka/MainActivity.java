package com.bhavya.C022_BhavyaSanghrajka;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.type.GenerateContentResponse;

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.widget.Toast;
import java.util.Locale;

import com.google.android.material.appbar.MaterialToolbar;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class MainActivity extends AppCompatActivity {

    private EditText promptEditText;
    private ProgressBar progressBar;
    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private final List<Message> messages = new ArrayList<>();
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        promptEditText = findViewById(R.id.promptEditText);
        ImageButton submitPromptButton = findViewById(R.id.sendButton);
        progressBar = findViewById(R.id.progressBar);

        MaterialToolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        recyclerView = findViewById(R.id.recyclerView);
        adapter = new MessageAdapter(messages);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        // ✅ Load chat history from Room
        db = AppDatabase.getDatabase(this);
        List<Message> chatHistory = db.messageDao().getAllMessages();
        messages.addAll(chatHistory);
        adapter.notifyDataSetChanged();

        GenerativeModel generativeModel = new GenerativeModel("gemini-2.0-flash", BuildConfig.API_KEY);

        ImageButton micButton = findViewById(R.id.micButton);

        micButton.setOnClickListener(v -> {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                Toast.makeText(this, "Speech recognition not available", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

            startActivityForResult(intent, 1001);
        });


        submitPromptButton.setOnClickListener(v -> {
            String prompt = promptEditText.getText().toString().trim();
            promptEditText.setError(null);
            if (prompt.isEmpty()) {
                promptEditText.setError(getString(R.string.field_cannot_be_empty));
                return;
            }

            // Add user message
            //messages.add(new Message(prompt, true));
            Message userMsg = new Message(prompt, true);
            messages.add(userMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            recyclerView.scrollToPosition(messages.size() - 1);

            // Add fake typing indicator
            Message typingMsg = new Message("Gemini is typing...", false, true);
            messages.add(typingMsg);
            adapter.notifyItemInserted(messages.size() - 1);
            recyclerView.scrollToPosition(messages.size() - 1);

            // ✅ Save to Room
            db.messageDao().insertMessage(userMsg);

            promptEditText.setText("");
            progressBar.setVisibility(VISIBLE);

            generativeModel.generateContent(prompt, new Continuation<>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {
                    GenerateContentResponse response = (GenerateContentResponse) o;
                    String responseString = response.getText();
                    if (responseString == null) {
                        responseString = "⚠️ No response received from AI.";
                    }
                    Log.d("Response", responseString);

                    String finalResponse = responseString;
                    runOnUiThread(() -> {
                        progressBar.setVisibility(GONE);
                        //messages.add(new Message(finalResponse, false));

                        // Remove typing indicator
                        int lastIndex = messages.size() - 1;
                        Message lastMsg = messages.get(lastIndex);
                        if (lastMsg.isTyping()) {
                            messages.remove(lastIndex);
                            adapter.notifyItemRemoved(lastIndex);
                        }

                        Message botMsg = new Message(finalResponse, false);
                        messages.add(botMsg);
                        adapter.notifyItemInserted(messages.size() - 1);
                        recyclerView.scrollToPosition(messages.size() - 1);

                        // ✅ Save bot reply to Room
                        db.messageDao().insertMessage(botMsg);
                    });

                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem item = menu.findItem(R.id.action_toggle_theme);
        View actionView = item.getActionView();
        SwitchCompat switchTheme = actionView.findViewById(R.id.switchTheme);

        // Restore preference
        boolean isDarkMode = getSharedPreferences("settings", MODE_PRIVATE).getBoolean("darkMode", false);
        switchTheme.setChecked(isDarkMode);
        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );

        // Toggle listener
        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
            getSharedPreferences("settings", MODE_PRIVATE).edit().putBoolean("darkMode", isChecked).apply();
        });

        return true;
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<String> texts = new ArrayList<>();
        ArrayList<Boolean> owners = new ArrayList<>();

        for (Message m : messages) {
            texts.add(m.getText());
            owners.add(m.isUser());
        }

        outState.putStringArrayList("messages_text", texts);
        outState.putSerializable("messages_owner", owners);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1001 && resultCode == RESULT_OK && data != null) {
            ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (result != null && !result.isEmpty()) {
                promptEditText.setText(result.get(0)); // insert voice text into input box
                promptEditText.setSelection(promptEditText.getText().length()); // move cursor to end
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_clear_chat) {
            db.messageDao().clearMessages();
            messages.clear();
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Chat cleared", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (id == R.id.action_export_chat) {
            exportChat();
            return true;
        }

        if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void exportChat() {
        // Fetch messages from DB
        List<Message> chatHistory = db.messageDao().getAllMessages();

        if (chatHistory.isEmpty()) {
            Toast.makeText(this, "No chat history to export", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build plain text
        StringBuilder builder = new StringBuilder();
        for (Message m : chatHistory) {
            if (m.isUser()) {
                builder.append("You: ");
            } else {
                builder.append("Bot: ");
            }
            builder.append(m.getText()).append("\n\n");
        }

        String chatText = builder.toString();

        // ⚡ Ask user: Save or Share?
        String[] options = {"Share Chat", "Save as TXT"};
        new android.app.AlertDialog.Builder(this)
                .setTitle("Export Chat")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // 👉 Share
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("text/plain");
                        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Chat Export");
                        shareIntent.putExtra(Intent.EXTRA_TEXT, chatText);
                        startActivity(Intent.createChooser(shareIntent, "Export Chat via"));
                    } else {
                        // 👉 Save as TXT
                        saveChatAsFile(chatText);
                    }
                })
                .show();
    }

    private void saveChatAsFile(String chatText) {
        try {
            // File path → /storage/emulated/0/Download/chat_export.txt
            String fileName = "chat_export_" + System.currentTimeMillis() + ".txt";
            java.io.File path = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
            java.io.File file = new java.io.File(path, fileName);

            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            fos.write(chatText.getBytes());
            fos.close();

            Toast.makeText(this, "Saved to " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show();
        }
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About This App")
                .setMessage(
                        "Bhavya AI Chat\n\n" +
                                "A smart chatbot app powered by Gemini API.\n\n" +
                                "Developed with ❤️ by Bhavya Sanghrajka.\n\n" +
                                "Features:\n" +
                                "• Voice Input\n" +
                                "• Dark Mode\n" +
                                "• Chat History (Room DB)\n" +
                                "• Export Chat\n" +
                                "• Typing Indicator\n\n" +
                                "GitHub: github.com/BkS1708\n"
                )
                .setPositiveButton("OK", null)
                .show();
    }


}
