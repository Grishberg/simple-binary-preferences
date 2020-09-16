package com.github.grishberg.binarypreferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

/**
 * Reads and writes {@link SharedPreferences} into binary file.
 */
@AnyThread
public class BinaryPreferences implements SharedPreferences {
    private static final String TAG = BinaryPreferences.class.getSimpleName();
    private static final byte TYPE_STRING = 0;
    private static final byte TYPE_STRING_SET = 1;
    private static final byte TYPE_INT = 2;
    private static final byte TYPE_BOOLEAN = 3;
    private static final byte TYPE_LONG = 4;
    private static final byte TYPE_FLOAT = 5;

    private final Object lock = new Object();
    private final Executor applyExecutor;
    private ArrayList<OnSharedPreferenceChangeListener> listeners = new ArrayList<>();

    private ArrayMap<String, ValueHolder> values = new ArrayMap<>();
    private final File preferencesFile;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    public BinaryPreferences(@NonNull Context context, @NonNull String preferencesName) {
        this(new File(context.getApplicationInfo().dataDir, preferencesName));
    }

    public BinaryPreferences(@NonNull File preferencesFile) {
        this(preferencesFile, createExecutor());
    }

    public BinaryPreferences(@NonNull File preferencesFile, @NonNull Executor applyExecutor) {
        this.preferencesFile = preferencesFile;
        this.applyExecutor = applyExecutor;
        readPreferences();
    }

    private static Executor createExecutor() {
        return Executors.newSingleThreadExecutor();
    }

    private void readPreferences() {
        if (!preferencesFile.exists()) {
            return;
        }
        try (FileInputStream stream = new FileInputStream(preferencesFile)) {
            try {
                BufferedInputStream bis = new BufferedInputStream(stream);
                ObjectInputStream ois = new ObjectInputStream(bis);

                readValues(ois);
            } catch (Exception e) {
                Log.e(TAG, "Read error", e);
            }
        } catch (FileNotFoundException e) {
            Log.e(TAG, "File not found", e);
        } catch (IOException e) {
            Log.e(TAG, "Read error", e);
        }
    }

    private void readValues(ObjectInputStream ois) throws IOException {
        int count = ois.readInt();
        values = new ArrayMap<>(count);
        for (int i = 0; i < count; i++) {
            // 1) name
            String name = ois.readUTF();
            // 2) type
            byte type = ois.readByte();

            switch (type) {
                case TYPE_STRING:
                    values.put(name, new ValueHolder(type, readUTF(ois)));
                    break;
                case TYPE_STRING_SET:
                    int setLength = ois.readInt();
                    HashSet<String> set = new HashSet<>(setLength);
                    for (int setIndex = 0; setIndex < setLength; setIndex++) {
                        set.add(ois.readUTF());
                    }
                    values.put(name, new ValueHolder(type, set));
                    break;
                case TYPE_INT:
                    values.put(name, new ValueHolder(type, ois.readInt()));
                    break;
                case TYPE_BOOLEAN:
                    values.put(name, new ValueHolder(type, ois.readBoolean()));
                    break;
                case TYPE_LONG:
                    values.put(name, new ValueHolder(type, ois.readLong()));
                    break;
                case TYPE_FLOAT:
                    values.put(name, new ValueHolder(type, ois.readFloat()));
                    break;
            }
        }
    }

    private static String readUTF(ObjectInputStream ois) throws IOException {
        int len = ois.readShort();
        byte[] buf = new byte[len];
        ois.readFully(buf);
        return new String(buf, StandardCharsets.UTF_8);
    }

    private static void writeUTF(ObjectOutputStream out, String str) throws IOException {
        byte[] buf = str.getBytes(StandardCharsets.UTF_8);
        out.writeShort(buf.length);
        out.write(buf);
    }

    @Override
    public Map<String, ?> getAll() {
        synchronized (lock) {
            return new HashMap<>(values);
        }
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        synchronized (lock) {
            ValueHolder result = values.get(key);
            return result != null ? (String) result.value : defValue;
        }
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        synchronized (lock) {
            ValueHolder result = values.get(key);
            return result != null ? (Set<String>) result.value : defValues;
        }
    }

    @Override
    public int getInt(String key, int defValue) {
        synchronized (lock) {
            ValueHolder result = values.get(key);
            return result != null ? (Integer) result.value : defValue;
        }
    }

    @Override
    public long getLong(String key, long defValue) {
        synchronized (lock) {
            ValueHolder result = values.get(key);
            return result != null ? (Long) result.value : defValue;
        }
    }

    @Override
    public float getFloat(String key, float defValue) {
        synchronized (lock) {
            ValueHolder result = values.get(key);
            return result != null ? (Float) result.value : defValue;
        }
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (lock) {
            ValueHolder result = values.get(key);
            return result != null ? (Boolean) result.value : defValue;
        }
    }

    @Override
    public boolean contains(String key) {
        synchronized (lock) {
            return values.containsKey(key);
        }
    }

    @Override
    public Editor edit() {
        return new BinaryEditor();
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (lock) {
            listeners.add(listener);
        }
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized (lock) {
            listeners.remove(listener);
        }
    }

    private class BinaryEditor implements Editor {
        private final HashMap<String, ValueHolder> cachedValues = new HashMap<>();
        private final HashSet<String> removedValues = new HashSet<>();
        private final HashSet<String> changedValues = new HashSet<>();

        public BinaryEditor() {
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            synchronized (lock) {
                changedValues.add(key);
                cachedValues.put(key, new ValueHolder(TYPE_STRING, value));
                return this;
            }
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            synchronized (lock) {
                changedValues.add(key);
                cachedValues.put(key, new ValueHolder(TYPE_STRING_SET, values));
                return this;
            }
        }

        @Override
        public Editor putInt(String key, int value) {
            synchronized (lock) {
                changedValues.add(key);
                cachedValues.put(key, new ValueHolder(TYPE_INT, value));
                return this;
            }
        }

        @Override
        public Editor putLong(String key, long value) {
            synchronized (lock) {
                changedValues.add(key);
                cachedValues.put(key, new ValueHolder(TYPE_LONG, value));
                return this;
            }
        }

        @Override
        public Editor putFloat(String key, float value) {
            synchronized (lock) {
                changedValues.add(key);
                cachedValues.put(key, new ValueHolder(TYPE_FLOAT, value));
                return this;
            }
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            synchronized (lock) {
                changedValues.add(key);
                cachedValues.put(key, new ValueHolder(TYPE_BOOLEAN, value));
                return this;
            }
        }

        @Override
        public Editor remove(String key) {
            synchronized (lock) {
                removedValues.add(key);
                return this;
            }
        }

        @Override
        public Editor clear() {
            synchronized (lock) {
                cachedValues.clear();
                return this;
            }
        }

        @Override
        public boolean commit() {
            commitToMemory();
            saveToFile(values);
            return true;
        }

        @Override
        public void apply() {
            commitToMemory();
            applyExecutor.execute(new ApplyRunnable(values));
        }

        private void commitToMemory() {
            synchronized (lock) {
                for (String removedValue : removedValues) {
                    values.remove(removedValue);
                }
                values.putAll(cachedValues);
                changedValues.addAll(removedValues);
            }
            notifyListeners(changedValues);
        }

        private void notifyListeners(final Set<String> keys) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                for (String key : keys) {
                    for (OnSharedPreferenceChangeListener listener : listeners) {
                        listener.onSharedPreferenceChanged(BinaryPreferences.this, key);
                    }
                }
            } else {
                // Run this function on the main thread.
                mainThreadHandler.post(() -> notifyListeners(keys));
            }
        }

        private void saveToFile(ArrayMap<String, ValueHolder> values) {
            if (preferencesFile.exists()) {
                if (!preferencesFile.delete()) {
                    Log.e(TAG, "Can't delete existing file");
                }
            }
            try {
                preferencesFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(preferencesFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bos.write(getBytes(values));
                bos.close();
            } catch (Exception e) {
                Log.e(TAG, "Save error", e);
            }
        }

        private byte[] getBytes(ArrayMap<String, ValueHolder> values) throws IOException {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                ObjectOutputStream out;
                out = new ObjectOutputStream(bos);

                out.writeInt(values.size());

                for (Map.Entry<String, ValueHolder> entry : values.entrySet()) {
                    String name = entry.getKey();
                    ValueHolder valueHolder = entry.getValue();

                    out.writeUTF(name);
                    out.writeByte(valueHolder.type);

                    Object value = valueHolder.value;
                    switch (valueHolder.type) {
                        case TYPE_STRING:
                            writeUTF(out, (String) value);
                            break;
                        case TYPE_STRING_SET:
                            Set<String> set = (Set<String>) value;
                            int setLength = set.size();
                            out.writeInt(setLength);

                            for (String str : set) {
                                writeUTF(out, str);
                            }
                            break;
                        case TYPE_INT:
                            out.writeInt((Integer) value);
                            break;
                        case TYPE_BOOLEAN:
                            out.writeBoolean((Boolean) value);
                            break;
                        case TYPE_LONG:
                            out.writeLong((Long) value);
                            break;
                        case TYPE_FLOAT:
                            out.writeFloat((Float) value);
                            break;
                    }
                }
                out.flush();
                return bos.toByteArray();
            }
        }

        private class ApplyRunnable implements Runnable {
            private final ArrayMap<String, ValueHolder> values;

            public ApplyRunnable(ArrayMap<String, ValueHolder> values) {
                this.values = values;
            }

            @Override
            public void run() {
                saveToFile(values);
            }
        }
    }

    private static class ValueHolder {
        final byte type;
        final Object value;

        public ValueHolder(byte type, Object value) {
            this.type = type;
            this.value = value;
        }
    }
}
