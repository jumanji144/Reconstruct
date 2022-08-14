public class A {

	int a;
	int b;

	void b() {
		C b = new B();
		b.b();
		c(b);
		c((B) b);

		D d = new D();
		d.e();
		d.b();

	}

	void c(C c) {
		c.b();
	}

	void c(B b) {
		b.b();
	}

}
