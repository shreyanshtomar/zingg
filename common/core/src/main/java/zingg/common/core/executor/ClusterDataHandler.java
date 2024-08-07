package zingg.common.core.executor;

import zingg.common.client.ClientOptions;
import zingg.common.client.IClusterDataHandler;
import zingg.common.client.ZFrame;
import zingg.common.client.ZinggClientException;
import zingg.common.client.util.ColName;
import zingg.common.core.context.Context;

import java.util.List;

public class ClusterDataHandler<S, D, R, C, T> extends ZinggBase<S, D, R, C, T> implements IClusterDataHandler<S, D, R, C> {

    @Override
    public void execute() throws ZinggClientException {
        throw new UnsupportedOperationException();
    }

    public ClusterDataHandler(Context<S,D,R,C,T> context, ClientOptions clientOptions) {
        setContext(context);
        setClientOptions(clientOptions);
        setName(this.getClass().getName());
    }

    public ZFrame<D, R, C> getClusterIdsFrame(ZFrame<D, R, C> lines) {
        return lines.select(ColName.CLUSTER_COLUMN).distinct();
    }

    public List<R> getClusterIds(ZFrame<D, R, C> lines) {
        return lines.collectAsList();
    }

    public ZFrame<D, R, C> getCurrentPair(ZFrame<D, R, C> lines, int index, List<R> clusterIds, ZFrame<D, R, C> clusterLines) {
        return lines.filter(lines.equalTo(ColName.CLUSTER_COLUMN,
                clusterLines.getAsString(clusterIds.get(index), ColName.CLUSTER_COLUMN))).cache();
    }

    public double getScore(ZFrame<D, R, C> currentPair) {
        return currentPair.getAsDouble(currentPair.head(), ColName.SCORE_COL);
    }

    public double getPrediction(ZFrame<D, R, C> currentPair) {
        return currentPair.getAsDouble(currentPair.head(), ColName.PREDICTION_COL);
    }
}