/*
 * Mavuno: A Hadoop-Based Text Mining Toolkit
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package edu.isi.mavuno.app.distsim;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import edu.isi.mavuno.extract.CombineSplits;
import edu.isi.mavuno.extract.Extract;
import edu.isi.mavuno.extract.ExtractGlobalStats;
import edu.isi.mavuno.extract.Split;
import edu.isi.mavuno.util.MavunoUtils;

/**
 * @author metzler
 *
 */
public class PatternToContext extends Configured implements Tool {
	private static final Logger sLogger = Logger.getLogger(PatternToContext.class);

	public PatternToContext(Configuration conf) {
		super(conf);
	}

	/* (non-Javadoc)
	 * @see org.apache.hadoop.util.Tool#run(java.lang.String[])
	 */
	@Override
	public int run(String[] args) throws ClassNotFoundException, InterruptedException, IOException {
		MavunoUtils.readParameters(args, "Mavuno.PatternToContext", getConf());
		return run();
	}
	
	public int run() throws ClassNotFoundException, InterruptedException, IOException {
		Configuration conf = getConf();

		String patternPath = MavunoUtils.getRequiredParam("Mavuno.PatternToContext.PatternPath", conf);
		String corpusPath = MavunoUtils.getRequiredParam("Mavuno.PatternToContext.CorpusPath", conf);
		String corpusClass = MavunoUtils.getRequiredParam("Mavuno.PatternToContext.CorpusClass", conf);
		String extractorClass = MavunoUtils.getRequiredParam("Mavuno.PatternToContext.ExtractorClass", conf);
		String extractorArgs = MavunoUtils.getRequiredParam("Mavuno.PatternToContext.ExtractorArgs", conf);
		String minMatches = MavunoUtils.getRequiredParam("Mavuno.PatternToContext.MinMatches", conf);
		boolean harvestGlobalStats = Boolean.parseBoolean(MavunoUtils.getRequiredParam("Mavuno.PatternToContext.GlobalStats", conf));
		String outputPath = MavunoUtils.getRequiredParam("Mavuno.PatternToContext.OutputPath", conf);

		MavunoUtils.createDirectory(conf, outputPath);

		sLogger.info("Tool name: PatternToContext");
		sLogger.info(" - Pattern path: " + patternPath);
		sLogger.info(" - Corpus path: " + corpusPath);
		sLogger.info(" - Corpus class: " + corpusClass);
		sLogger.info(" - Extractor class: " + extractorClass);
		sLogger.info(" - Extractor args: " + extractorArgs);
		sLogger.info(" - Min matches: " + minMatches);
		sLogger.info(" - Harvest global stats: " + harvestGlobalStats);
		sLogger.info(" - Output path: " + outputPath);

		// set total terms path
		conf.set("Mavuno.TotalTermsPath", outputPath + "/totalTerms");

		// split patterns into manageable chunks
		conf.set("Mavuno.Split.InputPath", patternPath);
		conf.set("Mavuno.Split.OutputPath", outputPath + "/patterns-split");
		conf.set("Mavuno.Split.SplitKey", "pattern");
		new Split(conf).run();

		// get pattern splits
		FileStatus [] files = MavunoUtils.getDirectoryListing(conf, outputPath + "/patterns-split");
		int split = 0;
		for(FileStatus file : files) {
			if(!file.getPath().getName().endsWith(".examples")) {
				continue;
			}

			// extract contexts
			conf.set("Mavuno.Extract.InputPath", file.getPath().toString());
			conf.set("Mavuno.Extract.CorpusPath", corpusPath);
			conf.set("Mavuno.Extract.CorpusClass", corpusClass);
			conf.set("Mavuno.Extract.ExtractorClass", extractorClass);
			conf.set("Mavuno.Extract.ExtractorArgs", extractorArgs);
			conf.set("Mavuno.Extract.ExtractorTarget", "context");
			conf.set("Mavuno.Extract.MinMatches", minMatches);
			conf.set("Mavuno.Extract.OutputPath", outputPath + "/patterns-split/contexts/" + split);
			new Extract(conf).run();

			// increment split
			split++;
		}

		// extract global context statistics if necessary
		if(harvestGlobalStats) {
			conf.set("Mavuno.ExtractGlobalStats.InputPath", outputPath + "/patterns-split/contexts/");
			conf.set("Mavuno.ExtractGlobalStats.CorpusPath", corpusPath);
			conf.set("Mavuno.ExtractGlobalStats.CorpusClass", corpusClass);
			conf.set("Mavuno.ExtractGlobalStats.ExtractorClass", extractorClass);
			conf.set("Mavuno.ExtractGlobalStats.ExtractorArgs", extractorArgs);
			conf.set("Mavuno.ExtractGlobalStats.ExtractorTarget", "context");
			conf.set("Mavuno.ExtractGlobalStats.OutputPath", outputPath + "/patterns-split/context-stats/");
			new ExtractGlobalStats(conf).run();
		}

		// combine pattern splits
		conf.set("Mavuno.CombineSplits.ExamplesPath", outputPath + "/patterns-split/contexts");
		conf.set("Mavuno.CombineSplits.ExampleStatsPath", outputPath + "/patterns-split/context-stats");
		conf.set("Mavuno.CombineSplits.SplitKey", "pattern");
		conf.setInt("Mavuno.CombineSplits.TotalSplits", split);
		conf.set("Mavuno.CombineSplits.OutputPath", outputPath + "/context-stats");
		new CombineSplits(conf).run();

		// delete pattern splits
		MavunoUtils.removeDirectory(conf, outputPath + "/patterns-split");

		return 0;
	}

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		int res = ToolRunner.run(new PatternToContext(conf), args);
		System.exit(res);
	}

}
