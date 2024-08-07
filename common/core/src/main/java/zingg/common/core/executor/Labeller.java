package zingg.common.core.executor;

import java.util.List;
import java.util.Scanner;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import zingg.common.client.*;
import zingg.common.client.cols.ZidAndFieldDefSelector;
import zingg.common.client.options.ZinggOptions;
import zingg.common.client.pipe.Pipe;
import zingg.common.client.util.ColName;

/**
 * The Labeller class is basically orchestrating the labeling phase of the data processing pipeline.
 * How?
 * By reading unmarked and marked records, processing them via CLI, updating records based on user input, and updating the data model.
 * @param <S>
 * @param <D>
 * @param <R>
 * @param <C>
 * @param <T>
 */
public abstract class Labeller<S,D,R,C,T> extends ZinggBase<S,D,R,C,T> {

	public static final Integer QUIT_LABELING = 9;
	public static final Integer INCREMENT = 1;
	private static final long serialVersionUID = 1L;
	protected static String name = "zingg.common.core.executor.Labeller";
	public static final Log LOG = LogFactory.getLog(Labeller.class);

	protected ITrainingDataModel<S, D, R, C> trainingDataModel;
	protected ILabelDataViewHelper<S, D, R, C> labelDataViewHelper;
	protected IClusterDataHandler<S, D, R, C> clusterDataHandler;
	protected RecordProcessingStrategy<S, D, R, C> recordProcessingStrategy;


	/**
	 * Constructor-based dependency injection for Labeller.
	 *
	 * Why?
	 * 1. Makes the dependencies explicit, improved code readability and maintainability.
	 * 2. Easier testing and mocking of dependencies.
	 * 3. Promotes immutability since dependencies are provided at the time of object creation and cannot be changed later.
	 *
	 * Note: No need for lazy initialization of dependencies now.
	 * Previously, the instances were created when first needed.
	 * With dependency injection, the instances are provided at the time of object creation,
	 * ensuring that all dependencies are available and initialized upfront.
	 */
	public Labeller(ITrainingDataModel<S, D, R, C> trainingDataModel,
					ILabelDataViewHelper<S, D, R, C> labelDataViewHelper,
					IClusterDataHandler<S, D, R, C> clusterDataHandler,
					RecordProcessingStrategy<S, D, R, C> recordProcessingStrategy) {
		setZinggOption(ZinggOptions.LABEL);
		this.trainingDataModel = trainingDataModel;
		this.labelDataViewHelper = labelDataViewHelper;
		this.clusterDataHandler = clusterDataHandler;
		this.recordProcessingStrategy = recordProcessingStrategy;
	}

	public Labeller(){
		setZinggOption(ZinggOptions.LABEL);
	}

	//Template Method Pattern in execute method.
	public void execute() throws ZinggClientException {
		try {
			LOG.info("Reading inputs for labelling phase ...");
			getTrainingDataModel().setMarkedRecordsStat(getMarkedRecords());
			ZFrame<D,R,C>  unmarkedRecords = getUnmarkedRecords();
			ZFrame<D,R,C>  updatedLabelledRecords = processRecordsCli(unmarkedRecords);
			getTrainingDataModel().writeLabelledOutput(updatedLabelledRecords,args);
			LOG.info("Finished labelling phase");
		} catch (Exception e) {
			e.printStackTrace();
			throw new ZinggClientException(e.getMessage());
		}
	}

	//Retrieves unmarked records and excludes already marked records.
	public ZFrame<D,R,C> getUnmarkedRecords() {
		ZFrame<D,R,C> unmarkedRecords = null;
		ZFrame<D,R,C> markedRecords = null;
		try {
			//getPipeUtil() -> returns an instance of a class that implements PipeUtilBase.
			//Retrieve the Pipe object for unmarked training data
			Pipe<D, R, C> unmarkedPipe = getPipeUtil().getTrainingDataUnmarkedPipe(args);

			//Read the unmarkedRecords data from the retrieved unmarkedPipe!
			unmarkedRecords = getPipeUtil().read(false, false, unmarkedPipe);
			try {
				//Retrieve the Pipe object for marked training data!
				Pipe<D, R, C> markedPipe = getPipeUtil().getTrainingDataMarkedPipe(args);

				//Read the markedRecords data from the retrieved unmarkedPipe!
				markedRecords = getPipeUtil().read(false, false, markedPipe);
			} catch (Exception e) {
				LOG.warn("No record has been marked yet");
			} catch (ZinggClientException zce) { //This is enough, we can remove the above catch block as it already extends Throwable.
				LOG.warn("No record has been marked yet");
			}
			if (markedRecords != null ) {
				/**
					Filter out the unmarked records from the marked records.
					Now, UnmarkedRecords will only include records that have not been marked,
					even if some records in the original unmarkedRecords set were already marked!
				*/
				unmarkedRecords = unmarkedRecords.join(markedRecords,ColName.CLUSTER_COLUMN, false,
						"left_anti");
				getTrainingDataModel().setMarkedRecordsStat(markedRecords);
			}
		}
		/**
		 * We can condense these catch blocks into a single catch block!
		 */
		catch (ZinggClientException e) {
			LOG.warn("No unmarked record for labelling");
			//Update ZinggClientException class
			//LOG.error() is more robust than just printing the stack trace.
			LOG.error("Exception occurred: ", e);
		}
		return unmarkedRecords;
	}

	//Print the statistics of marked records.
	void printStatistics() {
		getLabelDataViewHelper().printMarkedRecordsStat(
				getTrainingDataModel().getPositivePairsCount(),
				getTrainingDataModel().getNegativePairsCount(),
				getTrainingDataModel().getNotSurePairsCount(),
				getTrainingDataModel().getTotalCount()
		);
	}

	//Separated the concerns for processing records and displaying records.
	public ZFrame<D,R,C> processRecordsCli(ZFrame<D,R,C>  lines) throws ZinggClientException {
		LOG.info("Processing Records for CLI Labelling");
		if (lines != null && lines.count() > 0) {

			//display the current statistics of marked records.
			printStatistics();

			lines = lines.cache(); //Caches the lines to optimize performance for repeated access during the loop.

			ZidAndFieldDefSelector zidAndFieldDefSelector = new ZidAndFieldDefSelector(args.getFieldDefinition(), false, args.getShowConcise());

			// Why methods like getClusterIdsFrame and getClusterIds are part of ILabelDataViewHelper interface?
			// We can define a class ClusterDataHandler that implements methods like getClusterIdsFrame(), getClusterIds(), getCurrentPair(), getScore() and getPrediction().
			// Because it wasn't clear from the name ILabelDataViewHelper that it would contain methods like getClusterIdsFrame(), getClusterIds() like methods.
			ZFrame<D,R,C> clusterIdZFrame = getClusterDataHandler().getClusterIdsFrame(lines);
			List<R>  clusterIDs = getClusterDataHandler().getClusterIds(clusterIdZFrame);
			try {
				//Separate the concern, too big to be in a single method.
				//1st: Earlier I separated the method for processing records
				//2nd: But this can be made extensible by using a strategy pattern. Hence, I created a RecordProcessingStrategy interface.
				return recordProcessingStrategy.processRecords(lines, clusterIDs, clusterIdZFrame, zidAndFieldDefSelector);
			} catch (Exception e) {
				LOG.error("Labelling error has occurred: ", e);
				throw new ZinggClientException("An error has occurred while Labelling.", e);
			}
		} else {
			LOG.info("It seems there are no unmarked records at this moment. Please run findTrainingData job to build some pairs to be labelled and then run this labeler.");
			return null;
		}
	}

	protected int displayRecordsAndGetUserInput(ZFrame<D,R,C> records, String preMessage, String postMessage) {
		getLabelDataViewHelper().displayRecords(records, preMessage, postMessage);
        return readCliInput();
	}

	// This ensures scanner is closed after reading the input.
	int readCliInput() {
		try (Scanner sc = new Scanner(System.in)) {
			while (!sc.hasNext("[0129]")) {
				sc.next();
				System.out.println("Nope, please enter one of the allowed options!");
			}
			String word = sc.next();
			return Integer.parseInt(word);
		}
	}

	public void setTrainingDataModel(ITrainingDataModel<S, D, R, C> trainingDataModel) {
		this.trainingDataModel = trainingDataModel;
	}

	@Override
	public ITrainingDataModel<S, D, R, C> getTrainingDataModel() {
		return trainingDataModel;
	}

	@Override
	public ILabelDataViewHelper<S, D, R, C> getLabelDataViewHelper() {
		return labelDataViewHelper;
	}

	@Override
	public IClusterDataHandler<S, D, R, C> getClusterDataHandler() {
		return clusterDataHandler;
	}

	public void setLabelDataViewHelper(ILabelDataViewHelper<S, D, R, C> labelDataViewHelper) {
		this.labelDataViewHelper = labelDataViewHelper;
	}

}


