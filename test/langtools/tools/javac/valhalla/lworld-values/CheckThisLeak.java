/*
 * @test /nodynamiccopyright/
 * @bug 8205910
 * @summary Complain when `this' of a value class is leaked from constructor before all instance fields are definitely assigned.
 * @compile/fail/ref=CheckThisLeak.out -XDrawDiagnostics -XDdev CheckThisLeak.java
 */

inline class V {

	private final int x, ymx;

	V(int x, int y) {

		OK();                       // OK to call static methods.
		this.OK();                  // OK to call static methods.

		validate();                 // BAD to call instance method DU = {x, ymx}
		this.validate();            // BAD to call instance method DU = {x, ymx}
		V.this.validate();          // BAD to call instance method DU = {x, ymx}

		System.out.println(this);   // BAD to pass `this' as argument DU = {x, ymx}
		System.out.println(V.this); // BAD to pass `this' as argument DU = {x, ymx}

		V v = this;                 // BAD to create alias  DU = {x, ymx}
		v = V.this;                 // BAD to create alias  DU = {x, ymx}

		ymx = y - x;                // OK, implicit this for field write.
		int l = this.ymx;           // OK, explicit this for DA field read.

		OK();                       // OK to call static methods.
		this.OK();                  // OK to call static methods.

		validate();                 // BAD to call instance method DU = {x}
		this.validate();            // BAD to call instance method DU = {x}
		V.this.validate();          // BAD to call instance method DU = {x}

		System.out.println(this);   // BAD to pass `this' as argument DU = {x}
		System.out.println(V.this); // BAD to pass `this' as argument DU = {x}

		v = this;                   // BAD to create alias  DU = {x}
		v = V.this;                 // BAD to create alias  DU = {x}

		this.x = x;

        // ALL fields are assigned now.

		OK();                       // OK to call static methods.
		this.OK();                  // OK to call static methods.

		validate();                 // OK: DU = {}
		this.validate();            // OK: DU = {}
		V.this.validate();          // OK: DU = {}

		System.out.println(this);   // OK: DU = {}
		System.out.println(V.this); // OK: DU = {}

		v = this;                   // OK: DU = {}
		v = V.this;                 // OK: DU = {}
		assert (this.x > 0);        // OK: DU = {}
		assert (this.y() > 0);      // OK: DU = {}
	}

	V() { // non-initial constructor. All, statements below are OK.
        this(10, 20);
		OK();                       
		this.OK();                  

		validate();                 
		this.validate();            
		V.this.validate();          

		System.out.println(this);   
		System.out.println(V.this); 

		V v = this;                 
		v = V.this;                 

		int l = this.ymx;           

		assert (this.x > 0);        
		assert (this.y() > 0);      
	}

	static void OK() {
	}

	int x() {
		return x;
	}

	int y() {
		return ymx + x;
	}

	void validate() {
		assert (x() > 0 && y() > 0);
	}

	public static void main(String... av) {
		V z = new V(1, 10);
		assert (z.x() == 1);
		assert (z.y() == 10);
	}
}
