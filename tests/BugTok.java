/**
 * BugTok.
 *
 * @author Yannick Jestin <mailto:jestin@cena.fr>
 *
 * (c) CENA
 *
 * 1.2.3:
 *   - replace Vector.add by Vector.addElement() to maintain jdk1.1
 *   compatibility
 */
import java.util.Vector ;
class BugTok {

	public static String[] decoupe(String s,String sep) {
		int index=0, last=0, length=s.length();
		Vector<String> v = new Vector<String>();
		if (length!=0) while (true) {
		  index=s.indexOf(sep,last);
		  if (index==-1) {
		    v.addElement(s.substring(last,length));
		    break;
		  } else if (index<s.length()) {
		    v.addElement(s.substring(last,index));
		    last=index+1;
		  } else {
		    break;
		  }
		}
		String[] tab = new String[v.size()];
		v.copyInto(tab);
		return tab;
	}

	public static void doprint(String[] tab) {
	  System.out.println("------------ "+tab.length+" elements --------------");
	  for (int i=0; i<tab.length;i++) {
	    System.out.println("'"+tab[i]+"'");
	  }
	  System.out.println("------------------------------------------------------");
	}

	public static void main(String[] arg) {
	  doprint(decoupe("ils ont  change ma chanson"," ")) ;
	  doprint(decoupe(" ils ont  change ma chanson"," ")) ;
	  doprint(decoupe("\u0003ils\u0003ont\u0003\u0003change ma chanson","\u0003")) ;
	  doprint(decoupe(""," ")) ;
	}
}
