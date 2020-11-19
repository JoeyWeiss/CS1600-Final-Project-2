import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.io.IntWritable; 

public class KeyBasedPartition extends Partitioner<Comparer, IntWritable>{
	@Override
	public int getPartition(Comparer newKey, IntWritable value, int parts) {
		return Math.abs(newKey.getTerm().hashCode() % parts);
	}}
