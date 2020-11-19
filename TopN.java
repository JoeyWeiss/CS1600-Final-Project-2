import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FileUtil;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class TopN {
	
	public static class nMapper extends Mapper<Object, Text, Text, IntWritable>{
		private SortedMap<Integer, List<String>> SortMap;
		private int x = 0;
		private int N;
		private String currentWord = null;
		private int currentCount = 0;
		
		@Override
		public void setup(Context context) {
			N = Integer.parseInt(context.getConfiguration().get("TopN"));
			SortMap = new TreeMap<Integer, List<String>>();
		}
		
		@Override
		public void map(Object lineOffset, Text line, Context outputContext) {
			String[] arr = line.toString().split("\t");
			if(arr.length != 3)
				return;
			arr[0] = arr[0].replaceAll("[^\\x00-\\x7F]", "");
			if(arr[0].equals("")) return;

			
			if(currentWord == null || !currentWord.equals(arr[0])) {
				if(currentWord != null) {
					List<String> innerList;
					if(SortMap.containsKey(currentCount)) 
						innerList = SortMap.get(currentCount);
					else 
						innerList = new ArrayList<String>(N);
					innerList.add(currentWord);
					SortMap.put(currentCount, innerList);
					
					x++;
					if(x > N) {
						Integer firstKey = SortMap.firstKey();
						innerList = SortMap.get(firstKey);
						innerList.remove(0);
						x--;
						if(innerList.size() == 0)
							SortMap.remove(firstKey);
					}
				}
				
				currentWord = arr[0];
				currentCount = Integer.parseInt(arr[2]);
			}
			else
				currentCount += Integer.parseInt(arr[2]);
		}
		
		@Override
		public void cleanup(Context outputContext) throws IOException, InterruptedException {
			List<String> innerList;
			if(SortMap.containsKey(currentCount))
				innerList = SortMap.get(currentCount);
			else {
				innerList = new ArrayList<String>(N);
				SortMap.put(currentCount, innerList);
			}
			innerList.add(currentWord);
			x++;
			if(x > N) {
				Integer firstKey = SortMap.firstKey();
				innerList = SortMap.get(firstKey);
				innerList.remove(0);
				x--;
				if(innerList.size() == 0)
					SortMap.remove(firstKey);
			}
			
			for (Map.Entry<Integer, List<String>> entry: SortMap.entrySet()) {
				for(String term: entry.getValue()) {
					outputContext.write(new Text(term), new IntWritable(entry.getKey()));
				}
			}
		}
	}
	
	public static class nReducer extends Reducer<Text, IntWritable, Text, IntWritable>{
		private SortedMap<Integer, List<String>> SortMap;
		private int x = 0;
		private int N;
		
		@Override
		public void setup(Context context) {
			N = Integer.parseInt(context.getConfiguration().get("TopN"));
			SortMap = new TreeMap<Integer, List<String>>();
		}
		
		@Override
		public void reduce(Text term, Iterable<IntWritable> list, Context outputContext) {
			int freqs = 0;
			for(IntWritable freq: list)
				freqs += freq.get();
			
			List<String> innerList;
			if(!SortMap.containsKey(freqs)) {
				innerList = new ArrayList<String>(N);
				SortMap.put(freqs, innerList);
			}
			else
				innerList = SortMap.get(freqs);
			innerList.add(term.toString());
			x++;
			if(x > N) {
				Integer firstKey = SortMap.firstKey();
				innerList = SortMap.get(firstKey);
				innerList.remove(0);
				x--;
				if(innerList.size() == 0)
					SortMap.remove(firstKey);
			}
		}
		
		@Override
		public void cleanup(Context outputContext) throws IOException, InterruptedException {
			for(Map.Entry<Integer, List<String>> entry: SortMap.entrySet()) {
				for(String term: entry.getValue())
					outputContext.write(new Text(term), new IntWritable(entry.getKey()));
			}
		}
	}

	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException {
		Configuration conf = new Configuration();

		conf.set("TopN", args[2]);
		System.out.println("N: " + args[2]);
		
		Job job = Job.getInstance(conf, "Top N");
		
		job.setJarByClass(TopN.class);
		
		job.setNumReduceTasks(1);
		
		job.setMapperClass(nMapper.class);
		job.setReducerClass(nReducer.class);
		
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		job.waitForCompletion(true);
		
		Path deletePath = new Path(args[1] + "/_SUCCESS");
		Path srcPath = new Path(args[1]);
		Path desPath = new Path("gs://dataproc-staging-us-east1-656399245777-lxfd90ny/TopN.txt");
		
		FileSystem gsfs = deletePath.getFileSystem(conf);
		
		gsfs.delete(deletePath, false);
		
		boolean copySuccess = FileUtil.copyMerge(gsfs, srcPath, gsfs, desPath, false, conf, null);
		if(copySuccess)
			System.out.println("Files Merge Successful.");
		else
			System.out.println("Files Merge Failed.");
		
		
	}

}