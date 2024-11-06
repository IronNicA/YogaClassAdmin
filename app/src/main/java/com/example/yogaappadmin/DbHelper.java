package com.example.yogaappadmin;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.SetOptions;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "YogaApp.db";
    private static final int DATABASE_VERSION = 6;
    public static final String TABLE_YOGA_CLASSES = "YogaClasses";
    public static final String COLUMN_CLASS_ID = "classId";
    public static final String COLUMN_DAY_OF_WEEK = "dayOfWeek";
    public static final String COLUMN_TIME = "time";
    public static final String COLUMN_CAPACITY = "capacity";
    public static final String COLUMN_DURATION = "duration";
    public static final String COLUMN_PRICE_PER_CLASS = "pricePerClass";
    public static final String COLUMN_CLASS_TYPE = "classType";
    public static final String COLUMN_DESCRIPTION = "description";

    private static final String CREATE_TABLE_YOGA_CLASSES =
            "CREATE TABLE " + TABLE_YOGA_CLASSES + "("
                    + COLUMN_CLASS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_DAY_OF_WEEK + " TEXT NOT NULL, "
                    + COLUMN_TIME + " TEXT NOT NULL, "
                    + COLUMN_CAPACITY + " INTEGER NOT NULL, "
                    + COLUMN_DURATION + " INTEGER NOT NULL, "
                    + COLUMN_PRICE_PER_CLASS + " REAL NOT NULL, "
                    + COLUMN_CLASS_TYPE + " TEXT NOT NULL, "
                    + COLUMN_DESCRIPTION + " TEXT"
                    + ");";

    public static final String TABLE_CLASS_INSTANCES = "ClassInstances";
    public static final String COLUMN_INSTANCE_ID = "instanceId";
    public static final String COLUMN_CLASS_ID_FK = "classId"; // Foreign key from YogaClasses
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_TEACHER = "teacher";
    public static final String COLUMN_COMMENTS = "comments";

    private static final String CREATE_TABLE_CLASS_INSTANCES =
            "CREATE TABLE " + TABLE_CLASS_INSTANCES + "("
                    + COLUMN_INSTANCE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COLUMN_CLASS_ID_FK + " INTEGER, "
                    + COLUMN_DATE + " TEXT NOT NULL, "
                    + COLUMN_TEACHER + " TEXT, "
                    + COLUMN_COMMENTS + " TEXT, "
                    + "FOREIGN KEY(" + COLUMN_CLASS_ID_FK + ") REFERENCES " + TABLE_YOGA_CLASSES + "(" + COLUMN_CLASS_ID + ")"
                    + ");";

    public static final String TABLE_SYNC_STATUS = "SyncStatus";
    public static final String COLUMN_NOTIFY_FLAG = "notifyFlag";

    private static final String CREATE_TABLE_SYNC_STATUS =
            "CREATE TABLE " + TABLE_SYNC_STATUS + " ("
                    + COLUMN_NOTIFY_FLAG + " INTEGER NOT NULL DEFAULT 0, "
                    + "PRIMARY KEY (" + COLUMN_NOTIFY_FLAG + "));";

    private final FirebaseFirestore db;
    private final Context context;

    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_YOGA_CLASSES);
        db.execSQL(CREATE_TABLE_CLASS_INSTANCES);
        db.execSQL(CREATE_TABLE_SYNC_STATUS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_YOGA_CLASSES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CLASS_INSTANCES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SYNC_STATUS);
        onCreate(db);
    }

    public void notifySync() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTIFY_FLAG, 1);

        int rowsAffected = db.update(TABLE_SYNC_STATUS, values, null, null);
        if (rowsAffected == 0) {
            db.insert(TABLE_SYNC_STATUS, null, values);
        }
        db.close();
    }

    public boolean isSyncNeeded() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = null;
        boolean syncNeeded = false;

        try {
            cursor = db.query(TABLE_SYNC_STATUS,
                    new String[]{COLUMN_NOTIFY_FLAG},
                    null, null, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                int notifyFlag = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_NOTIFY_FLAG));
                syncNeeded = (notifyFlag == 1);

                if (syncNeeded) {
                    Log.d("DbHelper", "Sync is needed: notifyFlag is 1");
                } else {
                    Log.d("DbHelper", "Sync is not needed: notifyFlag is 0");
                }
            }
        } catch (Exception e) {
            Log.e("DbHelper", "Error checking sync status", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return syncNeeded;
    }

    public void resetSyncFlag() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NOTIFY_FLAG, 0);

        db.update(TABLE_SYNC_STATUS, values, null, null);
        db.close();
    }

    public long addYogaClass(YogaClass yogaClass) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_DAY_OF_WEEK, yogaClass.getDayOfWeek());
        values.put(COLUMN_TIME, yogaClass.getTime());
        values.put(COLUMN_CAPACITY, yogaClass.getCapacity());
        values.put(COLUMN_DURATION, yogaClass.getDuration());
        values.put(COLUMN_PRICE_PER_CLASS, yogaClass.getPricePerClass());
        values.put(COLUMN_CLASS_TYPE, yogaClass.getClassType());
        values.put(COLUMN_DESCRIPTION, yogaClass.getDescription());

        long classId = db.insert(TABLE_YOGA_CLASSES, null, values);
        db.close();

        notifySync();

        return classId;
    }

    public List<YogaClass> getAllYogaClasses() {
        List<YogaClass> yogaClasses = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_YOGA_CLASSES;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                YogaClass yogaClass = new YogaClass(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAY_OF_WEEK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAPACITY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE_PER_CLASS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                );
                yogaClasses.add(yogaClass);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return yogaClasses;
    }

    public void updateYogaClassField(int classId, String field, String newValue) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (field.contains(" ")) {
            field = "`" + field + "`";
        }
        values.put(field, newValue);
        db.update(TABLE_YOGA_CLASSES, values, COLUMN_CLASS_ID + " = ?", new String[]{String.valueOf(classId)});
        db.close();
        notifySync();
    }

    public boolean deleteYogaClass(long classId) {
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_CLASS_INSTANCES, COLUMN_CLASS_ID + " = ?", new String[]{String.valueOf(classId)});

        int result = db.delete(TABLE_YOGA_CLASSES, COLUMN_CLASS_ID + " = ?", new String[]{String.valueOf(classId)});
        db.close();

        notifySync();

        return result > 0;
    }

    public boolean deleteYogaCloud(int classId) {

        deleteClassInstancesFromFirestore(classId);

        deleteFirestore("yogaClasses", classId);

        notifySync();

        return true;
    }

    private void deleteClassInstancesFromFirestore(long classId) {
        db.collection("classInstances")
            .whereEqualTo("classId", classId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        deleteFirestore("classInstances", Long.parseLong(document.getId()));
                    }
                } else {
                    Log.w("Firestore", "Error getting documents: ", task.getException());
                }
            });
    }

    public List<ClassInstance> getAllClassInstances() {
        List<ClassInstance> classInstances = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CLASS_INSTANCES;

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ClassInstance classInstance = new ClassInstance(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INSTANCE_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID_FK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEACHER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENTS))
                );
                classInstances.add(classInstance);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return classInstances;
    }

    public YogaClass getYogaClassById(int classId) {
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(
                TABLE_YOGA_CLASSES,
                new String[]{COLUMN_CLASS_ID, COLUMN_DAY_OF_WEEK, COLUMN_TIME, COLUMN_CAPACITY, COLUMN_DURATION, COLUMN_PRICE_PER_CLASS, COLUMN_CLASS_TYPE, COLUMN_DESCRIPTION},
                COLUMN_CLASS_ID + "=?",
                new String[]{String.valueOf(classId)},
                null, null, null, null
        );

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                YogaClass yogaClass = new YogaClass(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DAY_OF_WEEK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TIME)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CAPACITY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_DURATION)),
                        cursor.getDouble(cursor.getColumnIndexOrThrow(COLUMN_PRICE_PER_CLASS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CLASS_TYPE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DESCRIPTION))
                );
                cursor.close();
                db.close();
                return yogaClass;
            }
            cursor.close();
        }
        db.close();

        return null;
    }

    public long addClassInstance(int classId, ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CLASS_ID_FK, classId);
        values.put(COLUMN_DATE, classInstance.getDate());
        values.put(COLUMN_TEACHER, classInstance.getTeacher());
        values.put(COLUMN_COMMENTS, classInstance.getComments());

        long instanceId = db.insert(TABLE_CLASS_INSTANCES, null, values);
        db.close();

        notifySync();

        return instanceId;
    }

    public ClassInstance getClassInstanceById(int instanceId) {
        SQLiteDatabase db = this.getReadableDatabase();
        String[] columns = {COLUMN_INSTANCE_ID, COLUMN_CLASS_ID_FK, COLUMN_DATE, COLUMN_TEACHER, COLUMN_COMMENTS};
        String selection = COLUMN_INSTANCE_ID + " = ?";
        String[] selectionArgs = {String.valueOf(instanceId)};

        Cursor cursor = db.query(TABLE_CLASS_INSTANCES, columns, selection, selectionArgs, null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            ClassInstance classInstance = new ClassInstance(
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INSTANCE_ID)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID_FK)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEACHER)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENTS))
            );
            cursor.close();
            db.close();
            return classInstance;
        } else {
            db.close();
            return null;
        }
    }

    public List<ClassInstance> getClassInstancesByClassId(int classId) {
        List<ClassInstance> classInstances = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CLASS_INSTANCES + " WHERE " + COLUMN_CLASS_ID_FK + " = ?";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, new String[] { String.valueOf(classId) });

        if (cursor.moveToFirst()) {
            do {
                ClassInstance classInstance = new ClassInstance(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INSTANCE_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID_FK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEACHER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENTS))
                );
                classInstances.add(classInstance);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return classInstances;
    }

    public List<ClassInstance> searchClassInstancesByTeacher(String teacher) {
        List<ClassInstance> classInstances = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_CLASS_INSTANCES + " WHERE LOWER(" + COLUMN_TEACHER + ") LIKE ?";

        SQLiteDatabase db = this.getReadableDatabase();
        String searchPattern = "%" + teacher.toLowerCase() + "%";
        Cursor cursor = db.rawQuery(selectQuery, new String[]{searchPattern});

        if (cursor.moveToFirst()) {
            do {
                ClassInstance classInstance = new ClassInstance(
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_INSTANCE_ID)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CLASS_ID_FK)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_TEACHER)),
                        cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_COMMENTS))
                );
                classInstances.add(classInstance);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return classInstances;
    }

    public int updateClassInstance(ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_CLASS_ID_FK, classInstance.getClassId());
        values.put(COLUMN_DATE, classInstance.getDate());
        values.put(COLUMN_TEACHER, classInstance.getTeacher());
        values.put(COLUMN_COMMENTS, classInstance.getComments());

        int rowsAffected = db.update(TABLE_CLASS_INSTANCES, values, COLUMN_INSTANCE_ID + " = ?", new String[]{String.valueOf(classInstance.getInstanceId())});


        db.close();
        return rowsAffected;
    }

    public boolean deleteClassInstance(int instanceId) {
        SQLiteDatabase db = this.getWritableDatabase();
        int result = db.delete(TABLE_CLASS_INSTANCES, COLUMN_INSTANCE_ID + " = ?", new String[]{String.valueOf(instanceId)});
        db.close();

        notifySync();

        return result > 0;
    }

    public boolean deleteInstanceCloud(int instanceId) {

        deleteFirestore("classInstances", instanceId);

        notifySync();

        return true;
    }

    public void syncYogaClassesAndInstances() {

        List<YogaClass> localYogaClasses = getAllYogaClasses();
        List<ClassInstance> localClassInstances = getAllClassInstances();
        AtomicInteger successCount = new AtomicInteger(0);

        for (YogaClass yogaClass : localYogaClasses) {
            uploadYogaClassToFirestore(yogaClass, successCount, localYogaClasses.size());
        }

        for (ClassInstance classInstance : localClassInstances) {
            uploadClassInstanceToFirestore(classInstance, successCount, localClassInstances.size());
        }

        if (successCount.get() == (localYogaClasses.size() + localClassInstances.size())) {
            resetSyncFlag();
            Log.d("FirestoreSync", "All classes and instances uploaded successfully");
        }
    }

    public void uploadYogaClassToFirestore(YogaClass yogaClass, AtomicInteger successCount, int totalClasses) {
        DocumentReference docRef = db.collection("yogaClasses").document(String.valueOf(yogaClass.getClassId()));
        docRef.set(yogaClass, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Log.d("Firestore", "Yoga class uploaded: " + yogaClass.getClassId());
                successCount.incrementAndGet();
                checkUploadCompletion(successCount, totalClasses);
            })
            .addOnFailureListener(e -> {
                Log.w("Firestore", "Error uploading yoga class", e);
            });
    }

    public void uploadClassInstanceToFirestore(ClassInstance classInstance, AtomicInteger successCount, int totalInstances) {
        DocumentReference docRef = db.collection("classInstances").document(String.valueOf(classInstance.getInstanceId()));
        docRef.set(classInstance, SetOptions.merge())
            .addOnSuccessListener(aVoid -> {
                Log.d("Firestore", "Class instance uploaded: " + classInstance.getInstanceId());
                successCount.incrementAndGet();
                checkUploadCompletion(successCount, totalInstances);
            })
            .addOnFailureListener(e -> {
                Log.w("Firestore", "Error uploading class instance", e);
            });
    }

    private void checkUploadCompletion(AtomicInteger successCount, int total) {
        if (successCount.get() == total) {
            resetSyncFlag();
        }
    }

    public void syncLocalDataWithFirebase() {
        Task<Void> yogaClassesTask = fetchYogaClassesFromFirebase();
        Task<Void> classInstancesTask = fetchClassInstancesFromFirebase();

        if (yogaClassesTask == null) {
            Log.e("Firebase", "yogaClassesTask is null.");
        }
        if (classInstancesTask == null) {
            Log.e("Firebase", "classInstancesTask is null.");
        }

        if (yogaClassesTask == null || classInstancesTask == null) {
            Log.e("Firebase", "One or both tasks are null. Cannot proceed with sync.");
            return;
        }

        Tasks.whenAllSuccess(yogaClassesTask, classInstancesTask)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    resetSyncFlag();
                    Log.d("Firebase", "Sync completed for yoga classes and class instances.");
                    Intent intent = new Intent(context, MainActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    Log.e("Firebase", "Sync failed: " + task.getException());
                }
            });
    }

    private Task<Void> fetchYogaClassesFromFirebase() {
        return db.collection("yogaClasses").get()
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    List<Integer> firestoreClassIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        YogaClass yogaClass = document.toObject(YogaClass.class);
                        firestoreClassIds.add(yogaClass.getClassId());
                        updateLocalYogaClass(yogaClass);
                    }
                    Log.d("Firebase", "Yoga classes fetched successfully.");
                    deleteNonExistingYogaClasses(firestoreClassIds);
                } else {
                    Log.w("Firebase", "Error fetching yoga classes.", task.getException());
                }
                return Tasks.forResult(null);
            });
    }

    private void deleteNonExistingYogaClasses(List<Integer> firestoreClassIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        String ids = TextUtils.join(",", firestoreClassIds);
        db.delete(TABLE_YOGA_CLASSES, COLUMN_CLASS_ID + " NOT IN (" + ids + ")", null);
    }

    private Task<Void> fetchClassInstancesFromFirebase() {
        return db.collection("classInstances").get()
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    List<Integer> firestoreInstanceIds = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        ClassInstance classInstance = document.toObject(ClassInstance.class);
                        firestoreInstanceIds.add(classInstance.getInstanceId());
                        updateLocalClassInstance(classInstance);
                    }
                    Log.d("Firebase", "Class instances fetched successfully.");

                    deleteNonExistingClassInstances(firestoreInstanceIds);
                } else {
                    Log.w("Firebase", "Error fetching class instances.", task.getException());
                }
                return Tasks.forResult(null);
            });
    }

    private void deleteNonExistingClassInstances(List<Integer> firestoreInstanceIds) {
        SQLiteDatabase db = this.getWritableDatabase();
        String ids = TextUtils.join(",", firestoreInstanceIds);
        db.delete(TABLE_CLASS_INSTANCES, COLUMN_INSTANCE_ID + " NOT IN (" + ids + ")", null);
    }

    private void updateLocalYogaClass(YogaClass yogaClass) {
        if (isYogaClassExists(yogaClass.getClassId())) {
            updateYogaClassInLocalDB(yogaClass);
        } else {
            insertYogaClassToLocalDB(yogaClass);
        }
    }

    private void updateLocalClassInstance(ClassInstance classInstance) {
        if (isClassInstanceExists(classInstance.getInstanceId())) {
            updateClassInstanceInLocalDB(classInstance);
        } else {
            insertClassInstanceToLocalDB(classInstance);
        }
    }

    private boolean isYogaClassExists(int classId) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_YOGA_CLASSES, new String[]{COLUMN_CLASS_ID}, COLUMN_CLASS_ID + " = ?", new String[]{String.valueOf(classId)}, null, null, null);
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    private void updateYogaClassInLocalDB(YogaClass yogaClass) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_DAY_OF_WEEK, yogaClass.getDayOfWeek());
        values.put(COLUMN_TIME, yogaClass.getTime());
        values.put(COLUMN_DESCRIPTION, yogaClass.getDescription());
        values.put(COLUMN_CAPACITY, yogaClass.getCapacity());
        values.put(COLUMN_DURATION, yogaClass.getDuration());
        values.put(COLUMN_PRICE_PER_CLASS, yogaClass.getPricePerClass());
        values.put(COLUMN_CLASS_TYPE, yogaClass.getClassType());

        db.update(TABLE_YOGA_CLASSES, values, COLUMN_CLASS_ID + " = ?", new String[]{String.valueOf(yogaClass.getClassId())});
    }

    private void insertYogaClassToLocalDB(YogaClass yogaClass) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_CLASS_ID, yogaClass.getClassId());
        values.put(COLUMN_DAY_OF_WEEK, yogaClass.getDayOfWeek());
        values.put(COLUMN_TIME, yogaClass.getTime());
        values.put(COLUMN_DESCRIPTION, yogaClass.getDescription());
        values.put(COLUMN_CAPACITY, yogaClass.getCapacity());
        values.put(COLUMN_DURATION, yogaClass.getDuration());
        values.put(COLUMN_PRICE_PER_CLASS, yogaClass.getPricePerClass());
        values.put(COLUMN_CLASS_TYPE, yogaClass.getClassType());

        db.insert(TABLE_YOGA_CLASSES, null, values);
    }

    private boolean isClassInstanceExists(int instanceId) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_CLASS_INSTANCES, new String[]{COLUMN_INSTANCE_ID},
                COLUMN_INSTANCE_ID + " = ?", new String[]{String.valueOf(instanceId)}, null, null, null);

        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }

    private void updateClassInstanceInLocalDB(ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(COLUMN_CLASS_ID_FK, classInstance.getClassId());
        values.put(COLUMN_DATE, classInstance.getDate());
        values.put(COLUMN_TEACHER, classInstance.getTeacher());
        values.put(COLUMN_COMMENTS, classInstance.getComments());

        db.update(TABLE_CLASS_INSTANCES, values, COLUMN_INSTANCE_ID + " = ?", new String[]{String.valueOf(classInstance.getInstanceId())});
    }

    private void insertClassInstanceToLocalDB(ClassInstance classInstance) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(COLUMN_INSTANCE_ID, classInstance.getInstanceId());
        values.put(COLUMN_CLASS_ID_FK, classInstance.getClassId());
        values.put(COLUMN_DATE, classInstance.getDate());
        values.put(COLUMN_TEACHER, classInstance.getTeacher());
        values.put(COLUMN_COMMENTS, classInstance.getComments());

        db.insert(TABLE_CLASS_INSTANCES, null, values);
    }

    private void deleteFirestore(String collection, long id) {
        DocumentReference docRef = db.collection(collection).document(String.valueOf(id));
        docRef.delete()
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "DocumentSnapshot successfully deleted!"))
                .addOnFailureListener(e -> Log.w("Firestore", "Error deleting document", e));
    }

    public void checkIfExists(String collection, int id, OnCheckExistsListener listener) {
        DocumentReference docRef = db.collection(collection).document(String.valueOf(id));
        docRef.get()
                .addOnSuccessListener(documentSnapshot -> {
                    listener.onResult(documentSnapshot.exists());
                })
                .addOnFailureListener(e -> {
                    Log.w("Firestore", "Error checking document existence", e);
                    listener.onResult(false);
                });
    }

    public interface OnCheckExistsListener {
        void onResult(boolean exists);
    }

    public void backupDatabase() {
        try {
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "/data/data/" + context.getPackageName() + "/databases/" + DATABASE_NAME;

                Calendar calendar = Calendar.getInstance();
                SimpleDateFormat timeFormat = new SimpleDateFormat("HHmm", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("ddMMyyyy", Locale.getDefault());

                String timeStamp = timeFormat.format(calendar.getTime());
                String dateStamp = dateFormat.format(calendar.getTime());
                String backupDBPath = "backup_" + DATABASE_NAME + "_" + timeStamp + "_" + dateStamp + ".db";

                File currentDB = new File(currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                    Log.d("DbHelper", "Backup successful to " + backupDB.getAbsolutePath());
                } else {
                    Log.d("DbHelper", "Database file does not exist at " + currentDBPath);
                }
            }
        } catch (Exception e) {
            Log.e("DbHelper", "Error backing up database: " + e.getMessage());
        }
    }

    public List<String> getBackupFiles() {
        List<String> backupFiles = new ArrayList<>();
        File sd = Environment.getExternalStorageDirectory();
        File backupFolder = new File(sd, "/");

        if (backupFolder.exists() && backupFolder.isDirectory()) {
            File[] files = backupFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("backup_" + DATABASE_NAME) && file.getName().endsWith(".db")) {
                        backupFiles.add(file.getName());
                        Log.d("BackupFiles", "Loaded backup file: " + file.getName());
                    }
                }
            }
        }
        return backupFiles;
    }

    public void restoreDatabase(String backupFileName) {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File data = Environment.getDataDirectory();

            String currentDBPath = "//data//" + context.getPackageName() + "//databases//" + DATABASE_NAME;
            File currentDB = new File(data, currentDBPath);
            File backupDB = new File(sd, backupFileName);

            if (backupDB.exists()) {
                FileChannel src = new FileInputStream(backupDB).getChannel();
                FileChannel dst = new FileOutputStream(currentDB).getChannel();
                dst.transferFrom(src, 0, src.size());
                src.close();
                dst.close();
                Log.d("DbHelper", "Database restored from " + backupFileName);
            } else {
                Log.e("DbHelper", "Backup file does not exist: " + backupFileName);
            }
        } catch (Exception e) {
            Log.e("DbHelper", "Error restoring database: " + e.getMessage());
        }
    }

    public void deleteBackup(String backupFileName) {
        try {
            File sd = Environment.getExternalStorageDirectory();
            File backupFile = new File(sd, backupFileName);

            if (backupFile.exists()) {
                if (backupFile.delete()) {
                    Log.d("DbHelper", "Backup deleted successfully: " + backupFileName);
                } else {
                    Log.e("DbHelper", "Failed to delete backup: " + backupFileName);
                }
            } else {
                Log.e("DbHelper", "Backup file does not exist: " + backupFileName);
            }
        } catch (Exception e) {
            Log.e("DbHelper", "Error deleting backup: " + e.getMessage());
        }
    }
}
