package testRepo;

public class ChainRootIdentifier_Test {

    public ChainRootIdentifier_Test foo() { return this; }

    public ChainRootIdentifier_Test bar() {return this; }

    public ChainRootIdentifier_Test baz() {return this; }

    public void main() {
        foo().bar().baz();
    }
}
