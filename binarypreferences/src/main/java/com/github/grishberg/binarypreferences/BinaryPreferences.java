package com.github.grishberg.binarypreferences;

import android.content.SharedPreferences;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

/**
 * Reads and writes {@link SharedPreferences} into binary file.
 */
@MainThread
class BinaryPreferences implements SharedPreferences {
    private static final String TAG = BinaryPreferences.class.getSimpleName();
    private static final byte TYPE_STRING = 0;
    private static final byte TYPE_STRING_SET = 1;
    private static final byte TYPE_INT = 2;
    private static final byte TYPE_BOOLEAN = 3;
    private static final byte TYPE_LONG = 4;
    private static final byte TYPE_FLOAT = 5;

    private final Executor applyExecutor;
    private ArrayList<OnSharedPreferenceChangeListener> listeners = new ArrayList<>();

    private HashMap<String, ValueHolder> values = new HashMap<>();
    private final File preferencesFile;

    public BinaryPreferences(String preferencesName) {
        this(new File(preferencesName));
    }

    public BinaryPreferences(File preferencesFile) {
        this(preferencesFile, createExecutor());
    }

    public BinaryPreferences(File preferencesFile, Executor applyExecutor) {
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
        values = new HashMap<>(count);
        for (int i = 0; i < count; i++) {
            // 1) name
            String name = ois.readUTF();
            // 2) type
            byte type = ois.readByte();

            switch (type) {
                case TYPE_STRING:
                    values.put(name, new ValueHolder(type, ois.readUTF()));
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

    @Override
    public Map<String, ?> getAll() {
        return values;
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        ValueHolder result = values.get(key);
        return result != null ? (String) result.value : defValue;
    }

    @Nullable
    @Override
    public Set<String> getStringSet(String key, @Nullable Set<String> defValues) {
        ValueHolder result = values.get(key);
        return result != null ? (Set<String>) result.value : defValues;
    }

    @Override
    public int getInt(String key, int defValue) {
        ValueHolder result = values.get(key);
        return result != null ? (Integer) result.value : defValue;
    }

    @Override
    public long getLong(String key, long defValue) {
        ValueHolder result = values.get(key);
        return result != null ? (Long) result.value : defValue;
    }

    @Override
    public float getFloat(String key, float defValue) {
        ValueHolder result = values.get(key);
        return result != null ? (Float) result.value : defValue;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        ValueHolder result = values.get(key);
        return result != null ? (Boolean) result.value : defValue;
    }

    @Override
    public boolean contains(String key) {
        return values.containsKey(key);
    }

    @Override
    public Editor edit() {
        return new BinaryEditor(applyExecutor, preferencesFile, values);
    }

    /**
     * @return Thread-safe {@link android.content.SharedPreferences.Editor}
     */
    public Editor threadSafeEdit() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        listeners.remove(listener);
    }

    private class BinaryEditor implements Editor {
        private final Executor applyExecutor;
        private final File targetFile;
        private final HashMap<String, ValueHolder> targetValues;
        private final HashMap<String, ValueHolder> cachedValues = new HashMap<>();
        private final HashSet<String> removedValues = new HashSet<>();

        public BinaryEditor(Executor executor, File targetFile, HashMap<String, ValueHolder> targetValues) {
            applyExecutor = executor;
            this.targetFile = targetFile;
            this.targetValues = targetValues;
        }

        @Override
        public Editor putString(String key, @Nullable String value) {
            cachedValues.put(key, new ValueHolder(TYPE_STRING, value));
            return this;
        }

        @Override
        public Editor putStringSet(String key, @Nullable Set<String> values) {
            cachedValues.put(key, new ValueHolder(TYPE_STRING_SET, values));
            return this;
        }

        @Override
        public Editor putInt(String key, int value) {
            cachedValues.put(key, new ValueHolder(TYPE_INT, value));
            return this;
        }

        @Override
        public Editor putLong(String key, long value) {
            cachedValues.put(key, new ValueHolder(TYPE_LONG, value));
            return this;
        }

        @Override
        public Editor putFloat(String key, float value) {
            cachedValues.put(key, new ValueHolder(TYPE_FLOAT, value));
            return this;
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            cachedValues.put(key, new ValueHolder(TYPE_BOOLEAN, value));
            return this;
        }

        @Override
        public Editor remove(String key) {
            removedValues.add(key);
            return this;
        }

        @Override
        public Editor clear() {
            cachedValues.clear();
            return this;
        }

        @Override
        public boolean commit() {
            commitToMemory();
            saveToFile(targetValues);
            return true;
        }

        @Override
        public void apply() {
            commitToMemory();
            applyExecutor.execute(new ApplyRunnable(targetValues));
        }

        private void commitToMemory() {
            for (String removedValue : removedValues) {
                targetValues.remove(removedValue);
            }
            targetValues.putAll(cachedValues);
        }

        private void saveToFile(HashMap<String, ValueHolder> values) {
            if (targetFile.exists()) {
                if (!targetFile.delete()) {
                    Log.e(TAG, "Can't delete existing file");
                }
            }
            try {
                targetFile.createNewFile();
                FileOutputStream fos = new FileOutputStream(targetFile);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                bos.write(getBytes(values));
                bos.close();
            } catch (Exception e) {
                Log.e(TAG, "Save error", e);
            }
        }

        private byte[] getBytes(HashMap<String, ValueHolder> values) throws IOException {
            try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                ObjectOutputStream out;
                out = new ObjectOutputStream(bos);

                out.writeInt(targetValues.size());

                for (Map.Entry<String, ValueHolder> entry : values.entrySet()) {
                    String name = entry.getKey();
                    ValueHolder valueHolder = entry.getValue();

                    out.writeUTF(name);
                    out.writeByte(valueHolder.type);

                    Object value = valueHolder.value;
                    switch (valueHolder.type) {
                        case TYPE_STRING:
                            out.writeUTF((String) value);
                            break;
                        case TYPE_STRING_SET:
                            Set<String> set = (Set<String>) value;
                            int setLength = set.size();
                            out.writeInt(setLength);

                            for (String str : set) {
                                out.writeUTF(str);
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
            private final HashMap<String, ValueHolder> values;

            public ApplyRunnable(HashMap<String, ValueHolder> values) {
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
