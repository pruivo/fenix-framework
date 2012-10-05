package pt.ist.fenixframework.example.helloworld;

import pt.ist.fenixframework.Atomic;

public class Person extends Person_Base {
    public Person(String name, HelloWorldApplication app) {
        setName(name);
        setApp(app);
    }

    @Atomic
    public void mmm() {
        System.out.println("doing nothing");
    }
    
    public String toString() {
	return getName();
    }
}
