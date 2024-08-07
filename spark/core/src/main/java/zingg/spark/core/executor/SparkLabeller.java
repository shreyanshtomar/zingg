package zingg.spark.core.executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.spark.sql.Column;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataType;
import zingg.common.client.ClientOptions;
import zingg.common.client.IArguments;
import zingg.common.client.ZinggClientException;
import zingg.common.client.options.ZinggOptions;
import zingg.common.core.executor.*;
import zingg.spark.core.context.ZinggSparkContext;


/**
 * Spark specific implementation of Labeller
 *
 *
 */

public class SparkLabeller extends Labeller<SparkSession, Dataset<Row>, Row, Column, DataType> {

	private static final long serialVersionUID = 1L;
	public static String name = "zingg.spark.core.executor.SparkLabeller";
	public static final Log LOG = LogFactory.getLog(SparkLabeller.class);

	public SparkLabeller() {
		this(new ZinggSparkContext());
	}

	public SparkLabeller(ZinggSparkContext sparkContext) {
		super(
				new TrainingDataModel<>(sparkContext, new ClientOptions()),
				new LabelDataViewHelper<>(sparkContext, new ClientOptions()),
				new ClusterDataHandler<>(sparkContext, new ClientOptions()),
				// new DefaultRecordProcessingStrategy<>(this)
				// the "this" reference cannot be used until after the superclass constructor has completed.
				null // Placeholder for the strategy
		);
		setZinggOption(ZinggOptions.LABEL);
		setContext(sparkContext);
		// Initialize the strategy after the superclass constructor has been called
		setRecordProcessingStrategy(new DefaultRecordProcessingStrategy<>(this));
	}

	@Override
	public void init(IArguments args, SparkSession s) throws ZinggClientException {
		super.init(args, s);
		getContext().init(s);
	}

	// We can set different strategies for processing records which makes our code more flexible to process records!
	private void setRecordProcessingStrategy(RecordProcessingStrategy<SparkSession, Dataset<Row>, Row, Column> strategy) {
		this.recordProcessingStrategy = strategy;
	}
}