import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Counters;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class invertDriver {
	public static enum COUNTERS {
		WORD_COUNT,
		MAPPER_COUNT
	}
	public static class SSIIMapper extends Mapper<Object, Text, Comparer, IntWritable>{
		private String fileName;
		
		@Override
		public void setup(Context context) { 
			fileName = ((FileSplit)context.getInputSplit()).getPath().getName();
			context.getCounter(COUNTERS.MAPPER_COUNT).increment(1);
		}
		
		@Override
		public void map(Object line_offset, Text line, Context outputContext) throws IOException, InterruptedException {
			StringTokenizer tokenizer = new StringTokenizer(line.toString().replaceAll("[^\\x00-\\x7F]", ""), " \t\r\n\r\f\",.:;?![]'*/-()&#");
			while(tokenizer.hasMoreTokens()) {
				String word = tokenizer.nextToken();
				outputContext.getCounter(COUNTERS.WORD_COUNT).increment(1);
				outputContext.write(new Comparer(word, fileName), new IntWritable(1));
			}
		}

	}
	
	public static class SSIIReducer extends Reducer<Comparer, IntWritable , Text, IntWritable>{
		@Override
		public void reduce(Comparer pair, Iterable<IntWritable> list, Context outputContext) throws IOException, InterruptedException {
			int count = 0;
			for(IntWritable l: list)
				count++;
			outputContext.write(new Text(pair.toString()), new IntWritable(count));
		}
	}
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "Inverted Index");
		
		job.setJarByClass(invertDriver.class);
		
		job.setPartitionerClass(KeyBasedPartition.class);
		
		job.setMapperClass(SSIIMapper.class);		
		job.setMapOutputKeyClass(Comparer.class);
		job.setMapOutputValueClass(IntWritable.class);
		
		job.setReducerClass(SSIIReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);
		
		Counters counters = job.getCounters();
		System.out.println(counters.findCounter(COUNTERS.WORD_COUNT).getDisplayName() + ": " + counters.findCounter(COUNTERS.WORD_COUNT).getValue());
		System.out.println(counters.findCounter(COUNTERS.MAPPER_COUNT).getDisplayName() + ": " + counters.findCounter(COUNTERS.MAPPER_COUNT).getValue());
		
		Path deletePath = new Path(args[1] + "/_SUCCESS");
		
		FileSystem gsfs = deletePath.getFileSystem(conf);
		
		gsfs.delete(deletePath, false);
		Path srcPath = new Path(args[1]);
		Path desPath = new Path("gs://dataproc-staging-us-east1-656399245777-lxfd90ny/invertedOutput.txt");
		boolean copySuccess = FileUtil.copyMerge(gsfs, srcPath, gsfs, desPath, false, conf, null);
		if(copySuccess)
			System.out.println("Files Merge Successful.");
		else
			System.out.println("Files Merge Failed.");
	}

}