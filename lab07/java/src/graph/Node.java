/**
 * Proiectarea Algoritmilor, 2016
 * Lab 7: Aplicatii DFS
 *
 * @author 	Radu Iacob
 * @email	radu.iacob23@gmail.com
 * @author adinu
 * @email  mandrei.dinu@gmail.com
 */

package graph;

public class Node {

	int id;
	
	public Node(int id) {
		this.id = id;
		reset();
	}
	
	public Node(int id, int value) {
	    this.id = id;
	    this.value = value;
	    reset();
	}
	
    /**
     * Reprezinta cate noduri au fost vizitate inainte de nodul curent. </br>
     */
    int index;

    /**
     * Valoarea default pentru indexul unui nod nevizitat. </br>
     */
    public final static int UNSET = -1;
	
	/**
	 * Cel mai mic index al unui nod accesibil din nodul curent. </br>
	 */
	public int lowLink;

	/**
     * (Optional) Descrie daca nodul curent se afla sau nu pe stiva,
     * pentru a verifica acest aspect eficient in timpul algoritmului
     * lui Tarjan.
     */
	public boolean inStack;
		
	/**
	 * (Bonus) Valoarea unui nod.
	 */
	int value;
	
	public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getLowLink() {
        return lowLink;
    }

    public void setLowLink(int lowLink) {
        this.lowLink = lowLink;
    }

    public boolean isInStack() {
        return inStack;
    }

    public void setInStack(boolean inStack) {
        this.inStack = inStack;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    /**
	 * @return 'true' daca nodul a fost vizitat, altfel intoarce false
	 */
	public boolean wasVisited() {
		return index != UNSET;
	}

	/**
	 * Reseteaza valoarea atributelor index si lowLink
	 */
	public void reset() {
	    index = lowLink = UNSET;
		inStack = false;
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {	    
	    return "( " + Integer.toString(id) + " )";	    
	}

}
