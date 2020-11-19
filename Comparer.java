import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public class Comparer implements Writable, WritableComparable<Comparer>{
	private String writter;
	private String Word;
	
	public Comparer() {}
	public Comparer(String Word, String writter) {
		this.Word = Word;
		this.writter = writter;
	}
	
	public String getTerm() {return Word;}
	
	public String getDocName() {return writter;}

	@Override
	public void readFields(DataInput in) throws IOException {
		Word = in.readUTF();
		writter = in.readUTF();
	}
	
	@Override
	public int compareTo(Comparer pair) {
		int termCompare = this.Word.compareTo(pair.Word);
		if(termCompare == 0) {
			int sameWritterTest = this.writter.compareTo(pair.writter);
			return sameWritterTest;
		}
		else {
			return termCompare;
		}
	}
	
	@Override
	public void write(DataOutput out) throws IOException {
		out.writeUTF(Word);;
		out.writeUTF(writter);
	}
	
	@Override
	public String toString() {
		return Word + "\t" + writter;
	}
}
