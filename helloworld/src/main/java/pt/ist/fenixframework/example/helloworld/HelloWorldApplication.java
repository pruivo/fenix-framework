package pt.ist.fenixframework.example.helloworld;

public class HelloWorldApplication extends HelloWorldApplication_Base {
    public void sayHello() {
        System.out.println("Lets greet everyone: " + getPerson().size());
        

        for (Person p : getPerson()) {
            System.out.println("Hello " + p.getName());
        }
        System.out.println("Done");
    }
}
