package zingg.common.client;

import java.util.List;

public interface IClusterDataHandler<S, D, R, C> {
    ZFrame<D, R, C> getClusterIdsFrame(ZFrame<D, R, C> lines);

    List<R> getClusterIds(ZFrame<D, R, C> lines);

//	List<C> getDisplayColumns(ZFrame<D, R, C> lines, IArguments args);

    ZFrame<D, R, C> getCurrentPair(ZFrame<D, R, C> lines, int index, List<R> clusterIds, ZFrame<D, R, C> clusterLines);

    double getScore(ZFrame<D, R, C> currentPair);

    double getPrediction(ZFrame<D, R, C> currentPair);
}
