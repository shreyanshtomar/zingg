package zingg.common.client;

import java.util.List;

public interface ILabelDataViewHelper<S, D, R, C> {

	String formatLabelingProgress(int index, int totalPairs);

	String formatPredictionMessage(double prediction, double score);

	void displayRecords(ZFrame<D, R, C> records, String preMessage, String postMessage);

	void printMarkedRecordsStat(long positivePairsCount, long negativePairsCount, long notSurePairsCount,
			long totalCount);

}