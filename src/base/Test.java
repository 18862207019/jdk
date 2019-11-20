package base;

public class Test {
    public static  volatile  int a=1;
    public static void test(){
        System.out.println(a);
        a=a+1;
        System.out.println(a);
    }

    public static void main(String[] args) {
        test();
    }
}
