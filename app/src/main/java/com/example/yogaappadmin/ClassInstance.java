package com.example.yogaappadmin;

public class ClassInstance {
    private int instanceId;
    private int classId; // Foreign key to YogaClass
    private String date;
    private String teacher;
    private String comments;

    public ClassInstance(int instanceId, int classId, String date, String teacher, String comments) {
        this.instanceId = instanceId;
        this.classId = classId;
        this.date = date;
        this.teacher = teacher;
        this.comments = comments;
    }

    public ClassInstance() {
    }

    public int getInstanceId() { return instanceId; }
    public void setInstanceId(int instanceId) { this.instanceId = instanceId; }

    public int getClassId() { return classId; }
    public void setClassId(int classId) { this.classId = classId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }
}
