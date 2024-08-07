package zingg.common.core.executor;

import zingg.common.client.ZFrame;
import zingg.common.client.ZinggClientException;
import zingg.common.client.cols.ZidAndFieldDefSelector;

import java.util.List;

public interface RecordProcessingStrategy<S, D, R, C> {
    ZFrame<D, R, C> processRecords(ZFrame<D, R, C> lines, List<R> clusterIDs, ZFrame<D, R, C> clusterIdZFrame, ZidAndFieldDefSelector zidAndFieldDefSelector) throws ZinggClientException;
}