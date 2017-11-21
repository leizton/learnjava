package com.whiker.learn.mysql;

public class User {

    public enum Gender {
        Null, Man, Woman;

        @Override
        public String toString() {
            if (this == Gender.Man) {
                return "1";
            } else if (this == Gender.Woman) {
                return "2";
            } else {
                return "0";
            }
        }
    }

    private int id;
    private String name;
    private short age;
    private Gender gender = Gender.Null;

    public User() {
    }

    public User(String name, short age, Gender gender) {
        this.name = name;
        this.age = age;
        this.gender = gender;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass() != User.class) {
            return false;
        }

        User user = (User) obj;
        return user.name.equals(name) &&
                user.age == age &&
                user.gender == gender;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setAge(short age) {
        this.age = age;
    }

    public void setGender(String s) {
        if (s.equals("1")) {
            this.gender = Gender.Man;
        } else if (s.equals("2")) {
            this.gender = Gender.Woman;
        } else {
            this.gender = Gender.Null;
        }
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public short getAge() {
        return age;
    }

    public Gender getGender() {
        return gender;
    }
}
