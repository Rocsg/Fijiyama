package io.github.rocsg.fijiyama.registration;

public class TestGargee {
    public static void main(String[] args) {
        System.out.println("Hello Gargee");
        System.out.println("Now running the test...");
        test();
    }

    public static void test() {
        //ItkTransform myRigidBodyTransform=ItkTransform.readTransformFromFile("Please write here your path");
        //or
        ItkTransform myRigidBodyTransform2=ItkTransform.array16ElementsToItkTransform(new double[]{1,0,0,0,  0,1,0,0,  0,0,1,0,  0,0,0,1});
        doSomething(myRigidBodyTransform2);
        System.out.println("Test passed!");
    }

    public static void doSomething(ItkTransform myRigidBodyTransform) {
        System.out.println("I am doing something with the transform");
    }
}
