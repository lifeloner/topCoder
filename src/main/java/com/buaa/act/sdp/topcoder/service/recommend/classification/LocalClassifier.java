package com.buaa.act.sdp.topcoder.service.recommend.classification;

import com.buaa.act.sdp.topcoder.util.Maths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by yang on 2017/3/15.
 */
@Component
public class LocalClassifier {

    private static final Logger logger = LoggerFactory.getLogger(LocalClassifier.class);

    @Autowired
    private TcBayes tcBayes;

    /**
     * 获取相似的任务
     *
     * @param features 特征向量
     * @param position 当前任务下标
     * @return
     */
    public List<Integer> getNeighbor(double[][] features, int position) {
        logger.info("get the similar tasks for a new task");
        List<Integer> neighborIndex = Maths.getSimilarityTasks(features, position);
        neighborIndex.add(position);
        return neighborIndex;
    }

    /**
     * 待推荐任务的开发者获胜概率
     *
     * @param features 特性
     * @param winners  所有的获胜者
     * @return
     */
    public Map<String, Double> getRecommendResult(double[][] features, List<String> winners, List<Integer> neighbors) {
        logger.info("recommend developers for a new task using local classifier");
        int k = neighbors.size();
        double[][] data = new double[k][features[0].length];
        List<String> winner = new ArrayList<>(k);
        Maths.copy(features, data, winners, winner, neighbors);
        Maths.normalization(data, 5);
        return tcBayes.getRecommendResult(data, k - 1, winner);
    }
}
