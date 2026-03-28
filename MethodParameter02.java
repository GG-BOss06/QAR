package com.wangwm.unit6;

class MethodParameter02 {
    public static void main(String[] args) {
        B b = new B();
        int[] arr = {1, 2, 3};
        b.test100(arr);
        System.out.println("main");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + "\t");
        }
        System.out.println();
        Person2 p = new Person2();
        p.name = "jack";
        p.age = 10;
        b.test200(p);
        System.out.println("main" + p.age);
    }
}

class B {
    public void test100(int[] arr) {
        arr[0] = 200;
        System.out.println("B");
        for (int i = 0; i < arr.length; i++) {
            System.out.print(arr[i] + "\t");
        }
        System.out.println();
    }

    public void test200(Person2 p) {
        p.age = 10000;
    }
}

class Person2 {
    String name;
    int age;
}
