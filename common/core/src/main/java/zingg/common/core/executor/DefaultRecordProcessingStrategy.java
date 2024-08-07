package zingg.common.core.executor;

import zingg.common.client.ZFrame;
import zingg.common.client.ZinggClientException;
import zingg.common.client.cols.ZidAndFieldDefSelector;

import java.util.List;

public class DefaultRecordProcessingStrategy<S, D, R, C> implements RecordProcessingStrategy<S, D, R, C> {
    private final Labeller<S, D, R, C, ?> labeller;

    public DefaultRecordProcessingStrategy(Labeller<S, D, R, C, ?> labeller) {
        this.labeller = labeller;
    }

    @Override
    public ZFrame<D, R, C> processRecords(ZFrame<D, R, C> lines, List<R> clusterIDs, ZFrame<D, R, C> clusterIdZFrame, ZidAndFieldDefSelector zidAndFieldDefSelector) throws ZinggClientException {
        double score;
        double prediction;
        ZFrame<D, R, C> updatedRecords = null;
        int selectedOption = -1;
        String msg1, msg2;
        int totalPairs = clusterIDs.size();

        for (int index = 0; index < totalPairs; index++) {
            ZFrame<D, R, C> currentPair = labeller.getClusterDataHandler().getCurrentPair(lines, index, clusterIDs, clusterIdZFrame);

            score = labeller.getClusterDataHandler().getScore(currentPair);
            prediction = labeller.getClusterDataHandler().getPrediction(currentPair);

            msg1 = labeller.getLabelDataViewHelper().formatLabelingProgress(index, totalPairs);
            msg2 = labeller.getLabelDataViewHelper().formatPredictionMessage(prediction, score);

            selectedOption = labeller.displayRecordsAndGetUserInput(currentPair.select(zidAndFieldDefSelector.getCols()), msg1, msg2);
            labeller.getTrainingDataModel().updateLabellerStat(selectedOption, Labeller.INCREMENT);

            labeller.printStatistics();

            if (selectedOption == Labeller.QUIT_LABELING) {
                Labeller.LOG.info("User has quit in the middle. Updating the records.");
                break;
            }
            updatedRecords = labeller.getTrainingDataModel().updateRecords(selectedOption, currentPair, updatedRecords);
        }
        Labeller.LOG.warn("Processing finished.");
        return updatedRecords;
    }
}